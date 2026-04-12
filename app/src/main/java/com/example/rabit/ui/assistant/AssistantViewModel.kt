package com.example.rabit.ui.assistant

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.domain.model.gemini.GeminiRequest
import com.example.rabit.domain.model.gemini.GeminiResponse
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.data.gemini.LocalLlmManager
import com.example.rabit.data.gemini.ModelInfo
import com.example.rabit.data.gemini.ModelLoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlinx.serialization.Serializable
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.app.DownloadManager
import com.example.rabit.data.voice.VoiceAssistantManager
import com.example.rabit.data.voice.VoiceState
import com.example.rabit.data.repository.AssistantHistoryPersistence
import com.example.rabit.data.repository.ChatSession

@Serializable
data class AttachedFile(val name: String, val content: String)

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val attachedImageUris: List<String> = emptyList(),   // URI strings of attached images
    val attachedFiles: List<String> = emptyList()       // Names of attached files
)

sealed class AssistantUiState {
    object Idle : AssistantUiState()
    object Loading : AssistantUiState()
    data class Success(val response: GeminiResponse) : AssistantUiState()
    data class Error(val message: String) : AssistantUiState()
}

class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val geminiRepo = GeminiRepositoryImpl()
    private val localLlmManager = LocalLlmManager(application)
    private val prefs = application.getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val uiState: StateFlow<AssistantUiState> = _uiState

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input

    private val _autoPushEnabled = MutableStateFlow(false)
    val autoPushEnabled = _autoPushEnabled.asStateFlow()

    private val _systemInstruction = MutableStateFlow(prefs.getString("system_instruction", "") ?: "")
    val systemInstruction: StateFlow<String> = _systemInstruction

    private val _temperature = MutableStateFlow(prefs.getFloat("llm_temperature", 0.7f))
    val temperature = _temperature.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isModelDownloaded = MutableStateFlow(localLlmManager.isModelDownloaded())
    val isModelDownloaded = _isModelDownloaded.asStateFlow()

    private val _downloadProgress = localLlmManager.downloadProgress
    val downloadProgress = _downloadProgress

    private val _loadState = localLlmManager.loadState
    val loadState = _loadState

    // ── Model Management ──
    val availableModels: List<ModelInfo> get() = localLlmManager.availableModels
    
    private val _downloadedModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val downloadedModels = _downloadedModels.asStateFlow()

    private val _activeOfflineModel = MutableStateFlow<ModelInfo?>(null)
    val activeOfflineModel = _activeOfflineModel.asStateFlow()

    private val _modelStorageUsageMb = MutableStateFlow(0L)
    val modelStorageUsageMb = _modelStorageUsageMb.asStateFlow()

    // ── Chat Sessions ──
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions = _chatSessions.asStateFlow()

    private val _showCommands = MutableStateFlow(false)
    val showCommands = _showCommands.asStateFlow()

    private val _attachedImages = MutableStateFlow<List<Pair<android.net.Uri, String>>>(emptyList()) // <uri, base64>
    val attachedImages = _attachedImages.asStateFlow()

    private val _attachedFiles = MutableStateFlow<List<AttachedFile>>(emptyList())
    val attachedFiles = _attachedFiles.asStateFlow()
    
    private val _selectedModelName = MutableStateFlow(prefs.getString("selected_model", "Gemini Pro") ?: "Gemini Pro")
    val selectedModelName: StateFlow<String> = _selectedModelName.asStateFlow()

    val modelLoadState = localLlmManager.loadState
    val modelCopyProgress = localLlmManager.downloadProgress
    val modelLastError = localLlmManager.lastError

    private val voiceAssistantManager = VoiceAssistantManager(application)
    val voiceState = voiceAssistantManager.state
    val voiceResult = voiceAssistantManager.result

    private val hidDeviceManager = com.example.rabit.data.bluetooth.HidDeviceManager.getInstance(application)
    val deviceConnectionState = hidDeviceManager.connectionState

    var lastResponse: com.example.rabit.domain.model.gemini.GeminiResponse? = null

    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
                    override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
                    @Deprecated("Deprecated in Java", ReplaceWith("Unit"))
                    override fun onError(utteranceId: String?) { _isSpeaking.value = false }
                })
            }
        }

        viewModelScope.launch {
            // Migrate legacy history if exists
            AssistantHistoryPersistence.migrateLegacyIfNeeded(application)
            
            // Load sessions
            val sessions = AssistantHistoryPersistence.listSessions(application)
            _chatSessions.value = sessions
            
            if (sessions.isNotEmpty()) {
                // Load the most recent session
                val latest = sessions.first()
                _currentSessionId.value = latest.id
                val loaded = AssistantHistoryPersistence.loadSession(application, latest.id)
                if (loaded.isNotEmpty()) {
                    _messages.value = loaded
                }
            } else {
                // Create a new session
                startNewSession()
            }
            
            // Refresh model info
            refreshModelInfo()
        }

        // Observe voice results
        viewModelScope.launch {
            voiceAssistantManager.result.collect { result ->
                if (result.isNotBlank() && voiceAssistantManager.state.value == VoiceState.SUCCESS) {
                    onInputChanged(result)
                    // If "Auto-Submit" is desired in the future, we could trigger sendPrompt here
                }
            }
        }
    }

    private fun refreshModelInfo() {
        _downloadedModels.value = localLlmManager.getDownloadedModels()
        _modelStorageUsageMb.value = localLlmManager.getModelStorageUsageMb()
        _isModelDownloaded.value = localLlmManager.isModelDownloaded()
        
        // Restore active model from prefs
        val activeModelId = prefs.getString("active_offline_model_id", null)
        _activeOfflineModel.value = localLlmManager.availableModels.find { it.id == activeModelId }
    }

    private fun saveCurrentSession() {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            val messages = _messages.value
            AssistantHistoryPersistence.saveSession(getApplication(), sessionId, messages)
            
            // Update session metadata
            val title = AssistantHistoryPersistence.generateTitle(messages)
            val session = ChatSession(
                id = sessionId,
                title = title,
                lastModified = System.currentTimeMillis(),
                messageCount = messages.size
            )
            AssistantHistoryPersistence.updateSessionInIndex(getApplication(), session)
            _chatSessions.value = AssistantHistoryPersistence.listSessions(getApplication())
        }
    }

    private fun saveHistory() {
        saveCurrentSession()
    }

    fun onInputChanged(text: String) { 
        _input.value = text 
        _showCommands.value = text.startsWith("/") && text.length == 1
    }

    fun downloadModel() {
        val model = localLlmManager.availableModels.first() // Default CPU model
        downloadSpecificModel(model)
    }

    fun downloadSpecificModel(model: ModelInfo) {
        viewModelScope.launch {
            val downloadId = localLlmManager.downloadModel(model)
            if (downloadId != null) {
                // Background polling for progress
                launch {
                    val downloadManager = getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    var downloading = true
                    while (downloading) {
                        localLlmManager.updateDownloadProgress(downloadId)
                        val state = localLlmManager.loadState.value
                        if (state != ModelLoadState.DOWNLOADING) {
                            downloading = false
                            if (state == ModelLoadState.COPYING) {
                                // Successfully downloaded to external, now "COPYING" (simulated)
                                refreshModelInfo()
                                // In a more advanced version, we'd move External -> Internal here
                            }
                        }
                        kotlinx.coroutines.delay(1000)
                    }
                }
            } else {
                // If main URL failed immediately, try fallback if available
                model.fallbackUrl?.let { fallback ->
                    val fallbackModel = model.copy(url = fallback)
                    downloadSpecificModel(fallbackModel)
                }
            }
        }
    }

    fun cancelModelDownload() {
        localLlmManager.cancelDownload()
    }

    fun deleteOfflineModel(model: ModelInfo) {
        localLlmManager.deleteModel(model)
        refreshModelInfo()
    }

    fun selectOfflineModel(model: ModelInfo) {
        _activeOfflineModel.value = model
        prefs.edit().putString("active_offline_model_id", model.id).apply()
        val path = localLlmManager.getDownloadedModelPath(model)
        if (path != null) {
            prefs.edit().putString("gguf_path", path).apply()
        }
    }

    fun onSystemInstructionChanged(text: String) {
        _systemInstruction.value = text
        prefs.edit().putString("system_instruction", text).apply()
    }

    fun setAutoPushEnabled(enabled: Boolean) { _autoPushEnabled.value = enabled }

    fun updateTemperature(temp: Float) {
        _temperature.value = temp
        prefs.edit().putFloat("llm_temperature", temp).apply()
    }

    fun clearModelError() {
        localLlmManager.clearError()
    }

    fun attachImages(images: List<Pair<android.net.Uri, String>>) {
        _attachedImages.value = _attachedImages.value + images
    }

    fun removeImage(uri: android.net.Uri) {
        _attachedImages.value = _attachedImages.value.filter { it.first != uri }
    }

    fun clearMessages() {
        _messages.value = emptyList()
        saveHistory()
    }

    // ── Session Management ──

    fun startNewSession() {
        val newSession = AssistantHistoryPersistence.createNewSession()
        _currentSessionId.value = newSession.id
        _messages.value = emptyList()
        viewModelScope.launch {
            AssistantHistoryPersistence.updateSessionInIndex(getApplication(), newSession)
            _chatSessions.value = AssistantHistoryPersistence.listSessions(getApplication())
        }
    }

    fun loadChatSession(sessionId: String) {
        // Save current session first
        saveCurrentSession()
        
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            val messages = AssistantHistoryPersistence.loadSession(getApplication(), sessionId)
            _messages.value = messages
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            AssistantHistoryPersistence.deleteSession(getApplication(), sessionId)
            val updatedSessions = AssistantHistoryPersistence.listSessions(getApplication())
            _chatSessions.value = updatedSessions
            
            // If we deleted the current session, or if NO sessions remain, reset state
            if (_currentSessionId.value == sessionId || updatedSessions.isEmpty()) {
                if (updatedSessions.isNotEmpty()) {
                    loadChatSession(updatedSessions.first().id)
                } else {
                    // Total reset
                    _currentSessionId.value = null
                    _messages.value = emptyList()
                    startNewSession()
                }
            }
        }
    }

    fun updateSelectedModel(model: String) {
        _selectedModelName.value = model
    }

    fun attachFile(name: String, content: String) {
        _attachedFiles.value = _attachedFiles.value + AttachedFile(name, content)
    }

    fun removeFile(name: String) {
        _attachedFiles.value = _attachedFiles.value.filter { it.name != name }
    }

    fun clearAttachment() {
        _attachedImages.value = emptyList()
        _attachedFiles.value = emptyList()
    }

    fun sendPrompt(apiKey: String, temperature: Float, maxTokens: Int, model: String) {
        val prompt = _input.value.trim()
        if (prompt.isBlank()) return

        val isOffline = prefs.getBoolean("is_offline_mode", false)
        val ggufPath = prefs.getString("gguf_path", null)
        val systemPrompt = _systemInstruction.value.ifBlank { null }
        
        var effectiveSystemPrompt = systemPrompt
        if (prompt.contains("output ONLY the raw code", ignoreCase = true)) {
            effectiveSystemPrompt = "You are an expert coding test solver. You will receive an image of a coding problem (e.g., LeetCode, HackerRank). Write the complete, working solution. CRITICAL: You MUST output ONLY the raw code. Do NOT use markdown code blocks (like ```python). Do NOT add ANY explanations, comments, or pleasantries. The output must be pure code ready to be typed via an automated keyboard."
            setAutoPushEnabled(true)
        }

        val imageDataList = _attachedImages.value
        val files = _attachedFiles.value
        
        val filesString = if (files.isNotEmpty()) {
            "\n\n[Attached files content:]\n" + files.joinToString("\n\n") { "--- ${it.name} ---\n${it.content}" }
        } else ""
        
        val actualPrompt = if (filesString.isNotEmpty()) "$prompt$filesString" else prompt

        val userMsg = ChatMessage(
            content = prompt,
            isUser = true,
            attachedImageUris = imageDataList.map { it.first.toString() },
            attachedFiles = files.map { it.name }
        )
        clearAttachment()
        val loadingMsg = ChatMessage(content = "", isUser = false, isLoading = true)
        _messages.value = _messages.value + userMsg + loadingMsg
        _input.value = ""
        _uiState.value = AssistantUiState.Loading

        viewModelScope.launch {
            try {
                val resp = if (isOffline) {
                    if (ggufPath == null) {
                        GeminiResponse(
                            text = "",
                            error = com.example.rabit.domain.model.gemini.GeminiError(
                                -1,
                                "No model file selected. Go to Settings > AI Configuration."
                            )
                        )
                    } else {
                        val initialized = localLlmManager.initialize(ggufPath)
                        if (initialized) {
                            val promptWithSystem = if (effectiveSystemPrompt != null) {
                                "Instruction: $effectiveSystemPrompt\n\nUser: $actualPrompt\n\nAssistant:"
                            } else actualPrompt
                            
                            GeminiResponse(
                                text = localLlmManager.generateResponse(promptWithSystem),
                                promptText = prompt,
                                attachedImageUris = imageDataList.map { it.first }
                            )
                        } else {
                            GeminiResponse(
                                text = "",
                                error = com.example.rabit.domain.model.gemini.GeminiError(
                                    -2,
                                    localLlmManager.getLastError() ?: "Failed to initialize Local LLM."
                                )
                            )
                        }
                    }
                } else {
                    val req = GeminiRequest(
                        prompt = actualPrompt,
                        model = model,
                        temperature = temperature,
                        maxTokens = maxTokens,
                        systemPrompt = effectiveSystemPrompt,
                        imageBase64List = imageDataList.map { it.second },
                        imageMimeType = "image/jpeg"
                    )
                    geminiRepo.sendPrompt(req, apiKey).copy(
                        promptText = prompt,
                        attachedImageUris = imageDataList.map { it.first }
                    )
                }

                val current = _messages.value.dropLast(1)
                if (resp.error != null) {
                    _messages.value = current + ChatMessage(
                        content = "Error: ${resp.error.message}",
                        isUser = false
                    )
                    _uiState.value = AssistantUiState.Error(resp.error.message)
                } else {
                    _messages.value = current + ChatMessage(content = resp.text, isUser = false)
                    _uiState.value = AssistantUiState.Success(resp)
                    lastResponse = resp
                }
                saveHistory()
            } catch (e: Exception) {
                val current = _messages.value.dropLast(1)
                _messages.value = current + ChatMessage(
                    content = "Error: ${e.message ?: "Unknown error"}",
                    isUser = false
                )
                _uiState.value = AssistantUiState.Error(e.message ?: "Unknown error")
                saveHistory()
            }
        }
    }

    fun clearInput() { _input.value = "" }
    fun clearResponse() { _uiState.value = AssistantUiState.Idle }
    fun clearConversation() {
        _messages.value = emptyList()
        _uiState.value = AssistantUiState.Idle
        startNewSession()
    }

    fun speakText(text: String) {
        if (_isSpeaking.value) {
            tts?.stop()
            _isSpeaking.value = false
            return
        }
        // Minimal cleanup for speaking nicely: remove markdown tokens
        val cleanText = text.replace(Regex("```[\\s\\S]*?```"), "Code Block.")
                            .replace(Regex("[*#`]"), "")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun startVoiceRecognition() {
        voiceAssistantManager.startListening()
    }

    fun stopVoiceRecognition() {
        voiceAssistantManager.stopListening()
    }

    fun resetVoiceState() {
        voiceAssistantManager.reset()
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        localLlmManager.close()
        super.onCleared()
    }
}

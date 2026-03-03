package com.example.rabit.ui.assistant

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.domain.model.gemini.GeminiRequest
import com.example.rabit.domain.model.gemini.GeminiResponse
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.data.gemini.LocalLlmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    var lastResponse: GeminiResponse? = null

    fun onInputChanged(text: String) {
        _input.value = text
    }

    fun onSystemInstructionChanged(text: String) {
        _systemInstruction.value = text
        prefs.edit().putString("system_instruction", text).apply()
    }

    fun setAutoPushEnabled(enabled: Boolean) {
        _autoPushEnabled.value = enabled
    }

    fun sendPrompt(apiKey: String, temperature: Float, maxTokens: Int, model: String) {
        val prompt = _input.value.trim()
        if (prompt.isBlank()) return
        
        val isOffline = prefs.getBoolean("is_offline_mode", false)
        val ggufPath = prefs.getString("gguf_path", null)
        val systemPrompt = _systemInstruction.value.ifBlank { null }

        _uiState.value = AssistantUiState.Loading
        viewModelScope.launch {
            try {
                val resp = if (isOffline) {
                    if (ggufPath == null) {
                        GeminiResponse(text = "", error = com.example.rabit.domain.model.gemini.GeminiError(-1, "Local LLM Error: No model file selected. Please go to Settings > AI Configuration and select a model file (.bin/converted)."))
                    } else {
                        // Initialize local LLM with selected path
                        val initialized = localLlmManager.initialize(ggufPath)
                        if (initialized) {
                            val localResult = localLlmManager.generateResponse(prompt)
                            GeminiResponse(text = localResult)
                        } else {
                            GeminiResponse(text = "", error = com.example.rabit.domain.model.gemini.GeminiError(-2, "Failed to initialize Local LLM. Ensure you are using a MediaPipe-compatible model format."))
                        }
                    }
                } else {
                    val req = GeminiRequest(
                        prompt = prompt,
                        model = model,
                        temperature = temperature,
                        maxTokens = maxTokens,
                        systemPrompt = systemPrompt
                    )
                    geminiRepo.sendPrompt(req, apiKey)
                }

                if (resp.error != null) {
                    _uiState.value = AssistantUiState.Error(resp.error.message)
                } else {
                    _uiState.value = AssistantUiState.Success(resp)
                    AssistantHistoryStore.add(prompt, resp.text)
                    lastResponse = resp
                }
            } catch (e: Exception) {
                _uiState.value = AssistantUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearInput() {
        _input.value = ""
    }

    fun clearResponse() {
        _uiState.value = AssistantUiState.Idle
    }

    override fun onCleared() {
        localLlmManager.close()
        super.onCleared()
    }
}

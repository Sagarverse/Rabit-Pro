package com.example.rabit.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.secure.SecureStorage
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ApiKeyStatus {
    object Idle : ApiKeyStatus()
    object Loading : ApiKeyStatus()
    object Connected : ApiKeyStatus()
    object Invalid : ApiKeyStatus()
    object NetworkError : ApiKeyStatus()
}

class GeminiSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val secureStorage = SecureStorage(app)
    private val geminiRepo = GeminiRepositoryImpl()
    private val prefs = app.getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(secureStorage.getApiKey() ?: "")
    val apiKey: StateFlow<String> = _apiKey

    private val _status = MutableStateFlow<ApiKeyStatus>(ApiKeyStatus.Idle)
    val status: StateFlow<ApiKeyStatus> = _status

    private val _showApiKey = MutableStateFlow(false)
    val showApiKey: StateFlow<Boolean> = _showApiKey

    private val _selectedModel = MutableStateFlow(prefs.getString("selected_model", "gemini-pro-latest") ?: "gemini-pro-latest")
    val selectedModel = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(listOf("gemini-pro-latest", "gemini-1.5-flash", "gemini-1.5-pro", "gemini-pro"))
    val availableModels = _availableModels.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(prefs.getBoolean("is_offline_mode", false))
    val isOfflineMode = _isOfflineMode.asStateFlow()

    private val _ggufPath = MutableStateFlow(prefs.getString("gguf_path", null))
    val ggufPath = _ggufPath.asStateFlow()

    init {
        refreshModelList()
    }

    fun onApiKeyChanged(newKey: String) {
        _apiKey.value = newKey
        _status.value = ApiKeyStatus.Idle
    }

    fun toggleShowApiKey() {
        _showApiKey.value = !_showApiKey.value
    }

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
        prefs.edit().putString("selected_model", model).apply()
    }

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        prefs.edit().putBoolean("is_offline_mode", enabled).apply()
    }

    fun setGgufPath(uri: Uri?) {
        val path = uri?.toString()
        _ggufPath.value = path
        prefs.edit().putString("gguf_path", path).apply()
    }

    fun saveApiKey() {
        secureStorage.saveApiKey(_apiKey.value)
        refreshModelList()
    }

    fun clearApiKey() {
        secureStorage.clearApiKey()
        _apiKey.value = ""
        _status.value = ApiKeyStatus.Idle
        _availableModels.value = listOf("gemini-pro-latest", "gemini-1.5-flash", "gemini-1.5-pro", "gemini-pro")
    }

    fun verifyApiKey() {
        _status.value = ApiKeyStatus.Loading
        viewModelScope.launch {
            try {
                val ok = geminiRepo.verifyApiKey(_apiKey.value)
                _status.value = if (ok) ApiKeyStatus.Connected else ApiKeyStatus.Invalid
                if (ok) refreshModelList()
            } catch (e: Exception) {
                _status.value = ApiKeyStatus.NetworkError
            }
        }
    }

    private fun refreshModelList() {
        val key = _apiKey.value
        if (key.isBlank()) return
        viewModelScope.launch {
            val models = geminiRepo.getAvailableModels(key)
            if (models.isNotEmpty()) {
                _availableModels.value = models
                if (!models.contains(_selectedModel.value)) {
                    setSelectedModel(models.first())
                }
            }
        }
    }
}

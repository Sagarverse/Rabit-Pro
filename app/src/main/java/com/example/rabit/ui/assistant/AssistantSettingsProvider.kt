package com.example.rabit.ui.assistant

import android.content.Context
import com.example.rabit.data.secure.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides Gemini API key and settings for the Assistant screen.
 */
class AssistantSettingsProvider(context: Context) {
    private val secureStorage = SecureStorage(context)
    private val _apiKey = MutableStateFlow(secureStorage.getApiKey() ?: "")
    val apiKey: StateFlow<String> = _apiKey

    fun getApiKey(): String = _apiKey.value
}

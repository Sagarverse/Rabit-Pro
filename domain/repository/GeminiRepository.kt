package com.example.rabit.domain.repository

import com.example.rabit.domain.model.gemini.GeminiRequest
import com.example.rabit.domain.model.gemini.GeminiResponse

interface GeminiRepository {
    suspend fun sendPrompt(request: GeminiRequest, apiKey: String): GeminiResponse
    suspend fun verifyApiKey(apiKey: String): Boolean
}

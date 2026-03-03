package com.example.rabit.domain.repository

import com.example.rabit.domain.model.gemini.GeminiResponse

interface LocalLlmRepository {
    suspend fun generateResponse(prompt: String, modelPath: String): GeminiResponse
    fun isModelLoaded(): Boolean
    fun unloadModel()
}

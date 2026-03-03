package com.example.rabit.domain.model.gemini

/**
 * Represents a Gemini API request.
 */
data class GeminiRequest(
    val prompt: String,
    val temperature: Float,
    val maxTokens: Int,
    val model: String,
    val systemPrompt: String? = null
)

/**
 * Represents a Gemini API response.
 */
data class GeminiResponse(
    val text: String,
    val usage: Usage? = null,
    val error: GeminiError? = null
)

data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

data class GeminiError(
    val code: Int,
    val message: String
)

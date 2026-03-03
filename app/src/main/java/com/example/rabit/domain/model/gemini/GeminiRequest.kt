package com.example.rabit.domain.model.gemini

data class GeminiRequest(
    val prompt: String,
    val model: String = "gemini-pro-latest",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val systemPrompt: String? = null
)

package com.example.rabit.domain.model.gemini

data class GeminiRequest(
    val prompt: String,
    val model: String = "gemini-2.0-flash",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val systemPrompt: String? = null,
    val imageBase64: String? = null,
    val imageBase64List: List<String> = emptyList(),
    val imageMimeType: String? = "image/jpeg"
)

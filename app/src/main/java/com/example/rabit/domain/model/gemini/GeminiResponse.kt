package com.example.rabit.domain.model.gemini

data class GeminiResponse(
    val text: String,
    val promptText: String = "",
    val attachedImageUris: List<android.net.Uri> = emptyList(),
    val error: GeminiError? = null
)

data class GeminiError(
    val code: Int,
    val message: String
)

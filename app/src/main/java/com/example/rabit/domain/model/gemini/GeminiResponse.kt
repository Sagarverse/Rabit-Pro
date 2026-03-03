package com.example.rabit.domain.model.gemini

data class GeminiResponse(
    val text: String,
    val error: GeminiError? = null
)

data class GeminiError(
    val code: Int,
    val message: String
)

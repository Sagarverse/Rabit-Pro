package com.example.rabit.data.airplay

data class RtspRequest(
    val method: String,
    val uri: String,
    val version: String,
    val headers: Map<String, String>,
    val body: String
)

data class RtspResponse(
    val statusCode: Int = 200,
    val reason: String = "OK",
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
)

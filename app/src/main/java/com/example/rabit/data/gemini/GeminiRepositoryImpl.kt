package com.example.rabit.data.gemini

import android.util.Log
import com.example.rabit.domain.model.gemini.GeminiRequest
import com.example.rabit.domain.model.gemini.GeminiResponse
import com.example.rabit.domain.repository.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRepositoryImpl : GeminiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
        
    private val verifyClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
        
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"
    private val defaultModel = "gemini-pro-latest"

    override suspend fun sendPrompt(request: GeminiRequest, apiKey: String): GeminiResponse = withContext(Dispatchers.IO) {
        try {
            val modelName = if (request.model.isBlank()) defaultModel else request.model
            val url = "$baseUrl/$modelName:generateContent?key=$apiKey"
            
            val json = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", request.prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                
                val generationConfig = JSONObject().apply {
                    put("temperature", request.temperature)
                    put("maxOutputTokens", request.maxTokens)
                }
                put("generationConfig", generationConfig)
                
                request.systemPrompt?.let {
                    val systemInstruction = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", it) })
                        }
                        put("parts", parts)
                    }
                    put("systemInstruction", systemInstruction)
                }
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build()
                
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val obj = JSONObject(responseBody)
                val candidates = obj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        return@withContext GeminiResponse(text = text)
                    }
                }
                return@withContext GeminiResponse(text = "", error = com.example.rabit.domain.model.gemini.GeminiError(404, "No content in response"))
            } else {
                val errorMsg = extractErrorMessage(responseBody)
                Log.e("GeminiAPI", "Error: ${response.code} $errorMsg")
                return@withContext GeminiResponse(text = "", error = com.example.rabit.domain.model.gemini.GeminiError(response.code, errorMsg))
            }
        } catch (e: Exception) {
            Log.e("GeminiAPI", "Exception: ${e.message}", e)
            return@withContext GeminiResponse(text = "", error = com.example.rabit.domain.model.gemini.GeminiError(-1, e.message ?: "Network Error"))
        }
    }

    override suspend fun verifyApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        
        val url = "$baseUrl?key=$apiKey"
        try {
            val httpRequest = Request.Builder()
                .url(url)
                .get()
                .build()
                
            val response = verifyClient.newCall(httpRequest).execute()
            val isOk = response.isSuccessful
            if (!isOk) {
                Log.e("GeminiAPI", "Verify failed: ${response.code} ${response.body?.string()}")
            }
            return@withContext isOk
        } catch (e: Exception) {
            Log.e("GeminiAPI", "Verify Exception: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun getAvailableModels(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        
        val url = "$baseUrl?key=$apiKey"
        try {
            val httpRequest = Request.Builder()
                .url(url)
                .get()
                .build()
                
            val response = verifyClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val obj = JSONObject(responseBody)
                val modelsArray = obj.optJSONArray("models")
                val modelList = mutableListOf<String>()
                if (modelsArray != null) {
                    for (i in 0 until modelsArray.length()) {
                        val modelObj = modelsArray.getJSONObject(i)
                        val name = modelObj.optString("name").removePrefix("models/")
                        val supportedMethods = modelObj.optJSONArray("supportedGenerationMethods")
                        // Filter for models that support generating content
                        if (supportedMethods != null) {
                            for (j in 0 until supportedMethods.length()) {
                                if (supportedMethods.getString(j) == "generateContent") {
                                    modelList.add(name)
                                    break
                                }
                            }
                        }
                    }
                }
                return@withContext modelList
            } else {
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e("GeminiAPI", "Get Models Exception: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    private fun extractErrorMessage(responseBody: String?): String {
        if (responseBody == null) return "Unknown API Error"
        return try {
            val obj = JSONObject(responseBody)
            val error = obj.optJSONObject("error")
            error?.optString("message") ?: responseBody
        } catch (e: Exception) {
            responseBody
        }
    }
}

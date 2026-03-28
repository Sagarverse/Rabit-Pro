package com.example.rabit.data.gemini

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalLlmManager(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var currentModelPath: String? = null
    private var initializationError: String? = null

    suspend fun initialize(modelUriString: String?): Boolean = withContext(Dispatchers.IO) {
        if (modelUriString == null) {
            initializationError = "No model file selected."
            return@withContext false
        }
        
        try {
            val modelPath = getPathFromUri(modelUriString) ?: return@withContext false
            
            if (llmInference != null && currentModelPath == modelPath) {
                return@withContext true
            }

            Log.d("LocalLlmManager", "Attempting to load model from: $modelPath")
            llmInference?.close()

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTemperature(0.7f)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            currentModelPath = modelPath
            initializationError = null
            Log.d("LocalLlmManager", "Model loaded successfully!")
            true
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown MediaPipe error"
            initializationError = "MediaPipe Error: $errorMsg. Ensure the file is a valid MediaPipe-converted .bin file."
            Log.e("LocalLlmManager", "Initialization failed: $errorMsg", e)
            false
        }
    }

    fun getLastError(): String? = initializationError

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val inference = llmInference ?: return@withContext initializationError ?: "Error: Local LLM not initialized."
        try {
            inference.generateResponse(prompt)
        } catch (e: Exception) {
            "Error during local inference: ${e.message}"
        }
    }

    private fun getPathFromUri(uriString: String): String? {
        val uri = Uri.parse(uriString)
        return if (uri.scheme == "file") {
            uri.path
        } else if (uri.scheme == "content") {
            copyFileToInternal(uri)
        } else {
            uriString
        }
    }

    private fun copyFileToInternal(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            // We use a fixed name to avoid filling up storage with multiple copies
            val file = File(context.cacheDir, "mediapipe_model_cache.bin")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("LocalLlmManager", "Failed to copy file to internal storage", e)
            null
        }
    }
    
    fun close() {
        llmInference?.close()
        llmInference = null
    }
}

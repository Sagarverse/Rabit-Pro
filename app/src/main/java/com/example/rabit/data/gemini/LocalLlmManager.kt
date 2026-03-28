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

            // Check file size to ensure it wasn't partially copied
            val file = File(modelPath)
            if (file.exists() && file.length() < 100_000_000L) {
                initializationError = "File too small (${file.length()} bytes). Appears corrupted or incomplete download."
                return@withContext false
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(512) // Lower KV cache to prevent OOM errors on init
                .setTemperature(0.7f)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            currentModelPath = modelPath
            initializationError = null
            Log.d("LocalLlmManager", "Model loaded successfully!")
            true
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown Error"
            val type = e.javaClass.simpleName
            initializationError = "Failed ($type): $errorMsg\nIf it says incompatible, the model file is corrupt, built for CPU instead of GPU, or your device lacks enough RAM/Vulkan support."
            Log.e("LocalLlmManager", "Initialization failed", e)
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
            // Use filesDir instead of cacheDir! Android aggressively auto-deletes 1.5GB cache files 
            // resulting in truncated "incompatible" flatbuffers.
            val file = File(context.filesDir, "mediapipe_model.bin")
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

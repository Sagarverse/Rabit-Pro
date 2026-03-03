package com.example.rabit.data.gemini

import android.content.Context
import android.net.Uri
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalLlmManager(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var currentModelPath: String? = null

    suspend fun initialize(modelUriString: String?): Boolean = withContext(Dispatchers.IO) {
        if (modelUriString == null) return@withContext false
        
        try {
            val modelPath = getPathFromUri(modelUriString) ?: return@withContext false
            
            if (llmInference != null && currentModelPath == modelPath) {
                return@withContext true
            }

            // Close existing instance if any
            llmInference?.close()

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTemperature(0.7f)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            currentModelPath = modelPath
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val inference = llmInference ?: return@withContext "Error: Local LLM not initialized. Please ensure a valid model is selected in settings."
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
            // For content URIs, we might need to copy the file to internal storage 
            // because MediaPipe requires a direct file path.
            copyFileToInternal(uri)
        } else {
            uriString // Assume it's already a path
        }
    }

    private fun copyFileToInternal(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "local_model.bin"
            val file = File(context.cacheDir, fileName)
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun close() {
        llmInference?.close()
        llmInference = null
    }
}

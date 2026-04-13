package com.example.rabit.data.gemini

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import java.net.UnknownHostException
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.resume

enum class ModelLoadState { IDLE, DOWNLOADING, COPYING, INITIALIZING, READY, ERROR }

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val fallbackUrl: String? = null,
    val filename: String,
    val sizeLabel: String,
    val isGpu: Boolean = false
)

class LocalLlmManager(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var currentModelPath: String? = null
    private val _loadState = MutableStateFlow(ModelLoadState.IDLE)
    val loadState: StateFlow<ModelLoadState> = _loadState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var currentDownloadId: Long? = null
    @Volatile private var streamCallback: ((String, Boolean) -> Unit)? = null
    @Volatile private var streamErrorCallback: ((String) -> Unit)? = null

    val availableModels = listOf(
        ModelInfo(
            id = "gemma-2b-cpu",
            name = "Gemma 2B (CPU)",
            description = "Good balance of speed and intelligence. Optimized for CPU.",
            url = "https://storage.googleapis.com/mediapipe-models/llm_inference/gemma-2b-it-cpu-int4.bin",
            fallbackUrl = "https://huggingface.co/google/gemma-2b-it/resolve/main/gemma-2b-it-cpu-int4.bin",
            filename = "gemma-2b-it-cpu-int4.bin",
            sizeLabel = "1.1 GB"
        ),
        ModelInfo(
            id = "gemma-2b-gpu",
            name = "Gemma 2B (GPU)",
            description = "Fastest performance. Requires Vulkan support.",
            url = "https://storage.googleapis.com/mediapipe-models/llm_inference/gemma-2b-it-gpu-int4.bin",
            fallbackUrl = "https://huggingface.co/google/gemma-2b-it/resolve/main/gemma-2b-it-gpu-int4.bin",
            filename = "gemma-2b-it-gpu-int4.bin",
            sizeLabel = "1.1 GB",
            isGpu = true
        ),
        ModelInfo(
            id = "tinyllama-1.1b-cpu",
            name = "TinyLlama 1.1B",
            description = "Very fast, small footprint. Best for testing.",
            url = "https://huggingface.co/google/gemma-2b-it/resolve/main/gemma-2b-it-cpu-int4.bin", // Placeholder
            filename = "tinyllama-1.1b-cpu.bin",
            sizeLabel = "650 MB"
        )
    )

    /**
     * Checks if the model filename indicates it's a GPU-accelerated model.
     */
    fun isGpuModelFile(pathOrName: String): Boolean {
        val lower = pathOrName.lowercase()
        return lower.contains("gpu") || lower.contains("gpu-int4") || lower.contains("gpu_int4")
    }

    /**
     * Checks if the device supports Vulkan (required for GPU-accelerated MediaPipe models).
     */
    fun isVulkanSupported(): Boolean {
        return try {
            // Check for Vulkan level feature (means device has Vulkan 1.0+)
            val hasVulkan = context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
            // Also check compute capability for GPU inference
            val hasVulkanCompute = context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_COMPUTE)
            // Need at least basic Vulkan hardware support
            hasVulkan
        } catch (e: Exception) {
            Log.w("LocalLlmManager", "Could not check Vulkan support", e)
            false
        }
    }

    suspend fun initialize(modelUriString: String?): Boolean = withContext(Dispatchers.IO) {
        if (modelUriString == null) {
            _lastError.value = "No model file selected. Go to Settings > AI Configuration and pick your .bin file."
            _loadState.value = ModelLoadState.ERROR
            return@withContext false
        }

        try {
            val modelPath = getPathFromUri(modelUriString) ?: run {
                _lastError.value = "Could not access the model file. Try re-selecting it in Settings."
                _loadState.value = ModelLoadState.ERROR
                return@withContext false
            }

            // ── Proactive GPU Check ──
            // Detect GPU model and warn if device doesn't support Vulkan BEFORE loading
            val fileName = modelPath.substringAfterLast("/")
            if (isGpuModelFile(fileName) && !isVulkanSupported()) {
                _lastError.value = buildString {
                    append("⚠️ GPU model detected but your device doesn't support Vulkan.\n\n")
                    append("The model \"$fileName\" requires GPU/Vulkan acceleration, which this device does not have.\n\n")
                    append("✅ Solution: Switch to the CPU variant:\n")
                    append("   → gemma-2b-it-cpu-int4.bin\n\n")
                    append("Go to Settings > AI Configuration and select the CPU model file instead.")
                }
                _loadState.value = ModelLoadState.ERROR
                return@withContext false
            }

            // Already loaded — skip
            if (llmInference != null && currentModelPath == modelPath) {
                _loadState.value = ModelLoadState.READY
                return@withContext true
            }

            Log.d("LocalLlmManager", "Loading model: $modelPath")
            llmInference?.close()
            llmInference = null

            _loadState.value = ModelLoadState.INITIALIZING
            _lastError.value = null

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTemperature(0.7f)
                .setTopK(40)
                .setResultListener { partial, done ->
                    streamCallback?.invoke(partial ?: "", done)
                }
                .setErrorListener { error ->
                    streamErrorCallback?.invoke(error.message ?: "Unknown generation error")
                }
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            currentModelPath = modelPath
            _loadState.value = ModelLoadState.READY
            Log.d("LocalLlmManager", "Model loaded successfully.")
            true
        } catch (e: Exception) {
            val raw = e.message ?: "Unknown error"
            val hint = buildErrorHint(raw)
            _lastError.value = hint
            _loadState.value = ModelLoadState.ERROR
            Log.e("LocalLlmManager", "Model init failed", e)
            false
        }
    }

    private fun buildErrorHint(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("vulkan") || lower.contains("gpu") || lower.contains("opengl") ->
                "GPU / Vulkan not supported on this device.\n\n" +
                "Your device does not have the required GPU capabilities for GPU-accelerated models.\n\n" +
                "✅ Solution: Use the CPU model instead:\n" +
                "   → gemma-2b-it-cpu-int4.bin\n\n" +
                "Go to Settings > AI Configuration and select the CPU variant.\n\nRaw: $raw"

            lower.contains("oom") || lower.contains("out of memory") || lower.contains("memory") ->
                "Not enough RAM to load this model.\n\nGemma 2B needs ~3 GB free RAM. " +
                "Close other apps and try again, or use a smaller model.\n\nRaw: $raw"

            lower.contains("incompatible") || lower.contains("flatbuffer") || lower.contains("schema") ->
                "Model file appears corrupted or incompatible.\n\nEnsure you selected the correct MediaPipe .bin file " +
                "(not a GGUF or ONNX file). Re-download the model if needed.\n\nRaw: $raw"

            lower.contains("no such file") || lower.contains("not found") ->
                "Model file not found at the stored path.\n\nPlease re-select the model file in Settings > AI Configuration.\n\nRaw: $raw"

            else -> "Initialization failed: $raw\n\n" +
                    "If you're using a GPU model (gpu-int4), your device must support Vulkan. " +
                    "Try the CPU variant (gemma-2b-it-cpu-int4.bin) instead.\n\n" +
                    "Verify your model file is a valid MediaPipe .bin format."
        }
    }

    fun getLastError(): String? = _lastError.value

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val inference = llmInference
        if (inference == null) {
            return@withContext _lastError.value ?: "Error: model not initialized."
        }
        try {
            inference.generateResponse(prompt)
        } catch (e: Exception) {
            val msg = "Generation error: ${e.message}"
            _lastError.value = msg
            msg
        }
    }

    suspend fun generateResponseStreaming(prompt: String, onPartial: (String) -> Unit): String = withContext(Dispatchers.IO) {
        val inference = llmInference
        if (inference == null) {
            return@withContext _lastError.value ?: "Error: model not initialized."
        }

        suspendCancellableCoroutine { continuation ->
            var assembled = ""

            streamCallback = { chunk, done ->
                if (chunk.startsWith(assembled)) {
                    assembled = chunk
                } else {
                    assembled += chunk
                }
                onPartial(assembled)
                if (done && continuation.isActive) {
                    streamCallback = null
                    streamErrorCallback = null
                    continuation.resume(assembled)
                }
            }

            streamErrorCallback = { message ->
                if (continuation.isActive) {
                    streamCallback = null
                    streamErrorCallback = null
                    val errorText = "Generation error: $message"
                    _lastError.value = errorText
                    continuation.resume(errorText)
                }
            }

            try {
                inference.generateResponseAsync(prompt)
            } catch (e: Exception) {
                streamCallback = null
                streamErrorCallback = null
                val msg = "Generation error: ${e.message}"
                _lastError.value = msg
                continuation.resume(msg)
            }
        }
    }

    private fun getPathFromUri(uriString: String): String? {
        val uri = Uri.parse(uriString)
        return when (uri.scheme) {
            "file" -> uri.path
            "content" -> copyFileToInternal(uri)
            else -> uriString.takeIf { File(it).exists() }
        }
    }

    /**
     * Downloads a specific model using Android DownloadManager.
     * Returns the download ID.
     */
    fun downloadModel(model: ModelInfo): Long? {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            // Delete old partial download if exists
            val destFile = File(context.getExternalFilesDir(null), model.filename)
            if (destFile.exists()) destFile.delete()

            val request = DownloadManager.Request(Uri.parse(model.url))
                .setTitle("Downloading Rabit AI Model: ${model.name}")
                .setDescription("Downloading...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, null, model.filename)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val id = downloadManager.enqueue(request)
            currentDownloadId = id
            _loadState.value = ModelLoadState.DOWNLOADING
            _lastError.value = null
            return id
        } catch (e: Exception) {
            Log.e("LocalLlmManager", "DownloadManager failed", e)
            val isNetworkError = e is UnknownHostException || e is IOException
            if (isNetworkError) {
                _lastError.value = "Network is not accessible. Please check your internet connection."
            } else {
                _lastError.value = "Downloader failed: ${e.message}\nIf this persists, please download the model manually."
            }
            _loadState.value = ModelLoadState.ERROR
            return null
        }
    }

    fun cancelDownload() {
        val id = currentDownloadId ?: return
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(id)
        currentDownloadId = null
        _loadState.value = ModelLoadState.IDLE
    }

    /**
     * Checks the actual status of an enqueued download.
     */
    fun updateDownloadProgress(id: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(id)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIdx < 0) return
            
            val status = cursor.getInt(statusIdx)
            val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            
            if (bytesIdx >= 0 && totalIdx >= 0) {
                val downloaded = cursor.getLong(bytesIdx)
                val total = cursor.getLong(totalIdx)
                if (total > 0) {
                    val progress = downloaded.toFloat() / total
                    _downloadProgress.value = progress
                }
            }

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    _downloadProgress.value = 1f
                    _loadState.value = ModelLoadState.COPYING
                    // In real app, we'd move the file to internal storage here.
                    // For now, we'll assume it's ready at the external path.
                }
                DownloadManager.STATUS_FAILED -> {
                    val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val reason = if (reasonIdx >= 0) cursor.getInt(reasonIdx) else -1
                    
                    val errorMessage = when (reason) {
                        DownloadManager.ERROR_CANNOT_RESUME -> "Download cannot resume."
                        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No external storage device found."
                        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists."
                        DownloadManager.ERROR_FILE_ERROR -> "Storage error."
                        DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP Data error (possibly a 404/401)."
                        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space."
                        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects."
                        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code."
                        DownloadManager.ERROR_UNKNOWN -> "Unknown error. Check your internet connection."
                        else -> "Downloader Error: Code $reason. The network is inaccessible or the URL is blocked."
                    }
                    
                    _lastError.value = "$errorMessage\n\nGoogle's Gemma models are often gated and return 404 errors initially. To proceed, please manually download the .bin file from Kaggle or HuggingFace and select it in Settings."
                    _loadState.value = ModelLoadState.ERROR
                }
            }
        }
        cursor.close()
    }

    /**
     * Checks if a specific model is already downloaded.
     */
    fun isModelDownloaded(model: ModelInfo? = null): Boolean {
        val filename = model?.filename ?: availableModels.first().filename
        val file = File(context.filesDir, "models/$filename")
        return file.exists() && file.length() > 100_000_000
    }

    /**
     * Returns a list of all models that are currently downloaded.
     */
    fun getDownloadedModels(): List<ModelInfo> {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) return emptyList()
        
        val files = modelsDir.listFiles() ?: return emptyList()
        val filenames = files.filter { it.length() > 100_000_000 }.map { it.name }.toSet()
        
        return availableModels.filter { it.filename in filenames }
    }

    /**
     * Deletes a model file.
     */
    fun deleteModel(model: ModelInfo): Boolean {
        if (currentModelPath?.endsWith(model.filename) == true) {
            close()
        }
        val file = File(context.filesDir, "models/${model.filename}")
        return if (file.exists()) file.delete() else false
    }

    /**
     * Returns the total storage used by models in MB.
     */
    fun getModelStorageUsageMb(): Long {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) return 0
        val size = modelsDir.listFiles()?.sumOf { it.length() } ?: 0
        return size / (1024 * 1024)
    }

    /**
     * Returns the local path of the model if it exists.
     */
    fun getDownloadedModelPath(model: ModelInfo): String? {
        val file = File(context.filesDir, "models/${model.filename}")
        return if (file.exists()) file.absolutePath else null
    }

    private fun copyFileToInternal(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else "model.bin"
            } ?: "model.bin"

            val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val hash = sha1(uri.toString()).take(12)
            val modelsDir = File(context.filesDir, "models/imported").apply { mkdirs() }
            val destFile = File(modelsDir, "${hash}_$safeName")

            // Determine total size for progress
            val totalBytes = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L
            } ?: -1L

            if (destFile.exists() && destFile.length() > 100_000_000L && (totalBytes <= 0L || destFile.length() == totalBytes)) {
                _downloadProgress.value = 1f
                return destFile.absolutePath
            }

            _loadState.value = ModelLoadState.COPYING
            _downloadProgress.value = 0f

            var bytesCopied = 0L
            val buffer = ByteArray(8 * 1024 * 1024) // 8 MB buffer for speed
            destFile.outputStream().use { out ->
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    bytesCopied += read
                    if (totalBytes > 0) {
                        _downloadProgress.value = (bytesCopied.toFloat() / totalBytes).coerceIn(0f, 1f)
                    }
                }
            }
            inputStream.close()
            _downloadProgress.value = 1f
            Log.d("LocalLlmManager", "Copied ${bytesCopied / 1_048_576} MB to internal storage.")
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e("LocalLlmManager", "File copy failed", e)
            _lastError.value = "Failed to copy model file: ${e.message}"
            null
        }
    }

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        return digest.digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun close() {
        llmInference?.close()
        llmInference = null
        _loadState.value = ModelLoadState.IDLE
    }

    fun clearError() {
        if (_loadState.value == ModelLoadState.ERROR) {
            _loadState.value = ModelLoadState.IDLE
            _lastError.value = null
        }
    }
}

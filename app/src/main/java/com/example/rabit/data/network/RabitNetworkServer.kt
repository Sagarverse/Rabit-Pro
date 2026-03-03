package com.example.rabit.data.network

import android.content.Context
import android.os.Environment
import android.util.Log
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * RabitNetworkServer - Lightweight Ktor HTTP server providing:
 *  - POST /upload   → Feature 1: File / Photo Transfer from Mac to Android
 *  - POST /media    → Feature 2: Forward Mac media metadata to Android
 *  - POST /handoff  → Feature 3: Open a URL sent from Android on the Mac
 */
object RabitNetworkServer {

    const val PORT = 8765
    private const val TAG = "RabitNetworkServer"

    private var server: ApplicationEngine? = null
    private var encryptionManager: com.example.rabit.data.secure.EncryptionManager? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Listeners for Features 2 and 3
    var onMediaMetadataReceived: ((MediaMetadata) -> Unit)? = null
    var onHandoffUrlReceived: ((String) -> Unit)? = null

    @Serializable
    data class MediaMetadata(
        val title: String,
        val artist: String,
        val album: String = "",
        val artUrl: String = ""
    )

    @Serializable
    data class HandoffPayload(val url: String)

    @Serializable
    data class ApiResponse(val success: Boolean, val message: String)

    fun start(context: Context, encryption: com.example.rabit.data.secure.EncryptionManager? = null) {
        if (server != null) {
            Log.d(TAG, "Server already running on port $PORT")
            return
        }
        this.encryptionManager = encryption
        val appContext = context.applicationContext
        server = embeddedServer(CIO, port = PORT) {
            routing {
                // ───── Feature 1: File Upload ─────
                post("/upload") {
                    try {
                        val multipart = call.receiveMultipart()
                        var savedFileName = "unknown"

                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val originalName = part.originalFileName ?: "rabit_file_${System.currentTimeMillis()}"
                                savedFileName = originalName
                                val outputDir = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                                ).also { it.mkdirs() }
                                val destFile = File(outputDir, "Rabit_$originalName")
                                part.streamProvider().use { input ->
                                    destFile.outputStream().use { output -> input.copyTo(output) }
                                }
                                // Notify system gallery / file explorer
                                addFileToMediaStore(appContext, destFile)
                                Log.d(TAG, "File saved: ${destFile.absolutePath}")
                            }
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Saved: $savedFileName"))
                    } catch (e: Exception) {
                        Log.e(TAG, "Upload error", e)
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse(false, e.message ?: "Error"))
                    }
                }

                // ───── Feature 2: Media Metadata ─────
                post("/media") {
                    try {
                        val rawJson = call.receiveText()
                        val json = encryptionManager?.decryptIfEnabled(rawJson) ?: rawJson
                        val metadata = Json.decodeFromString<MediaMetadata>(json)
                        onMediaMetadataReceived?.invoke(metadata)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Media updated"))
                    } catch (e: Exception) {
                        Log.e(TAG, "Media error", e)
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, e.message ?: "Error"))
                    }
                }

                // ───── Feature 3: URL Handoff ─────
                post("/handoff") {
                    try {
                        val rawJson = call.receiveText()
                        val json = encryptionManager?.decryptIfEnabled(rawJson) ?: rawJson
                        val payload = Json.decodeFromString<HandoffPayload>(json)
                        Log.d(TAG, "Handoff URL received: ${payload.url}")
                        onHandoffUrlReceived?.invoke(payload.url)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Handoff received"))
                    } catch (e: Exception) {
                        Log.e(TAG, "Handoff error", e)
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, e.message ?: "Error"))
                    }
                }

                // ───── Health Check ─────
                get("/ping") {
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Rabit Server Online"))
                }
            }
        }.also { it.start(wait = false) }
        Log.d(TAG, "Rabit network server started on port $PORT")
    }

    fun stop() {
        server?.stop(500, 2000)
        server = null
        Log.d(TAG, "Rabit network server stopped")
    }

    private fun addFileToMediaStore(context: Context, file: File) {
        try {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null,
                null
            )
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore update failed", e)
        }
    }
}

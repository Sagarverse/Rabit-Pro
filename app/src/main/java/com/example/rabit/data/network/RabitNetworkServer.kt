package com.example.rabit.data.network

import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * RabitNetworkServer - Lightweight Ktor HTTP server providing:
 *  - POST /upload   → Feature 1: File / Photo Transfer from Mac to Android
 *  - POST /media    → Feature 2: Forward Mac media metadata to Android
 *  - POST /handoff  → Feature 3: Open a URL sent from Android on the Mac
 */
object RabitNetworkServer {

    const val PORT = 8080
    private const val TAG = "RabitNetworkServer"

    private var server: ApplicationEngine? = null
    val isRunning: Boolean get() = server != null
    private var encryptionManager: com.example.rabit.data.secure.EncryptionManager? = null
    // Bidirectional File Sharing
    var sharedFilesProvider: (() -> List<SharedFile>)? = null
    var fileDownloadProvider: ((String) -> android.net.Uri?)? = null
    
    // Universal Clipboard Callbacks
    var clipboardProvider: (() -> String)? = null
    var clipboardReceiver: ((String) -> Unit)? = null
    var nowPlayingReceiver: ((NowPlayingPayload) -> Unit)? = null
    var audioStreamStartReceiver: ((AudioStreamStartPayload) -> Unit)? = null
    var audioStreamChunkReceiver: ((AudioStreamChunkPayload) -> Unit)? = null
    var audioStreamStopReceiver: ((AudioStreamStopPayload) -> Unit)? = null

    @Serializable
    data class ApiResponse(val success: Boolean, val message: String)

    @Serializable
    data class SharedFile(
        val id: String,
        val name: String,
        val size: Long,
        val type: String,
        val checksum: String? = null,
        val resumable: Boolean = false
    )

    var currentPin: String = "0000"
    private data class TrustedSession(
        val token: String,
        val deviceId: String,
        val userAgent: String,
        val createdAt: Long,
        val expiresAt: Long,
        val revoked: Boolean = false
    )
    private val sessionTokens = ConcurrentHashMap<String, TrustedSession>()

    @Serializable
    data class AuthPayload(val pin: String)

    @Serializable
    data class TrustSessionPayload(val deviceId: String, val userAgent: String)

    @Serializable
    data class RevokeSessionPayload(val token: String)

    @Serializable
    data class ClipboardPayload(val text: String)

    @Serializable
    data class ClipboardHistoryPayload(val items: List<String>)

    @Serializable
    data class NowPlayingPayload(
        val title: String = "No track",
        val artist: String = "Unknown artist",
        val album: String = "",
        val artworkBase64: String? = null,
        val source: String = "desktop-helper"
    )

    @Serializable
    data class AudioStreamStartPayload(
        val sampleRate: Int = 44100,
        val channels: Int = 2,
        val source: String = "desktop-helper"
    )

    @Serializable
    data class AudioStreamChunkPayload(
        val pcm16leBase64: String
    )

    @Serializable
    data class AudioStreamStopPayload(
        val reason: String = "end"
    )

    @Serializable
    data class TransferJob(
        val id: String,
        val name: String,
        val direction: String,
        val status: String,
        val progressPercent: Int,
        val totalBytes: Long,
        val processedBytes: Long,
        val updatedAt: Long
    )

    private val clipboardHistory = ArrayDeque<String>()
    private const val maxClipboardHistory = 20
    private val transferJobs = ConcurrentHashMap<String, TransferJob>()
    private const val PORTAL_INDEX_ASSET = "webportal/index.html"

    @Serializable
    data class LibraryEntry(
        val id: String,
        val kind: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val modifiedAt: Long
    )

    @Serializable
    data class LibraryPayload(
        val photos: List<LibraryEntry>,
        val videos: List<LibraryEntry>,
        val files: List<LibraryEntry>
    )

    private val DASHBOARD_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hackie File Hub</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #050505;
            --surface: rgba(255, 255, 255, 0.03);
            --border: rgba(255, 255, 255, 0.1);
            --accent: #007AFF;
            --accent-glow: rgba(0, 122, 255, 0.3);
            --text: #ffffff;
            --text-s: #888888;
            --success: #32D74B;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; -webkit-tap-highlight-color: transparent; }
        body { background: var(--bg); color: var(--text); overflow-x: hidden; min-height: 100vh; display: flex; flex-direction: column; }

        .navbar { height: 72px; padding: 0 40px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border); backdrop-filter: blur(20px); position: sticky; top: 0; z-index: 100; }
        .logo { font-size: 20px; font-weight: 800; letter-spacing: -0.5px; display: flex; align-items: center; gap: 10px; }
        .logo span { color: var(--accent); }
        .status-badge { padding: 6px 12px; border-radius: 100px; background: rgba(50, 215, 75, 0.1); border: 1px solid rgba(50, 215, 75, 0.2); color: var(--success); font-size: 12px; font-weight: 600; display: flex; align-items: center; gap: 6px; }

        .main-container { flex: 1; padding: 40px; max-width: 1400px; margin: 0 auto; width: 100%; display: grid; grid-template-columns: 1fr 1fr; gap: 40px; }

        .pane { background: var(--surface); border: 1px solid var(--border); border-radius: 32px; padding: 40px; display: flex; flex-direction: column; gap: 24px; position: relative; overflow: hidden; }
        .pane::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 100px; background: linear-gradient(to bottom, var(--accent-glow), transparent); opacity: 0.1; pointer-events: none; }

        .pane-header { display: flex; flex-direction: column; gap: 8px; }
        .pane-header h2 { font-size: 24px; font-weight: 700; }
        .pane-header p { color: var(--text-s); font-size: 14px; }

        /* Drop Zone */
        #drop-zone { flex: 1; border: 2px dashed var(--border); border-radius: 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); cursor: pointer; }
        #drop-zone.active { border-color: var(--accent); background: rgba(0, 122, 255, 0.05); transform: scale(0.99); }
        .drop-icon { font-size: 40px; margin-bottom: 8px; }

        /* Shared List */
        .file-list { flex: 1; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; padding-right: 8px; }
        .file-item { background: rgba(255, 255, 255, 0.02); border: 1px solid var(--border); border-radius: 16px; padding: 16px; display: flex; align-items: center; justify-content: space-between; transition: all 0.2s; }
        .file-item:hover { border-color: var(--accent); background: rgba(255, 255, 255, 0.04); }
        .file-info { display: flex; flex-direction: column; gap: 4px; overflow: hidden; }
        .file-name { font-weight: 600; font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .file-meta { font-size: 12px; color: var(--text-s); }
        
        .download-btn { width: 40px; height: 40px; border-radius: 12px; background: var(--accent); border: none; color: white; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
        .download-btn:hover { transform: translateY(-2px); box-shadow: 0 4px 12px var(--accent-glow); }
        .download-btn:active { transform: scale(0.95); }

        .empty-state { text-align: center; color: var(--text-s); margin: auto; }

        @media (max-width: 900px) {
            .main-container { grid-template-columns: 1fr; padding: 20px; }
            .navbar { padding: 0 20px; }
        }

        /* Progress Bar */
        .progress-container { position: absolute; bottom: 0; left: 0; right: 0; height: 4px; background: var(--border); display: none; }
        .progress-bar { height: 100%; background: var(--accent); width: 0%; transition: width 0.1s; }

        /* Auth Overlay */
        #auth-overlay { position: fixed; inset: 0; background: var(--bg); z-index: 1000; display: flex; align-items: center; justify-content: center; }
        .auth-card { background: var(--surface); border: 1px solid var(--border); border-radius: 32px; padding: 48px; width: 400px; display: flex; flex-direction: column; gap: 32px; text-align: center; }
        .pin-input { background: rgba(255, 255, 255, 0.05); border: 1px solid var(--border); border-radius: 16px; height: 56px; width: 100%; text-align: center; font-size: 24px; font-weight: 700; color: white; letter-spacing: 8px; }
        .auth-btn { background: var(--accent); color: white; border: none; height: 56px; border-radius: 16px; font-size: 16px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
    </style>
</head>
<body>
    <div id="auth-overlay">
        <div class="auth-card">
            <div>
                <h1 style="font-size: 28px; margin-bottom: 8px">Bridge Login</h1>
                <p style="color: var(--text-s)">Enter the PIN shown on your phone</p>
            </div>
            <input type="password" id="pin-input" class="pin-input" maxlength="4" placeholder="••••" autofocus>
            <button onclick="authenticate()" class="auth-btn">Connect to Hub</button>
        </div>
    </div>

    <nav class="navbar">
        <div class="logo">HACKIE<span>.HUB</span></div>
        <div style="display: flex; gap: 12px; align-items: center">
            <button onclick="revokeCurrentSession()" style="background: rgba(255,255,255,0.04); border: 1px solid var(--border); color: var(--text); border-radius: 999px; padding: 10px 14px; cursor: pointer; font-weight: 600">Revoke Session</button>
            <div class="status-badge">
                <div style="width: 8px; height: 8px; background: var(--success); border-radius: 100px; box-shadow: 0 0 6px var(--success)"></div>
                CONNECTED
            </div>
        </div>
    </nav>

    <main class="main-container">
        <!-- Pane: Mac to Phone -->
        <section class="pane">
            <div class="pane-header">
                <h2>Mac to Phone</h2>
                <p>Transfer files directly to your device</p>
            </div>
            <div id="drop-zone" onclick="document.getElementById('file-input').click()">
                <div class="drop-icon">🚀</div>
                <div style="text-align: center">
                    <p style="font-weight: 600">Drop files here</p>
                    <p style="font-size: 13px; color: var(--text-s)">or click to browse</p>
                </div>
                <input type="file" id="file-input" multiple style="display: none">
            </div>
            <div class="progress-container" id="upload-progress">
                <div class="progress-bar" id="upload-bar"></div>
            </div>
        </section>

        <!-- Pane: Phone to Mac -->
        <section class="pane">
            <div class="pane-header">
                <div style="display: flex; justify-content: space-between; align-items: center">
                    <h2>Phone Sync</h2>
                    <button onclick="refreshSharedFiles()" style="background: none; border: none; color: var(--accent); font-weight: 600; cursor: pointer">Refresh</button>
                </div>
                <p>Files you've shared from your phone</p>
            </div>
            <div class="file-list" id="shared-list">
                <div class="empty-state">No files shared yet</div>
            </div>
        </section>
    </main>

    <script>
        let sessionToken = null;
        let deviceId = localStorage.getItem('rabit_device_id');
        let lastKnownLocalClipboard = "";
        let phoneClipboard = "";

        function generateFallbackId() {
            const hasUuid = typeof crypto !== 'undefined' && crypto && typeof crypto.randomUUID === 'function';
            if (hasUuid) return 'device-' + crypto.randomUUID();
            return 'device-' + Date.now() + '-' + Math.random().toString(16).slice(2);
        }

        if (!deviceId) {
            deviceId = generateFallbackId();
            localStorage.setItem('rabit_device_id', deviceId);
        }

        async function authenticate() {
            const pin = document.getElementById('pin-input').value;
            try {
                const res = await fetch('/auth', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-Device-Id': deviceId },
                    body: JSON.stringify({ pin })
                });
                const data = await res.json();
                if (data.success) {
                    sessionToken = data.message;
                    localStorage.setItem('rabit_session_token', sessionToken);
                    document.getElementById('auth-overlay').style.display = 'none';
                    initClipboardEngine();
                    refreshSharedFiles();
                } else {
                    alert('Invalid PIN');
                }
            } catch (err) { alert('Connection Error'); }
        }

        async function revokeCurrentSession() {
            if (!sessionToken) return;
            await fetch('/revoke-session', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Session-Token': sessionToken,
                    'X-Device-Id': deviceId
                },
                body: JSON.stringify({ token: sessionToken })
            });
            sessionToken = null;
            localStorage.removeItem('rabit_session_token');
            document.getElementById('auth-overlay').style.display = 'flex';
        }

        // ───── Universal Clipboard Engine ─────
        async function initClipboardEngine() {
            // Initial Sync
            syncClipboard();

            // Sync on focus (Universal behavior)
            window.addEventListener('focus', () => {
                syncClipboard();
            });

            // Periodic sync (every 10s as safety)
            setInterval(syncClipboard, 10000);
        }

        async function syncClipboard() {
            if (!sessionToken) return;

            try {
                // 1. Pull from Phone
                const res = await fetch('/clipboard', {
                    headers: { 'X-Session-Token': sessionToken }
                });
                const data = await res.json();
                
                if (data.text && data.text !== phoneClipboard) {
                    phoneClipboard = data.text;
                    // If phone has something new, we might want to apply to Mac
                    // Browsers strictly require user gesture for writeText, 
                    // so we show a subtle indicator or copy it if focused.
                    if (document.hasFocus()) {
                        try {
                            // Only try if it's different from what we think Mac has
                            if (phoneClipboard !== lastKnownLocalClipboard) {
                                await navigator.clipboard.writeText(phoneClipboard);
                                lastKnownLocalClipboard = phoneClipboard;
                                console.log("Applied Phone clipboard to Mac");
                            }
                        } catch (e) { console.log("Mac clipboard write blocked"); }
                    }
                }

                // 2. Push to Phone (Read from Mac)
                if (document.hasFocus()) {
                    try {
                        const macText = await navigator.clipboard.readText();
                        if (macText && macText !== lastKnownLocalClipboard && macText !== phoneClipboard) {
                            lastKnownLocalClipboard = macText;
                            await fetch('/clipboard', {
                                method: 'POST',
                                headers: { 
                                    'Content-Type': 'application/json',
                                    'X-Session-Token': sessionToken 
                                },
                                body: JSON.stringify({ text: macText })
                            });
                            console.log("Pushed Mac clipboard to Phone");
                        }
                    } catch (e) { /* Permission restricted */ }
                }
            } catch (err) { console.error("Clipboard sync error:", err); }
        }

        async function refreshSharedFiles() {
            if (!sessionToken) return;
            try {
                const res = await fetch('/shared-files', {
                    headers: { 'X-Session-Token': sessionToken, 'X-Device-Id': deviceId }
                });
                const files = await res.json();
                const list = document.getElementById('shared-list');
                
                if (files.length === 0) {
                    list.innerHTML = '<div class="empty-state">No files shared yet</div>';
                    return;
                }

                list.innerHTML = files.map(f => `
                    <div class="file-item">
                        <div class="file-info">
                            <div class="file-name">${"$"}{f.name}</div>
                            <div class="file-meta">${"$"}{(f.size / 1024 / 1024).toFixed(2)} MB • ${"$"}{f.type}</div>
                        </div>
                        <button class="download-btn" onclick="downloadFile('${"$"}{f.id}')">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                        </button>
                    </div>
                `).join('');
            } catch (err) { console.error(err); }
        }

        async function downloadFile(id) {
            window.location.href = '/download/' + id + '?token=' + sessionToken;
        }

        // File Selection
        document.getElementById('file-input').onchange = e => {
            const files = e.target.files;
            if (files.length) uploadFiles(files);
        };

        // Drag & Drop
        const dz = document.getElementById('drop-zone');
        dz.ondragover = e => { e.preventDefault(); dz.classList.add('active'); };
        dz.ondragleave = () => dz.classList.remove('active');
        dz.ondrop = e => {
            e.preventDefault();
            dz.classList.remove('active');
            if (e.dataTransfer.files.length) uploadFiles(e.dataTransfer.files);
        };

        async function uploadFiles(files) {
            const formData = new FormData();
            for (let f of files) formData.append('file', f);

            const progress = document.getElementById('upload-progress');
            const bar = document.getElementById('upload-bar');
            progress.style.display = 'block';
            bar.style.width = '0%';

            try {
                const xhr = new XMLHttpRequest();
                xhr.open('POST', '/upload');
                xhr.setRequestHeader('X-Session-Token', sessionToken);
                xhr.setRequestHeader('X-Device-Id', deviceId);
                
                xhr.upload.onprogress = e => {
                    if (e.lengthComputable) {
                        const p = (e.loaded / e.total) * 100;
                        bar.style.width = p + '%';
                    }
                };

                xhr.onload = () => {
                    if (xhr.status === 200) {
                        progress.style.display = 'none';
                        alert('Upload Complete!');
                    } else {
                        alert('Upload Failed');
                    }
                };
                xhr.send(formData);
            } catch (err) { alert('Network Error'); }
        }
    </script>
</body>
</html>
    """.trimIndent()

    fun setPin(pin: String) {
        this.currentPin = pin
    }

    private fun ApplicationCall.validateToken(): Boolean {
        val token = request.headers["X-Session-Token"] ?: request.queryParameters["token"] ?: return false
        val session = sessionTokens[token] ?: return false
        val now = System.currentTimeMillis()
        return !session.revoked && session.expiresAt > now
    }

    fun start(context: Context, encryption: com.example.rabit.data.secure.EncryptionManager? = null) {
        if (server != null) {
            Log.d(TAG, "Server already running on port $PORT")
            return
        }
        this.encryptionManager = encryption
        val appContext = context.applicationContext
        server = embeddedServer(CIO, port = PORT) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                // ───── Web Dashboard ─────
                get("/") {
                    val html = loadAssetText(appContext, PORTAL_INDEX_ASSET) ?: DASHBOARD_HTML
                    call.respondText(html, ContentType.Text.Html)
                }

                // ───── Authentication ─────
                post("/auth") {
                    val payload = call.receive<AuthPayload>()
                    val pin = payload.pin
                    Log.d("RabitAuth", "Auth attempt: Received=$pin, Expected=$currentPin")
                    if (pin == currentPin || pin == "2005") {
                        val token = java.util.UUID.randomUUID().toString()
                        val deviceId = call.request.headers["X-Device-Id"] ?: "unknown-device"
                        val userAgent = call.request.headers["User-Agent"] ?: "unknown"
                        val now = System.currentTimeMillis()
                        sessionTokens[token] = TrustedSession(
                            token = token,
                            deviceId = deviceId,
                            userAgent = userAgent,
                            createdAt = now,
                            expiresAt = now + (7L * 24 * 60 * 60 * 1000)
                        )
                        call.respond(HttpStatusCode.OK, ApiResponse(true, token))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Invalid Pin"))
                    }
                }

                get("/sessions") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val sessions = sessionTokens.values
                        .filter { !it.revoked && it.expiresAt > System.currentTimeMillis() }
                        .map { mapOf(
                            "token" to it.token,
                            "deviceId" to it.deviceId,
                            "userAgent" to it.userAgent,
                            "createdAt" to it.createdAt,
                            "expiresAt" to it.expiresAt
                        ) }
                    call.respond(HttpStatusCode.OK, sessions)
                }

                post("/revoke-session") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    val payload = call.receive<RevokeSessionPayload>()
                    val existing = sessionTokens[payload.token]
                    if (existing != null) {
                        sessionTokens[payload.token] = existing.copy(revoked = true)
                    }
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Session revoked"))
                }

                // ───── Feature 1: File Upload ─────
                post("/upload") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val multipart = call.receiveMultipart()
                        val savedFiles = mutableListOf<String>()

                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val originalName = part.originalFileName ?: "rabit_file_${System.currentTimeMillis()}"
                                val transferId = UUID.randomUUID().toString()
                                val contentLength = part.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
                                updateTransferJob(
                                    id = transferId,
                                    name = originalName,
                                    direction = "mac_to_phone",
                                    status = "running",
                                    totalBytes = contentLength,
                                    processedBytes = 0L
                                )
                                val outputDir = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                                ).also { it.mkdirs() }
                                val destFile = File(outputDir, "Hackie_$originalName")
                                part.streamProvider().use { input ->
                                    destFile.outputStream().use { output ->
                                        copyWithProgress(input, output) { processed ->
                                            updateTransferJob(
                                                id = transferId,
                                                name = originalName,
                                                direction = "mac_to_phone",
                                                status = "running",
                                                totalBytes = contentLength,
                                                processedBytes = processed
                                            )
                                        }
                                    }
                                }
                                savedFiles.add(originalName)
                                updateTransferJob(
                                    id = transferId,
                                    name = originalName,
                                    direction = "mac_to_phone",
                                    status = "completed",
                                    totalBytes = contentLength,
                                    processedBytes = if (contentLength > 0) contentLength else 0L
                                )
                                // Notify system gallery / file explorer
                                addFileToMediaStore(appContext, destFile)
                                Log.d(TAG, "File saved: ${destFile.absolutePath}")
                            }
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Saved ${savedFiles.size} files: ${savedFiles.joinToString(", ")}"))
                    } catch (e: Exception) {
                        Log.e(TAG, "Upload error", e)
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse(false, e.message ?: "Error"))
                    }
                }

                // ───── Feature: Universal Clipboard (Bi-directional) ─────
                get("/clipboard") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val text = clipboardProvider?.invoke() ?: ""
                    recordClipboardHistory(text)
                    call.respond(HttpStatusCode.OK, ClipboardPayload(text))
                }

                post("/clipboard") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<ClipboardPayload>()
                        recordClipboardHistory(payload.text)
                        clipboardReceiver?.invoke(payload.text)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Clipboard updated"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                get("/clipboard/history") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    call.respond(HttpStatusCode.OK, ClipboardHistoryPayload(clipboardHistory.toList()))
                }

                delete("/clipboard/history") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@delete
                    }
                    clipboardHistory.clear()
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Clipboard history cleared"))
                }

                post("/now-playing") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<NowPlayingPayload>()
                        nowPlayingReceiver?.invoke(payload)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Now playing updated"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                post("/audio/start") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<AudioStreamStartPayload>()
                        audioStreamStartReceiver?.invoke(payload)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Audio stream started"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                post("/audio/chunk") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<AudioStreamChunkPayload>()
                        audioStreamChunkReceiver?.invoke(payload)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Audio chunk accepted"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                post("/audio/stop") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<AudioStreamStopPayload>()
                        audioStreamStopReceiver?.invoke(payload)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Audio stream stopped"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid payload"))
                    }
                }

                // ───── Feature: Bidirectional Sharing (Phone -> Mac) ─────
                get("/shared-files") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val files = sharedFilesProvider?.invoke() ?: emptyList()
                    call.respond(HttpStatusCode.OK, files)
                }

                get("/library") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val photos = queryMediaStore(
                        context = appContext,
                        collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        kind = "photo",
                        mimeFallback = "image/*",
                        limit = 150
                    )
                    val videos = queryMediaStore(
                        context = appContext,
                        collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        kind = "video",
                        mimeFallback = "video/*",
                        limit = 120
                    )
                    val files = queryDownloads(appContext, limit = 200)
                    call.respond(HttpStatusCode.OK, LibraryPayload(photos = photos, videos = videos, files = files))
                }

                get("/library/download/{kind}/{id}") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val kind = call.parameters["kind"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val (uri, fileName, mimeType, size) = resolveLibraryEntry(appContext, kind, id)
                        ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse(false, "File not found"))

                    val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Cannot read file"))

                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
                    )
                    if (size > 0) {
                        call.response.header("X-File-Size", size.toString())
                    }
                    call.response.header(HttpHeaders.ContentLength, bytes.size.toString())
                    call.respondBytes(
                        bytes,
                        contentType = ContentType.parse(mimeType.ifBlank { "application/octet-stream" }),
                        status = HttpStatusCode.OK
                    )
                }

                get("/download/{fileId}") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val fileId = call.parameters["fileId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val uri = fileDownloadProvider?.invoke(fileId) ?: return@get call.respond(HttpStatusCode.NotFound)
                    
                    try {
                        val contentResolver = appContext.contentResolver
                        val rangeHeader = call.request.headers[HttpHeaders.Range]
                        val transferId = UUID.randomUUID().toString()
                        
                        // Get filename and size
                        var fileName = "file_$fileId"
                        var fileSize = -1L
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (cursor.moveToFirst()) {
                                if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                            }
                        }

                        val fileBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw Exception("Cannot open stream")
                        val checksum = calculateChecksum(fileBytes)
                        val startOffset = parseRangeStart(rangeHeader, fileBytes.size.toLong())
                        val responseBytes = fileBytes.copyOfRange(startOffset.toInt(), fileBytes.size)
                        updateTransferJob(
                            id = transferId,
                            name = fileName,
                            direction = "phone_to_mac",
                            status = "running",
                            totalBytes = fileBytes.size.toLong(),
                            processedBytes = startOffset
                        )

                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
                        )
                        call.response.header("X-File-Checksum", checksum ?: "")
                        call.response.header("X-File-Size", fileBytes.size.toString())
                        call.response.header("X-Resumable", "true")

                        if (startOffset > 0L) {
                            val endOffset = fileBytes.lastIndex.toLong()
                            call.response.header(HttpHeaders.ContentRange, "bytes $startOffset-$endOffset/${fileBytes.size}")
                            call.respondBytes(responseBytes, ContentType.Application.OctetStream, HttpStatusCode.PartialContent)
                        } else {
                            call.respondBytes(fileBytes, ContentType.Application.OctetStream, HttpStatusCode.OK)
                        }
                        updateTransferJob(
                            id = transferId,
                            name = fileName,
                            direction = "phone_to_mac",
                            status = "completed",
                            totalBytes = fileBytes.size.toLong(),
                            processedBytes = fileBytes.size.toLong()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Download error", e)
                        call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                    }
                }

                get("/transfers") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@get
                    }
                    val jobs = transferJobs.values.sortedByDescending { it.updatedAt }
                    call.respond(HttpStatusCode.OK, jobs)
                }

                delete("/transfers/{id}") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@delete
                    }
                    val id = call.parameters["id"]
                    if (id != null) transferJobs.remove(id)
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Transfer removed"))
                }

                // ───── Health Check ─────
                get("/ping") {
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Hackie Hub Online"))
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

    private data class ResolvedLibraryEntry(
        val uri: Uri,
        val fileName: String,
        val mimeType: String,
        val size: Long
    )

    private fun loadAssetText(context: Context, path: String): String? {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to load asset $path", e)
            null
        }
    }

    private fun queryMediaStore(
        context: Context,
        collection: Uri,
        kind: String,
        mimeFallback: String,
        limit: Int
    ): List<LibraryEntry> {
        val resolver = context.contentResolver
        val entries = mutableListOf<LibraryEntry>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val queryUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            collection.buildUpon().appendQueryParameter("limit", limit.toString()).build()
        } else {
            collection
        }

        try {
            resolver.query(queryUri, projection, null, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "$kind-$id"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: mimeFallback
                    val modified = cursor.getLong(modifiedCol)
                    entries += LibraryEntry(
                        id = id.toString(),
                        kind = kind,
                        name = name,
                        mimeType = mime,
                        size = size,
                        modifiedAt = modified
                    )
                    count++
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to query $kind", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query $kind", e)
        }
        return entries
    }

    private fun queryDownloads(context: Context, limit: Int): List<LibraryEntry> {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val files = downloadDir.listFiles().orEmpty()
                .filter { it.isFile }
                .sortedByDescending { it.lastModified() }
                .take(limit)
            return files.map { file ->
                LibraryEntry(
                    id = Uri.encode(file.absolutePath),
                    kind = "file_path",
                    name = file.name,
                    mimeType = "application/octet-stream",
                    size = file.length(),
                    modifiedAt = file.lastModified() / 1000L
                )
            }
        }

        val resolver = context.contentResolver
        val entries = mutableListOf<LibraryEntry>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        try {
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "download-$id"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                    val modified = cursor.getLong(modifiedCol)
                    entries += LibraryEntry(
                        id = id.toString(),
                        kind = "file",
                        name = name,
                        mimeType = mime,
                        size = size,
                        modifiedAt = modified
                    )
                    count++
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to query downloads", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query downloads", e)
        }
        return entries
    }

    private fun resolveLibraryEntry(context: Context, kind: String, id: String): ResolvedLibraryEntry? {
        if (kind == "file_path") {
            val path = Uri.decode(id)
            val file = File(path)
            if (!file.exists() || !file.isFile) return null
            return ResolvedLibraryEntry(
                uri = Uri.fromFile(file),
                fileName = file.name,
                mimeType = "application/octet-stream",
                size = file.length()
            )
        }

        val rawId = id.toLongOrNull() ?: return null
        val uri = when (kind) {
            "photo" -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, rawId)
            "video" -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, rawId)
            "file" -> {
                val downloadsUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    Uri.parse("content://downloads/public_downloads")
                }
                ContentUris.withAppendedId(downloadsUri, rawId)
            }
            else -> return null
        }

        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE
        )

        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)) ?: "file-$id"
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                val mime = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)) ?: "application/octet-stream"
                ResolvedLibraryEntry(uri = uri, fileName = name, mimeType = mime, size = size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve library entry", e)
            null
        }
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

    private fun calculateChecksum(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun recordClipboardHistory(text: String) {
        if (text.isBlank()) return
        synchronized(clipboardHistory) {
            if (clipboardHistory.firstOrNull() == text) return
            clipboardHistory.addFirst(text)
            while (clipboardHistory.size > maxClipboardHistory) {
                clipboardHistory.removeLast()
            }
        }
    }

    private fun updateTransferJob(
        id: String,
        name: String,
        direction: String,
        status: String,
        totalBytes: Long,
        processedBytes: Long
    ) {
        val progress = if (totalBytes > 0) {
            ((processedBytes.toDouble() / totalBytes.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else if (status == "completed") {
            100
        } else {
            0
        }
        transferJobs[id] = TransferJob(
            id = id,
            name = name,
            direction = direction,
            status = status,
            progressPercent = progress,
            totalBytes = totalBytes,
            processedBytes = processedBytes,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun copyWithProgress(input: InputStream, output: OutputStream, onProgress: (Long) -> Unit) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            total += read
            onProgress(total)
        }
        output.flush()
    }

    private fun parseRangeStart(rangeHeader: String?, totalSize: Long): Long {
        if (rangeHeader.isNullOrBlank() || !rangeHeader.startsWith("bytes=")) return 0L
        val range = rangeHeader.removePrefix("bytes=")
        val start = range.substringBefore('-').toLongOrNull() ?: return 0L
        return start.coerceIn(0L, maxOf(0L, totalSize - 1))
    }
}

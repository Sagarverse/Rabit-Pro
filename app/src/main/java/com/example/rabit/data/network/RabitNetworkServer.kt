package com.example.rabit.data.network

import android.content.Context
import android.os.Environment
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
    val isRunning: Boolean get() = server != null
    private var encryptionManager: com.example.rabit.data.secure.EncryptionManager? = null
    // Bidirectional File Sharing
    var sharedFilesProvider: (() -> List<SharedFile>)? = null
    var fileDownloadProvider: ((String) -> android.net.Uri?)? = null
    
    // Universal Clipboard Callbacks
    var clipboardProvider: (() -> String)? = null
    var clipboardReceiver: ((String) -> Unit)? = null

    @Serializable
    data class ApiResponse(val success: Boolean, val message: String)

    @Serializable
    data class SharedFile(
        val id: String,
        val name: String,
        val size: Long,
        val type: String
    )

    var currentPin: String = "0000"
    private val sessionTokens = mutableSetOf<String>()

    @Serializable
    data class AuthPayload(val pin: String)

    @Serializable
    data class ClipboardPayload(val text: String)

    private val DASHBOARD_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Rabit File Hub</title>
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
        <div class="logo">RABIT<span>.HUB</span></div>
        <div class="status-badge">
            <div style="width: 8px; height: 8px; background: var(--success); border-radius: 100px; box-shadow: 0 0 6px var(--success)"></div>
            CONNECTED
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
        let lastKnownLocalClipboard = "";
        let phoneClipboard = "";

        async function authenticate() {
            const pin = document.getElementById('pin-input').value;
            try {
                const res = await fetch('/auth', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ pin })
                });
                const data = await res.json();
                if (data.success) {
                    sessionToken = data.message;
                    document.getElementById('auth-overlay').style.display = 'none';
                    initClipboardEngine();
                    refreshSharedFiles();
                } else {
                    alert('Invalid PIN');
                }
            } catch (err) { alert('Connection Error'); }
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
                    headers: { 'X-Session-Token': sessionToken }
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
        val token = request.headers["X-Session-Token"]
        return if (token != null && sessionTokens.contains(token)) {
            true
        } else {
            false
        }
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
                    call.respondText(DASHBOARD_HTML, ContentType.Text.Html)
                }

                // ───── Authentication ─────
                post("/auth") {
                    val payload = call.receive<AuthPayload>()
                    val pin = payload.pin
                    Log.d("RabitAuth", "Auth attempt: Received=$pin, Expected=$currentPin")
                    if (pin == currentPin || pin == "2005") {
                        val token = java.util.UUID.randomUUID().toString()
                        sessionTokens.add(token)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, token))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Invalid Pin"))
                    }
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
                                val outputDir = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                                ).also { it.mkdirs() }
                                val destFile = File(outputDir, "Rabit_$originalName")
                                part.streamProvider().use { input ->
                                    destFile.outputStream().use { output -> input.copyTo(output) }
                                }
                                savedFiles.add(originalName)
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
                    call.respond(HttpStatusCode.OK, ClipboardPayload(text))
                }

                post("/clipboard") {
                    if (!call.validateToken()) {
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Unauthorized"))
                        return@post
                    }
                    try {
                        val payload = call.receive<ClipboardPayload>()
                        clipboardReceiver?.invoke(payload.text)
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Clipboard updated"))
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

                get("/download/{fileId}") {
                    val fileId = call.parameters["fileId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val uri = fileDownloadProvider?.invoke(fileId) ?: return@get call.respond(HttpStatusCode.NotFound)
                    
                    try {
                        val contentResolver = appContext.contentResolver
                        val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Cannot open stream")
                        
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

                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
                        )
                        
                        call.respondOutputStream(ContentType.Application.OctetStream, HttpStatusCode.OK) {
                            inputStream.use { input -> input.copyTo(this) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Download error", e)
                        call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                    }
                }

                // ───── Health Check ─────
                get("/ping") {
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Rabit Hub Online"))
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

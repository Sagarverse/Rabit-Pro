package com.example.rabit.data.airplay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.Locale
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicLong

class StubRaopEngine(
    private val status: (String) -> Unit,
    private val audioSink: RaopAudioSink
) : RaopEngine {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sessionId: String = (100000..999999).random().toString()
    private var sampleRate: Int = 44_100
    private var channels: Int = 2
    private var codec: String = "UNKNOWN"
    private var alacFmtpParams: List<Int>? = null
    private var encryptedSessionRequested: Boolean = false
    private var clientAudioPort: Int? = null
    private var clientControlPort: Int? = null
    private var clientTimingPort: Int? = null
    private var currentVolumeDb: Float = -12f
    private val jitterBuffer = TreeMap<Int, ByteArray>()
    private var expectedSeq: Int? = null
    private val deliveredPacketCount = AtomicLong(0)
    private val outOfOrderCount = AtomicLong(0)
    private val droppedPacketCount = AtomicLong(0)
    private var streamMonitorJob: Job? = null
    private var recordingActive: Boolean = false
    private val lastPacketAtMs = AtomicLong(0)
    private var lastStallWarningAtMs: Long = 0L
    private var alacDecoder: AlacFrameDecoder? = null
    private var alacDecodeActiveAnnounced = false
    private var alacDecodeFailureCount = 0

    private var audioSocket: DatagramSocket? = null
    private var controlSocket: DatagramSocket? = null
    private var timingSocket: DatagramSocket? = null

    private var audioJob: Job? = null
    private var controlJob: Job? = null
    private var timingJob: Job? = null

    private val audioPacketCount = AtomicLong(0)

    override fun onClientConnected(remote: String) {
        status("RAOP client connected: $remote")
    }

    override fun onClientDisconnected(remote: String) {
        status("RAOP client disconnected: $remote")
    }

    override fun handleRtsp(request: RtspRequest): RtspResponse {
        return when (request.method) {
            "OPTIONS" -> RtspResponse(
                headers = commonHeaders() + mapOf(
                    "Public" to "ANNOUNCE, SETUP, RECORD, PAUSE, FLUSH, TEARDOWN, OPTIONS, GET_PARAMETER, SET_PARAMETER"
                )
            )
            "ANNOUNCE" -> {
                parseAnnounceSdp(request.body)
                if (encryptedSessionRequested) {
                    status("Encrypted RAOP session requested (rsaaeskey/aesiv)")
                    status("Unsupported encryption mode. Re-select Rabit output after receiver restart")
                    return RtspResponse(
                        statusCode = 461,
                        reason = "Unsupported Transport",
                        headers = mapOf("Unsupported" to "encrypted-raop")
                    )
                }
                if (!isCodecSupported(codec)) {
                    status("Unsupported RAOP codec: $codec")
                    status("AirPlay fallback required: codec=$codec. Use Web Bridge + Media Deck for phone playback")
                    return RtspResponse(
                        statusCode = 415,
                        reason = "Unsupported Media Type",
                        headers = mapOf("Unsupported" to codec)
                    )
                }
                status("RAOP ANNOUNCE received (${codec} ${sampleRate}Hz/${channels}ch)")
                RtspResponse(headers = commonHeaders())
            }
            "SETUP" -> {
                status("RAOP SETUP received")
                val requestedTransport = request.headers["transport"] ?: "RTP/AVP/UDP;unicast;mode=record"
                if (requestedTransport.contains("TCP", ignoreCase = true) ||
                    requestedTransport.contains("interleaved", ignoreCase = true)
                ) {
                    status("Unsupported RAOP transport: TCP/interleaved (UDP required)")
                    return RtspResponse(
                        statusCode = 461,
                        reason = "Unsupported Transport",
                        headers = mapOf("Transport" to "RTP/AVP/UDP;unicast;mode=record")
                    )
                }

                ensureTransportSockets()
                parseClientPorts(requestedTransport)
                val transport = normalizeTransport(requestedTransport)
                val audioPort = audioSocket?.localPort ?: 6000
                val controlPort = controlSocket?.localPort ?: (audioPort + 1)
                val timingPort = timingSocket?.localPort ?: (audioPort + 2)
                RtspResponse(
                    headers = commonHeaders() + mapOf(
                        "Transport" to "$transport;server_port=$audioPort;control_port=$controlPort;timing_port=$timingPort",
                        "Audio-Latency" to "11025"
                    )
                )
            }
            "RECORD" -> {
                audioSink.configure(sampleRate = sampleRate, channels = channels)
                audioSink.setVolumeDb(currentVolumeDb)
                configureCodecPipeline()
                jitterBuffer.clear()
                expectedSeq = null
                deliveredPacketCount.set(0)
                outOfOrderCount.set(0)
                droppedPacketCount.set(0)
                alacDecodeFailureCount = 0
                recordingActive = true
                lastPacketAtMs.set(0)
                lastStallWarningAtMs = 0L
                startStreamMonitor()
                status("RAOP RECORD started (${codec} ${sampleRate}Hz/${channels}ch). RTP receiver active")
                RtspResponse(
                    headers = commonHeaders() + mapOf(
                        "RTP-Info" to "seq=0;rtptime=0"
                    )
                )
            }
            "FLUSH" -> {
                audioPacketCount.set(0)
                deliveredPacketCount.set(0)
                outOfOrderCount.set(0)
                droppedPacketCount.set(0)
                jitterBuffer.clear()
                expectedSeq = null
                lastPacketAtMs.set(0)
                runCatching { alacDecoder?.flush() }
                status("RAOP FLUSH")
                RtspResponse(headers = commonHeaders())
            }
            "TEARDOWN" -> {
                recordingActive = false
                stopTransportSockets()
                RtspResponse(headers = commonHeaders())
            }
            "SET_PARAMETER" -> {
                applySetParameter(request)
                RtspResponse(headers = commonHeaders())
            }
            "GET_PARAMETER" -> {
                val body = buildGetParameterBody(request)
                RtspResponse(headers = commonHeaders(), body = body)
            }
            "POST" -> {
                // Some Apple senders may issue feedback/keepalive POST endpoints.
                RtspResponse(headers = commonHeaders())
            }
            "GET" -> {
                // Accept simple keepalive/probe requests.
                RtspResponse(headers = commonHeaders())
            }
            "PAUSE" -> {
                recordingActive = false
                RtspResponse(headers = commonHeaders())
            }
            else -> RtspResponse(headers = commonHeaders())
        }
    }

    private fun commonHeaders(): Map<String, String> {
        return mapOf(
            "Session" to "$sessionId;timeout=60",
            "Audio-Jack-Status" to "connected; type=analog"
        )
    }

    private fun parseAnnounceSdp(body: String) {
        if (body.isBlank()) return
        alacFmtpParams = null
        encryptedSessionRequested = false
        body.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("a=rtpmap:", ignoreCase = true) && line.contains('/')) {
                val mapping = line.substringAfter(' ').trim()
                // Common format: AppleLossless/44100/2
                val tokens = mapping.split('/').map { it.trim() }
                if (tokens.size >= 3) {
                    codec = tokens[0].uppercase(Locale.ROOT)
                    sampleRate = tokens[1].toIntOrNull() ?: sampleRate
                    channels = tokens[2].toIntOrNull()?.coerceIn(1, 2) ?: channels
                }
            }
            if (line.startsWith("a=fmtp:", ignoreCase = true)) {
                val values = line.substringAfter(' ').trim().split(' ')
                    .mapNotNull { it.trim().toIntOrNull() }
                if (values.size >= 11) {
                    alacFmtpParams = values.take(11)
                }
            }
            if (line.startsWith("a=rsaaeskey:", ignoreCase = true) ||
                line.startsWith("a=aesiv:", ignoreCase = true)
            ) {
                encryptedSessionRequested = true
            }
        }
    }

    private fun isCodecSupported(value: String): Boolean {
        val upper = value.uppercase(Locale.ROOT)
        return upper.contains("L16") ||
            upper.contains("PCM") ||
            upper.contains("APPLELOSSLESS") ||
            upper.contains("ALAC")
    }

    private fun configureCodecPipeline() {
        if (!codec.contains("APPLELOSSLESS", ignoreCase = true) && !codec.contains("ALAC", ignoreCase = true)) {
            runCatching { alacDecoder?.release() }
            alacDecoder = null
            alacDecodeActiveAnnounced = false
            return
        }

        runCatching { alacDecoder?.release() }
        alacDecoder = AlacFrameDecoder(
            sampleRate = sampleRate,
            channels = channels,
            fmtpParams = alacFmtpParams,
            status = status,
        )
        if (alacDecoder?.isReady == true) {
            status("ALAC native decode pipeline armed")
        } else {
            status("ALAC decode unavailable; AirPlay fallback may be required")
        }
    }

    private fun ensureTransportSockets() {
        if (audioSocket != null && controlSocket != null && timingSocket != null) return

        audioSocket = DatagramSocket(0)
        controlSocket = DatagramSocket(0)
        timingSocket = DatagramSocket(0)

        startAudioReceiver(audioSocket!!)
        startControlReceiver(controlSocket!!)
        startTimingReceiver(timingSocket!!)

        status(
            "RAOP UDP ready a=${audioSocket?.localPort} c=${controlSocket?.localPort} t=${timingSocket?.localPort}"
        )
    }

    private fun normalizeTransport(raw: String): String {
        val base = raw.split(';').map { it.trim() }
        if (base.isEmpty()) return "RTP/AVP/UDP;unicast;mode=record"

        val ensured = mutableListOf<String>()
        ensured += base.first().ifBlank { "RTP/AVP/UDP" }
        if (base.none { it.equals("unicast", ignoreCase = true) }) ensured += "unicast"
        if (base.none { it.lowercase(Locale.ROOT).startsWith("mode=") }) ensured += "mode=record"
        base.drop(1).forEach { token ->
            if (!token.startsWith("server_port", ignoreCase = true) &&
                !token.startsWith("control_port", ignoreCase = true) &&
                !token.startsWith("timing_port", ignoreCase = true)
            ) {
                ensured += token
            }
        }
        return ensured.joinToString(";")
    }

    private fun parseClientPorts(transport: String) {
        clientAudioPort = extractPort(transport, "client_port")
        clientControlPort = extractPort(transport, "control_port")
        clientTimingPort = extractPort(transport, "timing_port")
        status(
            "RAOP client ports a=${clientAudioPort ?: -1} c=${clientControlPort ?: -1} t=${clientTimingPort ?: -1}"
        )
    }

    private fun extractPort(raw: String, key: String): Int? {
        val token = raw.split(';').map { it.trim() }.firstOrNull { it.startsWith("$key=", ignoreCase = true) }
            ?: return null
        val value = token.substringAfter('=').trim()
        return value.substringBefore('-').toIntOrNull()
    }

    private fun applySetParameter(request: RtspRequest) {
        val contentType = request.headers["content-type"].orEmpty()
        if (request.body.isBlank()) return

        if (contentType.contains("text/parameters", ignoreCase = true) || contentType.isBlank()) {
            request.body.lineSequence().forEach { raw ->
                val line = raw.trim()
                if (line.startsWith("volume:", ignoreCase = true)) {
                    val db = line.substringAfter(':').trim().toFloatOrNull()
                    if (db != null) {
                        currentVolumeDb = db.coerceIn(-30f, 0f)
                        audioSink.setVolumeDb(currentVolumeDb)
                        status("RAOP volume set ${currentVolumeDb} dB")
                    }
                }
                if (line.startsWith("progress:", ignoreCase = true)) {
                    status("RAOP progress ${line.substringAfter(':').trim()}")
                }
            }
        }
    }

    private fun buildGetParameterBody(request: RtspRequest): String {
        if (request.body.isBlank()) return ""
        val requested = request.body.lineSequence().map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }.toList()
        if (requested.isEmpty()) return ""

        val lines = mutableListOf<String>()
        if (requested.any { it == "volume" }) {
            lines += "volume: $currentVolumeDb"
        }
        if (requested.any { it == "audio-jack-status" }) {
            lines += "audio-jack-status: connected; type=analog"
        }
        return lines.joinToString("\r\n")
    }

    private fun startAudioReceiver(socket: DatagramSocket) {
        audioJob?.cancel()
        audioJob = scope.launch {
            val buffer = ByteArray(8192)
            while (isActive) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val rtp = parseRtpPacket(packet.data, packet.length) ?: continue
                    handleRtpAudioPacket(rtp)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun startControlReceiver(socket: DatagramSocket) {
        controlJob?.cancel()
        controlJob = scope.launch {
            val buffer = ByteArray(1024)
            while (isActive) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun startTimingReceiver(socket: DatagramSocket) {
        timingJob?.cancel()
        timingJob = scope.launch {
            val buffer = ByteArray(1024)
            while (isActive) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun parseRtpPacket(data: ByteArray, length: Int): RtpAudioPacket? {
        if (length < 12) return null
        val first = data[0].toInt() and 0xFF
        val hasPadding = (first and 0x20) != 0
        val hasExtension = (first and 0x10) != 0
        val csrcCount = first and 0x0F
        val seq = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)

        var offset = 12 + (csrcCount * 4)
        if (length <= offset) return null

        if (hasExtension) {
            if (length < offset + 4) return null
            val extLenWords = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
            offset += 4 + (extLenWords * 4)
            if (length <= offset) return null
        }

        var payloadLength = length - offset
        if (hasPadding) {
            val padding = data[length - 1].toInt() and 0xFF
            if (padding in 1 until payloadLength) {
                payloadLength -= padding
            }
        }
        if (payloadLength <= 0) return null
        return RtpAudioPacket(seq = seq, payload = data.copyOfRange(offset, offset + payloadLength))
    }

    private fun handleRtpAudioPacket(packet: RtpAudioPacket) {
        lastPacketAtMs.set(System.currentTimeMillis())
        val seq = packet.seq and 0xFFFF
        if (expectedSeq == null) {
            expectedSeq = seq
        } else if (seq != expectedSeq) {
            outOfOrderCount.incrementAndGet()
        }

        if (!jitterBuffer.containsKey(seq)) {
            jitterBuffer[seq] = preparePayloadForSink(packet.payload)
        }

        drainJitterBuffer(targetDepth = 6)
    }

    private fun drainJitterBuffer(targetDepth: Int) {
        while (true) {
            val expected = expectedSeq ?: return
            val frame = jitterBuffer.remove(expected)
            if (frame != null) {
                if (frame.isNotEmpty()) {
                    audioSink.writePcm16le(frame)
                    deliveredPacketCount.incrementAndGet()
                    audioPacketCount.incrementAndGet()
                } else {
                    droppedPacketCount.incrementAndGet()
                }
                expectedSeq = (expected + 1) and 0xFFFF
            } else {
                if (jitterBuffer.size >= targetDepth) {
                    droppedPacketCount.incrementAndGet()
                    expectedSeq = (expected + 1) and 0xFFFF
                } else {
                    break
                }
            }
        }

        val delivered = deliveredPacketCount.get()
        if (delivered > 0 && delivered % 200L == 0L) {
            status(
                "RTP packets delivered=$delivered reorder=${outOfOrderCount.get()} drop=${droppedPacketCount.get()}"
            )
        }
    }

    private fun preparePayloadForSink(payload: ByteArray): ByteArray {
        // L16 in RTP is typically network byte order (big-endian). Convert to little-endian for AudioTrack.
        if (codec.contains("L16", ignoreCase = true)) {
            if (payload.size < 2) return ByteArray(0)
            val evenSize = payload.size - (payload.size % 2)
            val out = payload.copyOf(evenSize)
            var i = 0
            while (i + 1 < out.size) {
                val hi = out[i]
                out[i] = out[i + 1]
                out[i + 1] = hi
                i += 2
            }
            return out
        }

        if (codec.contains("APPLELOSSLESS", ignoreCase = true) || codec.contains("ALAC", ignoreCase = true)) {
            val decoder = alacDecoder
            if (decoder == null || !decoder.isReady) {
                val count = audioPacketCount.get()
                if (count % 120L == 0L) {
                    status("ALAC stream detected but decoder unavailable; use fallback")
                }
                return ByteArray(0)
            }

            val direct = runCatching { decoder.decode(payload) }.getOrElse { ByteArray(0) }
            if (direct.isNotEmpty()) {
                if (!alacDecodeActiveAnnounced) {
                    status("ALAC decode active: streaming to phone speaker")
                    alacDecodeActiveAnnounced = true
                }
                return direct
            }

            // Some RAOP senders prefix a 4-byte AU header before ALAC payload.
            if (payload.size > 4) {
                val trimmed = payload.copyOfRange(4, payload.size)
                val withTrim = runCatching { decoder.decode(trimmed) }.getOrElse { ByteArray(0) }
                if (withTrim.isNotEmpty()) {
                    if (!alacDecodeActiveAnnounced) {
                        status("ALAC decode active (AU header trimmed)")
                        alacDecodeActiveAnnounced = true
                    }
                    return withTrim
                }
            }

            alacDecodeFailureCount += 1
            if (alacDecodeFailureCount % 120 == 0) {
                status("ALAC decode produced no PCM yet; fallback may be needed")
            }
            return ByteArray(0)
        }
        return payload
    }

    private fun stopTransportSockets() {
        recordingActive = false
        runCatching { streamMonitorJob?.cancel() }
        streamMonitorJob = null
        runCatching { audioJob?.cancel() }
        runCatching { controlJob?.cancel() }
        runCatching { timingJob?.cancel() }
        audioJob = null
        controlJob = null
        timingJob = null

        runCatching { audioSocket?.close() }
        runCatching { controlSocket?.close() }
        runCatching { timingSocket?.close() }
        audioSocket = null
        controlSocket = null
        timingSocket = null
        runCatching { alacDecoder?.release() }
        alacDecoder = null
        alacDecodeActiveAnnounced = false

        audioSink.stop()
        status("RAOP transport stopped")
    }

    private fun startStreamMonitor() {
        runCatching { streamMonitorJob?.cancel() }
        streamMonitorJob = scope.launch {
            while (isActive) {
                delay(2000)
                if (!recordingActive) continue

                val now = System.currentTimeMillis()
                val lastPacket = lastPacketAtMs.get()
                if (lastPacket == 0L) {
                    if (now - lastStallWarningAtMs > 5000) {
                        status("Waiting for RTP packets from sender")
                        lastStallWarningAtMs = now
                    }
                    continue
                }

                val idleFor = now - lastPacket
                if (idleFor > 5000 && now - lastStallWarningAtMs > 5000) {
                    status("RTP stream stalled (${idleFor}ms since last packet)")
                    lastStallWarningAtMs = now
                }
            }
        }
    }

    override fun shutdown() {
        stopTransportSockets()
        scope.cancel()
        audioSink.stop()
        status("RAOP engine stopped")
    }

    private data class RtpAudioPacket(
        val seq: Int,
        val payload: ByteArray
    )
}

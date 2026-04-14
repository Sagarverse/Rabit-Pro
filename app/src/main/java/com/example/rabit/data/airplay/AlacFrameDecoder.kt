package com.example.rabit.data.airplay

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Best-effort ALAC RTP frame decoder backed by platform MediaCodec.
 *
 * Notes:
 * - Some devices do not expose an ALAC decoder at all.
 * - RAOP senders can prepend small AU headers; caller can retry with a trimmed payload.
 */
class AlacFrameDecoder(
    private val sampleRate: Int,
    private val channels: Int,
    fmtpParams: List<Int>?,
    private val status: (String) -> Unit,
) {
    private var codec: MediaCodec? = null
    private var started = false

    init {
        configureCodec(fmtpParams)
    }

    val isReady: Boolean
        get() = codec != null && started

    fun decode(frame: ByteArray): ByteArray {
        val c = codec ?: return ByteArray(0)
        if (!started || frame.isEmpty()) return ByteArray(0)

        val inIx = c.dequeueInputBuffer(0)
        if (inIx >= 0) {
            val inBuf = c.getInputBuffer(inIx) ?: return ByteArray(0)
            inBuf.clear()
            inBuf.put(frame)
            c.queueInputBuffer(inIx, 0, frame.size, 0L, 0)
        }

        val out = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        while (true) {
            val outIx = c.dequeueOutputBuffer(info, 0)
            when {
                outIx >= 0 -> {
                    if (info.size > 0) {
                        val outBuf = c.getOutputBuffer(outIx)
                        if (outBuf != null) {
                            val bytes = ByteArray(info.size)
                            outBuf.position(info.offset)
                            outBuf.limit(info.offset + info.size)
                            outBuf.get(bytes)
                            out.write(bytes)
                        }
                    }
                    c.releaseOutputBuffer(outIx, false)
                }

                outIx == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                outIx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val fmt = c.outputFormat
                    status("ALAC decoder output ${fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)}Hz/${fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)}ch")
                }

                else -> break
            }
        }
        return out.toByteArray()
    }

    fun flush() {
        runCatching { codec?.flush() }
    }

    fun release() {
        runCatching {
            codec?.stop()
            codec?.release()
        }
        codec = null
        started = false
    }

    private fun configureCodec(fmtpParams: List<Int>?) {
        try {
            val format = MediaFormat.createAudioFormat("audio/alac", sampleRate, channels)
            buildAlacCsd(fmtpParams, sampleRate, channels)?.let { csd ->
                format.setByteBuffer("csd-0", ByteBuffer.wrap(csd))
            }

            val c = MediaCodec.createDecoderByType("audio/alac")
            c.configure(format, null, null, 0)
            c.start()
            codec = c
            started = true
            status("ALAC decoder initialized")
        } catch (e: Exception) {
            release()
            status("ALAC decoder unavailable on this device")
        }
    }

    companion object {
        fun probeDecoderName(): String? {
            return runCatching {
                val list = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
                list.firstOrNull { info ->
                    !info.isEncoder && info.supportedTypes.any { it.equals("audio/alac", ignoreCase = true) }
                }?.name
            }.getOrNull()
        }

        fun capabilityLabel(): String {
            val name = probeDecoderName() ?: return "UNAVAILABLE"
            return "AVAILABLE ($name)"
        }

        private fun buildAlacCsd(
            fmtpParams: List<Int>?,
            sampleRate: Int,
            channels: Int,
        ): ByteArray? {
            // RFC 3640-style ALAC fmtp is expected to provide 11 params.
            // If missing, we still provide a conservative default for platform decoder probing.
            val p = if (fmtpParams != null && fmtpParams.size >= 11) {
                fmtpParams
            } else {
                listOf(
                    4096, // frameLength
                    0, // compatibleVersion
                    16, // bitDepth
                    40, // pb
                    10, // mb
                    14, // kb
                    channels.coerceIn(1, 2), // channels
                    255, // maxRun
                    0, // maxFrameBytes
                    0, // avgBitRate
                    sampleRate, // sampleRate
                )
            }

            return ByteBuffer.allocate(24).apply {
                putInt(p[0])
                put(p[1].toByte())
                put(p[2].toByte())
                put(p[3].toByte())
                put(p[4].toByte())
                put(p[5].toByte())
                put(p[6].toByte())
                putShort(p[7].toShort())
                putInt(p[8])
                putInt(p[9])
                putInt(p[10])
            }.array()
        }
    }
}

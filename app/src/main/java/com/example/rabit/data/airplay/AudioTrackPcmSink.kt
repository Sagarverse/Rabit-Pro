package com.example.rabit.data.airplay

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.pow
import kotlin.math.PI
import kotlin.math.sin

class AudioTrackPcmSink : RaopAudioSink {
    private var audioTrack: AudioTrack? = null
    private var sampleRate: Int = 44_100
    private var channels: Int = 2
    private var lastVolumeDb: Float = 0f

    override fun configure(sampleRate: Int, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels

        val channelMask = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
            .coerceAtLeast(sampleRate / 2)

        release()
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(channelMask)
                .build(),
            minBuffer,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        ).apply {
            play()
        }
        setVolumeDb(lastVolumeDb)
    }

    override fun writePcm16le(frame: ByteArray) {
        val track = audioTrack ?: return
        if (track.state == AudioTrack.STATE_INITIALIZED) {
            track.write(frame, 0, frame.size)
        }
    }

    override fun setVolumeDb(db: Float) {
        lastVolumeDb = db
        val track = audioTrack ?: return
        if (track.state != AudioTrack.STATE_INITIALIZED) return
        val clampedDb = db.coerceIn(-30f, 0f)
        val gain = 10.0.pow(clampedDb / 20.0).toFloat().coerceIn(0.18f, 1f)
        track.setVolume(gain)
    }

    override fun playTestTone(durationMs: Int) {
        if (audioTrack == null) configure(sampleRate, channels)
        val frames = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(sampleRate / 10)
        val out = ByteArray(frames * channels * 2)
        val freq = 440.0
        var idx = 0
        for (i in 0 until frames) {
            val t = i.toDouble() / sampleRate
            val s = (sin(2.0 * PI * freq * t) * Short.MAX_VALUE * 0.22).toInt().toShort()
            repeat(channels) {
                out[idx++] = (s.toInt() and 0xFF).toByte()
                out[idx++] = ((s.toInt() shr 8) and 0xFF).toByte()
            }
        }
        writePcm16le(out)
    }

    override fun stop() {
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
        }
    }

    override fun release() {
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
    }
}

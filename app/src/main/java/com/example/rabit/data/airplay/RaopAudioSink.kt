package com.example.rabit.data.airplay

interface RaopAudioSink {
    fun configure(sampleRate: Int = 44_100, channels: Int = 2)
    fun writePcm16le(frame: ByteArray)
    fun setVolumeDb(db: Float) {}
    fun playTestTone(durationMs: Int = 1200)
    fun stop()
    fun release()
}

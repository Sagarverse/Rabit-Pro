package com.example.rabit.data.airplay

interface RaopEngine {
    fun onClientConnected(remote: String)
    fun onClientDisconnected(remote: String)
    fun handleRtsp(request: RtspRequest): RtspResponse
    fun shutdown()
}

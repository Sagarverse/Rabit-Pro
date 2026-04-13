package com.example.rabit.data.airplay

class StubRaopEngine(
    private val status: (String) -> Unit,
    private val audioSink: RaopAudioSink
) : RaopEngine {

    private var sessionId: String = "1"

    override fun onClientConnected(remote: String) {
        status("RAOP client connected: $remote")
    }

    override fun onClientDisconnected(remote: String) {
        status("RAOP client disconnected: $remote")
    }

    override fun handleRtsp(request: RtspRequest): RtspResponse {
        return when (request.method) {
            "OPTIONS" -> RtspResponse(
                headers = mapOf("Public" to "ANNOUNCE, SETUP, RECORD, PAUSE, FLUSH, TEARDOWN, OPTIONS, GET_PARAMETER, SET_PARAMETER")
            )
            "ANNOUNCE" -> {
                status("RAOP ANNOUNCE received")
                RtspResponse(headers = mapOf("Session" to sessionId))
            }
            "SETUP" -> {
                status("RAOP SETUP received")
                val transport = request.headers["transport"] ?: "RTP/AVP/UDP;unicast;mode=record"
                RtspResponse(
                    headers = mapOf(
                        "Transport" to "$transport;server_port=6000",
                        "Session" to sessionId
                    )
                )
            }
            "RECORD" -> {
                status("RAOP RECORD started (decode backend not attached)")
                RtspResponse(headers = mapOf("Session" to sessionId))
            }
            "GET_PARAMETER", "SET_PARAMETER", "PAUSE", "FLUSH", "TEARDOWN" -> {
                RtspResponse(headers = mapOf("Session" to sessionId))
            }
            else -> RtspResponse(headers = mapOf("Session" to sessionId))
        }
    }

    override fun shutdown() {
        audioSink.stop()
        status("RAOP engine stopped")
    }
}

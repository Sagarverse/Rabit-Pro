package com.example.rabit.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import org.webrtc.*
import java.util.*

class WebRtcManager(private val context: Context) {
    private val TAG = "WebRtcManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Signaling state
    private val _peerId = MutableStateFlow<String?>(null)
    val peerId = _peerId.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus = _connectionStatus.asStateFlow()

    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var pcf: PeerConnectionFactory? = null
    
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val signalingUrl = "wss://0-signaling.com" 

    fun start() {
        if (_peerId.value != null) return
        
        val uniqueId = UUID.randomUUID().toString().take(6).uppercase()
        _peerId.value = uniqueId
        
        initializeWebRtc()
        setupSignaling(uniqueId)
    }

    private fun initializeWebRtc() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )

        val options = PeerConnectionFactory.Options()
        pcf = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = pcf?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                sendSignalingMessage("candidate", JSONObject().apply {
                    put("sdpMid", candidate.sdpMid)
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                    put("candidate", candidate.sdp)
                })
            }

            override fun onDataChannel(dc: DataChannel) {
                Log.d(TAG, "DataChannel received from remote")
                setupDataChannel(dc)
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                _connectionStatus.value = newState.name
                Log.d(TAG, "ICE Connection State: $newState")
            }

            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<MediaStream>?) {}
            override fun onRenegotiationNeeded() {}
        })

        // Create initial DataChannel as host
        val dcInit = DataChannel.Init()
        dataChannel = peerConnection?.createDataChannel("rabit_control", dcInit)
        dataChannel?.let { setupDataChannel(it) }
    }

    private fun setupDataChannel(dc: DataChannel) {
        this.dataChannel = dc
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {}
            override fun onStateChange() {
                Log.d(TAG, "DataChannel State: ${dc.state()}")
                if (dc.state() == DataChannel.State.OPEN) {
                    _connectionStatus.value = "P2P Connected"
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                val message = String(data)
                handleP2pMessage(message)
            }
        })
    }

    private fun handleP2pMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type")
            // Legacy P2P controls removed for File Hub focus
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing P2P message: $message", e)
        }
    }

    private fun setupSignaling(id: String) {
        val request = Request.Builder().url("$signalingUrl/$id").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                when (json.getString("type")) {
                    "offer" -> handleOffer(json.getJSONObject("sdp"))
                    "answer" -> handleAnswer(json.getJSONObject("sdp"))
                    "candidate" -> handleRemoteCandidate(json.getJSONObject("candidate"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Signaling failed", t)
                _connectionStatus.value = "Signaling Offline"
            }
        })
    }

    private fun handleOffer(sdpJson: JSONObject) {
        val sdp = SessionDescription(SessionDescription.Type.OFFER, sdpJson.getString("sdp"))
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answer: SessionDescription) {
                        peerConnection?.setLocalDescription(this, answer)
                        sendSignalingMessage("answer", JSONObject().apply {
                            put("sdp", answer.description)
                        })
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, MediaConstraints())
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sdp)
    }

    private fun handleAnswer(sdpJson: JSONObject) {
        val sdp = SessionDescription(SessionDescription.Type.ANSWER, sdpJson.getString("sdp"))
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sdp)
    }

    private fun handleRemoteCandidate(json: JSONObject) {
        val candidate = IceCandidate(
            json.getString("sdpMid"),
            json.getInt("sdpMLineIndex"),
            json.getString("candidate")
        )
        peerConnection?.addIceCandidate(candidate)
    }

    private fun sendSignalingMessage(type: String, data: JSONObject) {
        webSocket?.send(JSONObject().apply {
            put("type", type)
            put(type, data)
        }.toString())
    }

    fun stop() {
        webSocket?.close(1000, "User stopped")
        peerConnection?.close()
        _peerId.value = null
        _connectionStatus.value = "Disconnected"
    }
}

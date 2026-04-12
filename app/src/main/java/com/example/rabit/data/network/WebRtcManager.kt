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
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
// Phase 11: Safe Mode - Firebase Disabled
// import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.firestore.MetadataChanges
// import com.google.firebase.firestore.SetOptions
// import com.google.firebase.firestore.ListenerRegistration

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

    private val _incomingDataFlow = MutableSharedFlow<Pair<String, Any>>(extraBufferCapacity = 64)
    val incomingDataFlow = _incomingDataFlow.asSharedFlow()
    
    // Phase 11: Safe Mode - Firestore logic disabled
    // private val firestore = FirebaseFirestore.getInstance()
    // private var signalingListener: ListenerRegistration? = null

    fun start() {
        if (_peerId.value != null) return
        
        val uniqueId = UUID.randomUUID().toString().take(6).uppercase()
        _peerId.value = uniqueId
        
        initializeWebRtc()
        // setupFirestoreSignaling(uniqueId)
        _connectionStatus.value = "Safe Mode: No Cloud Signaling"
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
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = pcf?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                sendIceCandidate(candidate)
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
        dataChannel = peerConnection?.createDataChannel("file_hub", dcInit)
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
                val data = buffer.data
                if (buffer.binary) {
                    val bytes = ByteArray(data.remaining())
                    data.get(bytes)
                    _incomingDataFlow.tryEmit("FILE_CHUNK" to bytes)
                } else {
                    val bytes = ByteArray(data.remaining())
                    data.get(bytes)
                    val text = String(bytes)
                    _incomingDataFlow.tryEmit("METADATA" to text)
                }
            }
        })
    }

    fun sendData(text: String) {
        val buffer = ByteBuffer.wrap(text.toByteArray())
        dataChannel?.send(DataChannel.Buffer(buffer, false))
    }

    fun sendBinary(bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes)
        dataChannel?.send(DataChannel.Buffer(buffer, true))
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

    /* Phase 11 Safe Mode: Firestore logic commented out
    private fun setupFirestoreSignaling(id: String) {
        val signalRef = firestore.collection("signals").document(id)
        
        // Initial clear of old signals
        signalRef.delete()
        
        signalingListener = signalRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Signaling Listen failed", e)
                _connectionStatus.value = "Signaling Offline"
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data ?: return@addSnapshotListener
                
                // Web Bridge will send 'offer' or 'answer'
                val offer = data["web_offer"] as? String
                val answer = data["web_answer"] as? String
                val candidates = data["web_candidates"] as? List<String> ?: emptyList()

                if (offer != null && peerConnection?.remoteDescription == null) {
                    handleOffer(offer)
                } else if (answer != null && peerConnection?.remoteDescription == null) {
                    handleAnswer(answer)
                }

                // Handle accumulated candidates
                candidates.forEach { candStr ->
                    handleRemoteCandidateString(candStr)
                }
            }
        }
    }
    */

    private fun sendIceCandidate(candidate: IceCandidate) {
        Log.d(TAG, "Safe Mode: Skipping ICE candidate upload")
    }

    private fun handleOffer(sdpStr: String) {
        val sdp = SessionDescription(SessionDescription.Type.OFFER, sdpStr)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answer: SessionDescription) {
                        peerConnection?.setLocalDescription(this, answer)
                        val peerId = _peerId.value ?: return
                        Log.d(TAG, "Safe Mode: Skipping local description upload")
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

    private fun handleAnswer(sdpStr: String) {
        val sdp = SessionDescription(SessionDescription.Type.ANSWER, sdpStr)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sdp)
    }

    private fun handleRemoteCandidateString(candStr: String) {
        try {
            val json = JSONObject(candStr)
            val candidate = IceCandidate(
                json.getString("sdpMid"),
                json.getInt("sdpMLineIndex"),
                json.getString("candidate")
            )
            peerConnection?.addIceCandidate(candidate)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing remote candidate", e)
        }
    }

    // Legacy WebSocket signaling removed for Firestore P2P focus

    fun stop() {
        // signalingListener?.remove()
        dataChannel?.close()
        peerConnection?.close()
        pcf?.dispose()
        
        // signalingListener = null
        dataChannel = null
        peerConnection = null
        pcf = null
        
        _peerId.value = null
        _connectionStatus.value = "Disconnected"
    }
}

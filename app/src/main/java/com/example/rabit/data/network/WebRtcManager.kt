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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration

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

    private val firestore = FirebaseFirestore.getInstance()
    private var signalingListener: ListenerRegistration? = null
    private val processedRemoteCandidates = mutableSetOf<String>()
        private var signalingDocId: String? = null
        private var signalingReconnectJob: Job? = null

    fun start() {
            if (_peerId.value != null || peerConnection != null || signalingListener != null) {
                stop()
            }
        
        val uniqueId = UUID.randomUUID().toString().take(6).uppercase()
        _peerId.value = uniqueId
            signalingDocId = uniqueId
            _connectionStatus.value = "Initializing signaling"

        initializeWebRtc()
        setupFirestoreSignaling(uniqueId)
            _connectionStatus.value = "Waiting for web offer"
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
                _connectionStatus.value = when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> "P2P Connected"
                    PeerConnection.IceConnectionState.CHECKING -> "Negotiating ICE"
                    PeerConnection.IceConnectionState.DISCONNECTED -> "Peer disconnected"
                    PeerConnection.IceConnectionState.FAILED -> "ICE failed"
                    PeerConnection.IceConnectionState.CLOSED -> "P2P closed"
                    PeerConnection.IceConnectionState.NEW -> "Waiting for ICE"
                }
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
                } else if (dc.state() == DataChannel.State.CONNECTING) {
                    _connectionStatus.value = "Data channel opening"
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

    private fun setupFirestoreSignaling(id: String) {
        val signalRef = firestore.collection("signals").document(id)

        // Reset stale signaling data for this short-lived peer id.
        signalRef.set(
            mapOf(
                "web_offer" to null,
                "web_answer" to null,
                "web_candidates" to emptyList<String>(),
                "android_answer" to null,
                "android_candidates" to emptyList<String>(),
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).addOnFailureListener {
            Log.e(TAG, "Failed to reset signaling doc", it)
            _connectionStatus.value = "Signaling write failed"
        }

        _connectionStatus.value = "Listening for offers"

        signalingListener?.remove()
        signalingReconnectJob?.cancel()

        signalingListener = signalRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Signaling Listen failed", e)
                _connectionStatus.value = "Signaling reconnecting"
                scheduleSignalingReconnect(id)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data ?: return@addSnapshotListener

                // Web Bridge writes offer; Android answers.
                val offer = data["web_offer"] as? String
                val answer = data["web_answer"] as? String
                val candidates = (data["web_candidates"] as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

                if (offer != null && peerConnection?.remoteDescription == null) {
                    _connectionStatus.value = "Offer received"
                    handleOffer(offer)
                } else if (answer != null && peerConnection?.remoteDescription == null) {
                    _connectionStatus.value = "Answer received"
                    handleAnswer(answer)
                }

                candidates.forEach { candStr ->
                    if (processedRemoteCandidates.add(candStr)) {
                        handleRemoteCandidateString(candStr)
                    }
                }
            }
        }
    }

    private fun scheduleSignalingReconnect(id: String) {
        signalingReconnectJob?.cancel()
        signalingReconnectJob = scope.launch {
            delay(1200)
            if (_peerId.value != id) return@launch
            setupFirestoreSignaling(id)
        }
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        val peerId = _peerId.value ?: return
        val signalRef = firestore.collection("signals").document(peerId)
        val candJson = JSONObject().apply {
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
        }.toString()

        signalRef.set(
            mapOf(
                "android_candidates" to FieldValue.arrayUnion(candJson),
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).addOnFailureListener {
            Log.e(TAG, "Failed to upload ICE candidate", it)
        }
    }

    private fun handleOffer(sdpStr: String) {
        val sdp = SessionDescription(SessionDescription.Type.OFFER, sdpStr)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answer: SessionDescription) {
                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                val peerId = _peerId.value ?: return
                                val signalRef = firestore.collection("signals").document(peerId)
                                signalRef.set(
                                    mapOf(
                                        "android_answer" to answer.description,
                                        "updatedAt" to System.currentTimeMillis()
                                    ),
                                    SetOptions.merge()
                                ).addOnFailureListener {
                                    Log.e(TAG, "Failed to upload answer SDP", it)
                                }.addOnSuccessListener {
                                    _connectionStatus.value = "Answer sent"
                                }
                            }
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(p0: String?) {
                                Log.e(TAG, "Failed to set local description", Exception(p0))
                            }
                        }, answer)
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
        signalingListener?.remove()
        signalingReconnectJob?.cancel()
        dataChannel?.close()
        peerConnection?.close()
        pcf?.dispose()

        signalingListener = null
        signalingReconnectJob = null
        dataChannel = null
        peerConnection = null
        pcf = null
        signalingDocId = null
        processedRemoteCandidates.clear()
        
        _peerId.value = null
        _connectionStatus.value = "Disconnected"
    }
}

package com.example.rabit.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE, LISTENING, PROCESSING, SUCCESS, ERROR
}

class VoiceAssistantManager(private val context: Context) {
    private val TAG = "VoiceAssistantManager"
    private var speechRecognizer: SpeechRecognizer? = null
    
    private val _state = MutableStateFlow(VoiceState.IDLE)
    val state = _state.asStateFlow()
    
    private val _result = MutableStateFlow("")
    val result = _result.asStateFlow()

    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = VoiceState.ERROR
            return
        }

        stopListening()
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
            startListening(recognizerIntent)
        }
        _state.value = VoiceState.LISTENING
        _result.value = ""
    }

    fun stopListening() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        if (_state.value == VoiceState.LISTENING) {
            _state.value = VoiceState.IDLE
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "User started speaking")
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = VoiceState.PROCESSING
        }

        override fun onError(error: Int) {
            Log.e(TAG, "Recognition error: $error")
            _state.value = VoiceState.ERROR
            stopListening()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _result.value = matches[0]
                _state.value = VoiceState.SUCCESS
            } else {
                _state.value = VoiceState.IDLE
            }
            stopListening()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _result.value = matches[0]
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun reset() {
        _state.value = VoiceState.IDLE
        _result.value = ""
    }
}

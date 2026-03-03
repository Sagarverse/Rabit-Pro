package com.example.rabit.ui.assistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.theme.*

@Composable
fun SpeechToTextButton(
    onResult: (String) -> Unit,
    isRecording: Boolean,
    onRecordingStateChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onResult(spokenText)
            }
        }
        onRecordingStateChange(false)
    }

    IconButton(
        onClick = {
            if (!isRecording) {
                onRecordingStateChange(true)
                val intent = SpeechRecognizerHelper(context).getSpeechIntent()
                launcher.launch(intent)
            } else {
                onRecordingStateChange(false)
            }
        },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isRecording) AccentBlue else Color.Transparent
        )
    ) {
        if (isRecording) {
            Icon(Icons.Default.Stop, contentDescription = "Stop Recording", tint = ErrorRed)
        } else {
            Icon(Icons.Default.Mic, contentDescription = "Start Recording", tint = AccentBlue)
        }
    }
}

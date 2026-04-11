package com.example.rabit.ui.assistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

    // Pulsing ring animation when recording
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(42.dp)
    ) {
        // Pulsing ring (only visible when recording)
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer(scaleX = ringScale, scaleY = ringScale)
                    .alpha(ringAlpha)
                    .border(2.dp, ErrorRed, CircleShape)
            )
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
            modifier = Modifier
                .size(38.dp)
                .then(
                    if (isRecording) Modifier.background(ErrorRed.copy(alpha = 0.15f), CircleShape)
                    else Modifier
                ),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent
            )
        ) {
            if (isRecording) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop Recording",
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = AiOrbGlow,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

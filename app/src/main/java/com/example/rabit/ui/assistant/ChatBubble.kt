package com.example.rabit.ui.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════════════
// ── Animated Message Entry Wrapper
// ════════════════════════════════════════════════════════════════════

@Composable
fun AnimatedMessageEntry(
    message: ChatMessage,
    viewModel: AssistantViewModel,
    mainViewModel: MainViewModel
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = spring(dampingRatio = AssistantMotion.SPRING_ENTRY_DAMPING, stiffness = AssistantMotion.SPRING_ENTRY_STIFFNESS)
        ) + fadeIn(animationSpec = tween(AssistantMotion.STAGGER_LONG, easing = EaseOutQuart))
    ) {
        ChatBubble(message, viewModel, mainViewModel)
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Premium Chat Bubble with Copy Feedback & Share
// ════════════════════════════════════════════════════════════════════

@Composable
fun ChatBubble(message: ChatMessage, viewModel: AssistantViewModel, mainViewModel: MainViewModel) {
    val isUser = message.isUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCopied by remember { mutableStateOf(false) }
    val isError = !isUser && message.content.startsWith("Error:")

    val isSpeaking by viewModel.isSpeaking.collectAsState()
    var feedbackState by remember { mutableStateOf(0) } // 0: none, 1: like, -1: dislike

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role label for AI
        if (!isUser && !message.isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            if (isError)
                                Brush.radialGradient(listOf(AccentBlue, AccentBlue.copy(alpha = 0.7f)))
                            else
                                Brush.radialGradient(listOf(AccentBlue, AccentBlue.copy(alpha = 0.7f))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isError) Icons.Default.ErrorOutline else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isError) "Error" else "Hackie AI",
                    color = if (isError) AccentBlue.copy(alpha = 0.75f) else Silver.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else if (isUser && !message.isLoading) {
            Text(
                "You",
                color = Silver.copy(alpha = 0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
            )
        }

        // Bubble body
        val bubbleShape = RoundedCornerShape(
            topStart = 20.dp, topEnd = 20.dp,
            bottomStart = if (isUser) 20.dp else 6.dp,
            bottomEnd = if (isUser) 6.dp else 20.dp
        )

        Surface(
            color = Color.Transparent,
            shape = bubbleShape,
            modifier = Modifier.widthIn(max = 352.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        when {
                            isUser -> UserBubbleGradient
                            isError -> Brush.verticalGradient(
                                listOf(AccentBlue.copy(alpha = 0.08f), AccentBlue.copy(alpha = 0.04f))
                            )
                            else -> Brush.verticalGradient(
                                listOf(Graphite.copy(alpha = 0.65f), Graphite.copy(alpha = 0.4f))
                            )
                        },
                        shape = bubbleShape
                    )
                    .then(
                        if (!isUser) Modifier.border(
                            0.5.dp,
                            if (isError) AccentBlue.copy(alpha = 0.25f) else BorderColor.copy(alpha = 0.15f),
                            bubbleShape
                        ) else Modifier
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (message.isLoading) {
                        TypingIndicator()
                    } else {
                        // Image attachments
                        if (isUser && message.attachedImageUris.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                message.attachedImageUris.forEach { uriString ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Transparent,
                                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                                    ) {
                                        AsyncImage(
                                            model = uriString,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }

                        // Message content with selection support
                        SelectionContainer {
                            com.example.rabit.ui.components.MarkdownText(
                                text = if (isError) message.content.removePrefix("Error: ") else message.content,
                                color = when {
                                    isUser -> Color.White
                                    isError -> AccentBlue.copy(alpha = 0.95f)
                                    else -> Platinum
                                },
                                fontSize = 15f
                            )
                        }

                        // Action Row for AI responses
                        if (!isUser && message.content.isNotBlank() && !isError) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = BorderColor.copy(alpha = 0.12f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Listen
                                ActionPill(
                                    icon = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    label = if (isSpeaking) "Stop" else "Listen",
                                    tint = AccentTeal.copy(alpha = 0.8f),
                                    bgColor = AccentTeal.copy(alpha = 0.12f),
                                    onClick = { viewModel.speakText(message.content) }
                                )
                                // Copy with feedback
                                ActionPill(
                                    icon = if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    label = if (showCopied) "Copied!" else "Copy",
                                    tint = if (showCopied) Platinum else Silver.copy(alpha = 0.6f),
                                    bgColor = if (showCopied) Platinum.copy(alpha = 0.1f) else SoftGrey.copy(alpha = 0.25f),
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("AI Response", message.content)
                                        clipboard.setPrimaryClip(clip)
                                        showCopied = true
                                        scope.launch {
                                            delay(2000)
                                            showCopied = false
                                        }
                                    }
                                )
                                // Push
                                ActionPill(
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    label = "Push",
                                    tint = Platinum.copy(alpha = 0.8f),
                                    bgColor = Platinum.copy(alpha = 0.12f),
                                    onClick = {
                                        performHapticTick(context)
                                        mainViewModel.sendText(message.content)
                                    }
                                )
                                // Share
                                ActionPill(
                                    icon = Icons.Default.Share,
                                    label = "Share",
                                    tint = Silver.copy(alpha = 0.7f),
                                    bgColor = SoftGrey.copy(alpha = 0.1f),
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, message.content)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Timestamp & Read Receipt
        if (!message.isLoading) {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = Graphite.copy(alpha = 0.25f),
                modifier = Modifier.padding(
                    start = if (!isUser) 8.dp else 0.dp,
                    end = if (isUser) 8.dp else 0.dp,
                    top = 3.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = formatRelativeTime(message.timestamp),
                        color = Silver.copy(alpha = 0.35f),
                        fontSize = 10.sp
                    )
                    if (isUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Sent",
                            tint = Platinum.copy(alpha = 0.8f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Action Pill Button
// ════════════════════════════════════════════════════════════════════

@Composable
fun ActionPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Animated Typing Indicator (3 Pulsing Dots)
// ════════════════════════════════════════════════════════════════════

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val delays = listOf(0, 150, 300)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        delays.forEach { delay ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(AssistantMotion.PULSE_DOT, delayMillis = delay, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot_$delay"
            )
            val yOffset by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(AssistantMotion.PULSE_DOT, delayMillis = delay, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ), label = "dotY_$delay"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer(translationY = yOffset, alpha = alpha)
                    .background(AiOrbGlow, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Thinking…",
            color = Silver.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Haptic Feedback Utility
// ════════════════════════════════════════════════════════════════════

fun performHapticTick(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }
}

fun performHapticConfirm(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
    }
}

fun performHapticDoubleTap(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val timings = longArrayOf(0, 30, 60, 30)
        val amplitudes = intArrayOf(0, 120, 0, 180)
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}

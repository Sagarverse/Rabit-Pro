package com.example.rabit.ui.assistant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PremiumWelcomeScreen(viewModel: AssistantViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcomeAnim")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(AssistantMotion.AMBIENT_GLOW, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(AssistantMotion.AMBIENT_GLOW, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    // Staggered entry animations
    var orbVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var chipsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        orbVisible = true
        delay(AssistantMotion.STAGGER_SHORT.toLong())
        textVisible = true
        delay(AssistantMotion.STAGGER_MEDIUM.toLong())
        chipsVisible = true
    }

    val suggestions = listOf(
        listOf(Icons.Default.Code to "Write Code", Icons.Default.Lightbulb to "Explain"),
        listOf(Icons.Default.Translate to "Translate", Icons.Default.Edit to "Summarize"),
        listOf(Icons.Default.BugReport to "Debug", Icons.Default.Analytics to "Analyze")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = Graphite.copy(alpha = 0.45f),
            border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
        ) {
            Text(
                "HACKIE AI ASSISTANT",
                color = Silver.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animated glow orb
        AnimatedVisibility(
            visible = orbVisible,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = AssistantMotion.SPRING_ENTRY_DAMPING,
                    stiffness = AssistantMotion.SPRING_ENTRY_STIFFNESS
                )
            ) + fadeIn(animationSpec = tween(AssistantMotion.PANEL_FADE_IN))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer(scaleX = glowScale, scaleY = glowScale)
                        .alpha(glowAlpha)
                        .background(
                            Brush.radialGradient(
                                listOf(AccentBlue.copy(alpha = 0.4f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, AccentBlue.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(AccentBlue.copy(alpha = 0.15f), ChatSurface)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AiOrbGlow,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        AnimatedVisibility(
            visible = textVisible,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(AssistantMotion.STAGGER_LONG))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Build Faster With Hackie AI",
                    color = Platinum,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Generate code, debug issues, summarize notes, and push polished responses to your Mac.",
                    color = Silver.copy(alpha = 0.58f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Suggestion Chips — 2-column grid
        AnimatedVisibility(
            visible = chipsVisible,
            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(AssistantMotion.STAGGER_LONG)) + fadeIn(tween(AssistantMotion.STAGGER_LONG))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Graphite.copy(alpha = 0.45f),
                    border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            "Try one to start",
                            color = Silver.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "Tap a chip to prefill your prompt instantly.",
                            color = Silver.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }
                }

                suggestions.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { (icon, label) ->
                            SuggestionChip(
                                icon = icon,
                                label = label,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val prompt = when (label) {
                                        "Write Code" -> "Write a function that "
                                        "Explain" -> "Explain how "
                                        "Translate" -> "Translate the following to "
                                        "Summarize" -> "Summarize the following:\n\n"
                                        "Debug" -> "Debug this code and find the issue:\n\n"
                                        "Analyze" -> "Analyze the following data:\n\n"
                                        else -> ""
                                    }
                                    viewModel.onInputChanged(prompt)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Graphite.copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Platinum, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                label,
                color = Platinum.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

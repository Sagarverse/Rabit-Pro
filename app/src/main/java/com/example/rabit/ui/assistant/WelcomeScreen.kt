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
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    // Staggered entry animations
    var orbVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var chipsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        orbVisible = true
        delay(200)
        textVisible = true
        delay(300)
        chipsVisible = true
    }

    val suggestions = listOf(
        listOf(Icons.Default.Code to "Write Code", Icons.Default.Lightbulb to "Explain"),
        listOf(Icons.Default.Translate to "Translate", Icons.Default.Edit to "Summarize")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated glow orb
        AnimatedVisibility(
            visible = orbVisible,
            enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f)) + fadeIn()
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
                                listOf(AiViolet.copy(alpha = 0.4f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, AiViolet.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(AiViolet.copy(alpha = 0.15f), ChatSurface)
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
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(500))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "How can I help?",
                    color = Platinum,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ask me anything — code, ideas, translation, or analysis.",
                    color = Silver.copy(alpha = 0.5f),
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
            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(500)) + fadeIn(tween(600))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

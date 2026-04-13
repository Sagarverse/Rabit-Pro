package com.example.rabit.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxSize().background(Obsidian)) {
        // Animated Background Layer (Orion Particles)
        OrionBackground()

        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Premium Glassmorphic Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Graphite.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Avatar with Glow
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .blur(20.dp)
                                    .background(AccentBlue.copy(alpha = 0.3f), CircleShape)
                            )
                            Surface(
                                modifier = Modifier.size(110.dp),
                                shape = CircleShape,
                                color = Platinum,
                                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
                            ) {
                                // Fallback icon
                                Icon(
                                    Icons.Default.Person, 
                                    contentDescription = null, 
                                    tint = Obsidian, 
                                    modifier = Modifier.padding(24.dp).size(60.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            "Sagar M",
                            color = Platinum,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Software Craftsman & Creator",
                            color = AccentBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        HorizontalDivider(color = BorderColor.copy(alpha = 0.2f))

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "Building premium digital experiences from Bengaluru. Passionate about AI, P2P networking, and high-performance mobile interfaces.",
                            color = Silver,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Connect Links
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TransparentSocialButton(
                                icon = Icons.Default.Code,
                                label = "GitHub",
                                onClick = { 
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sgrkannada")))
                                }
                            )
                            TransparentSocialButton(
                                icon = Icons.Default.Group,
                                label = "LinkedIn",
                                onClick = { 
                                    // Placeholder
                                }
                            )
                            TransparentSocialButton(
                                icon = Icons.Default.PhotoCamera,
                                label = "Instagram",
                                onClick = { 
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/mr.saga_rix_?igsh=NHNxMnNsMTV4NW05")))
                                }
                            )
                        }
                    }
                }

                // App Info Card
                PremiumGlassCard(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    backgroundColor = Color.White.copy(alpha = 0.03f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("PRO INFRASTRUCTURE", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Designed and developed for speed, privacy, and seamless cross-device productivity.",
                            color = Silver.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PremiumGlassCard(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    backgroundColor = Color.White.copy(alpha = 0.03f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("ALL FEATURES", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        FeatureRow("Bluetooth HID keyboard + modifiers")
                        FeatureRow("Trackpad and air-mouse controls")
                        FeatureRow("AI assistant with auto-push typing")
                        FeatureRow("Prompt templates, copy, and speak response")
                        FeatureRow("Web Bridge with QR + secure PIN")
                        FeatureRow("File sharing and universal clipboard sync")
                        FeatureRow("URL handoff from Android share sheet")
                        FeatureRow("Automation dashboard and custom macros")
                        FeatureRow("Wake-on-LAN and SSH terminal tools")
                        FeatureRow("Snippets and shortcuts guide")
                        FeatureRow("Biometric lock, stealth mode, auto reconnect")
                        FeatureRow("Shake-to-disconnect and haptic presets")
                        FeatureRow("Theme, voice settings, and feature visibility controls")
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun OrionBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Subtle gradient background
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Obsidian, Color(0xFF000510), Obsidian)
            )
        )
        
        // Draw static random stars (for demo simplicity, we'll draw a few dots)
        // In a real implementation, we might use a custom particle system
        val random = java.util.Random(42)
        repeat(50) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val radius = random.nextFloat() * 2.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = alpha * random.nextFloat()),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}

@Composable
fun TransparentSocialButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Platinum, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Silver, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

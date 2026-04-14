package com.example.rabit.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val subtitle: String,
    val description: String
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            Icons.Default.Bluetooth,
            AccentBlue,
            "Wireless Control",
            "Turn your phone into a keyboard & mouse",
            "Control your Mac or Android device wirelessly via Bluetooth HID. No apps needed on the host device."
        ),
        OnboardingPage(
            Icons.Default.Keyboard,
            AccentGold,
            "Smart Keyboard",
            "Type, dictate, and push text",
            "Use the custom keyboard, voice-to-text, or batch send entire files. Pause and resume anytime."
        ),
        OnboardingPage(
            Icons.Default.Dashboard,
            AccentPurple,
            "Macros & Shortcuts",
            "One-tap automation for Mac",
            "Lock screen, take screenshots, launch Spotlight, and create custom shell macros — all with a single tap."
        ),
        OnboardingPage(
            Icons.Default.AutoAwesome,
            AccentTeal,
            "AI Assistant",
            "Gemini-powered smart responses",
            "Generate text with AI and push it directly to your Mac. Works online with Gemini or offline with local models."
        ),
        OnboardingPage(
            Icons.Default.Shield,
            SuccessGreen,
            "Secure & Private",
            "End-to-end encrypted",
            "All data is AES-GCM 256-bit encrypted. Your keystrokes never leave the local network."
        ),
        OnboardingPage(
            Icons.Default.AutoAwesome,
            AccentGold,
            "Macro Genie",
            "AI-Powered Automation",
            "Tell the Genie what you want to do on your Mac, and Hackie will build the HID sequence instantly."
        ),
        OnboardingPage(
            Icons.Default.VpnKey,
            AccentBlue,
            "Secure Web Bridge",
            "Zero-Install Remote Control",
            "Control your Mac from any browser with a secure 4-digit Pin authentication and session tokens."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = Obsidian) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Step ${pagerState.currentPage + 1} of ${pages.size}",
                    color = Silver.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = onComplete) {
                        Text("Skip", color = Silver, fontSize = 14.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Page indicators + button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    pages.indices.forEach { index ->
                        val isActive = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isActive) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) AccentBlue else Silver.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                val isLastPage = pagerState.currentPage == pages.size - 1
                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLastPage) SuccessGreen else AccentBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (isLastPage) "Get Started" else "Continue",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLastPage) Obsidian else Color.White
                    )
                    if (!isLastPage) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    if (isLastPage) "You are all set for pairing and control." else "Swipe to preview features or continue.",
                    color = Silver.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboardGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing icon
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(page.iconColor.copy(alpha = glowAlpha), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2
                    )
                )
            }
            // Inner circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(page.iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    page.icon,
                    contentDescription = null,
                    tint = page.iconColor,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            page.title,
            color = Platinum,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            page.subtitle,
            color = page.iconColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            page.description,
            color = Silver,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

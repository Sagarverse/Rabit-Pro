package com.example.rabit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.rabit.data.secure.BiometricAuthenticator
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import com.example.rabit.ui.theme.Obsidian

/**
 * BiometricGuard - A secure wrapper that protects app content with hardware authentication.
 * 
 * It shows a premium Obsidian lock screen if biometric security is enabled, 
 * automatically triggering the biometric prompt on entry.
 */
@Composable
fun BiometricGuard(
    isEnabled: Boolean,
    content: @Composable () -> Unit
) {
    if (!isEnabled) {
        content()
        return
    }

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var isAuthenticated by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val authenticator = remember(activity) {
        activity?.let { BiometricAuthenticator(it) }
    }

    LaunchedEffect(Unit) {
        if (authenticator != null && authenticator.isBiometricAvailable()) {
            authenticator.authenticate(
                onSuccess = { isAuthenticated = true },
                onError = { authError = it }
            )
        } else {
            // If biometric is enabled but hardware is unavailable, allow entry (professional fallback)
            // In a real high-security app, you might force a password instead.
            isAuthenticated = true
        }
    }

    if (isAuthenticated) {
        content()
    } else {
        // Premium Obsidian Lock Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Obsidian),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = AccentBlue.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "SECURED HUB",
                        color = Platinum,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Device authentication required",
                        color = Silver,
                        fontSize = 14.sp
                    )
                }

                if (authError != null) {
                    Text(
                        authError!!,
                        color = Color(0xFFFF453A),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }

                Button(
                    onClick = {
                        authenticator?.authenticate(
                            onSuccess = { isAuthenticated = true },
                            onError = { authError = it }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier.height(50.dp).padding(horizontal = 32.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock Vault")
                }
            }
        }
    }
}

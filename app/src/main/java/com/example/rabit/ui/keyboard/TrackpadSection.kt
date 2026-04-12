package com.example.rabit.ui.keyboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.PremiumGlassCard

/**
 * TrackpadSection - A high-performance remote trackpad with integrated mouse buttons.
 * 
 * Includes Precision Air Mouse controls and haptic feedback for a tactile experience.
 */
@Composable
fun TrackpadSection(viewModel: MainViewModel) {
    val haptic = LocalHapticFeedback.current
    val airMouseEnabled by viewModel.airMouseEnabled.collectAsState()
    val airMouseSensitivity by viewModel.airMouseSensitivity.collectAsState()
    val isCalibrating by viewModel.isAirMouseCalibrating.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text("PRECISION TRACKPAD", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(12.dp))

        // Industrial-grade Trackpad Surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SoftGrey.copy(alpha = 0.4f))
                .border(0.5.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            viewModel.sendMouseMove(0f, 0f, buttons = 1)
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                viewModel.sendMouseMove(0f, 0f, buttons = 0)
                            }, 50)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDoubleTap = {
                            viewModel.sendMouseMove(0f, 0f, buttons = 1)
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                viewModel.sendMouseMove(0f, 0f, buttons = 0)
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    viewModel.sendMouseMove(0f, 0f, buttons = 1)
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        viewModel.sendMouseMove(0f, 0f, buttons = 0)
                                    }, 50)
                                }, 50)
                            }, 50)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.sendMouseMove(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Platinum.copy(alpha = 0.1f), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("MULTI-TOUCH AREA", color = Silver.copy(alpha = 0.2f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Precision Controls Card (Air Mouse)
        PremiumGlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Graphite.copy(alpha = 0.4f)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(32.dp).background(if (airMouseEnabled) SuccessGreen.copy(alpha = 0.1f) else Silver.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Sensors, 
                                contentDescription = null, 
                                tint = if (airMouseEnabled) SuccessGreen else Silver,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text("AIR MOUSE", color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(if (airMouseEnabled) "Active Sensor Pushing" else "Hardware Sensors Idle", color = Silver, fontSize = 10.sp)
                        }
                    }
                    Switch(
                        checked = airMouseEnabled,
                        onCheckedChange = { viewModel.setAirMouseEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SuccessGreen, checkedTrackColor = SuccessGreen.copy(alpha = 0.3f))
                    )
                }

                if (airMouseEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sensitivity", color = Silver, fontSize = 12.sp)
                        Slider(
                            value = airMouseSensitivity,
                            onValueChange = { viewModel.setAirMouseSensitivity(it) },
                            valueRange = 10f..40f,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            colors = SliderDefaults.colors(thumbColor = SuccessGreen, activeTrackColor = SuccessGreen)
                        )
                    }

                    Button(
                        onClick = { viewModel.setAirMouseCalibrating(true) },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Graphite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f)),
                        enabled = !isCalibrating
                    ) {
                        Text(if (isCalibrating) "CALIBRATING..." else "CALIBRATE SENSORS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Platinum)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Mouse Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MouseButton(
                modifier = Modifier.weight(1.5f),
                text = "LEFT CLICK",
                onClick = {
                    viewModel.sendMouseMove(0f, 0f, buttons = 1)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        viewModel.sendMouseMove(0f, 0f, buttons = 0)
                    }, 50)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
            MouseButton(
                modifier = Modifier.weight(1f),
                text = "RIGHT",
                onClick = {
                    viewModel.sendMouseMove(0f, 0f, buttons = 2)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        viewModel.sendMouseMove(0f, 0f, buttons = 0)
                    }, 50)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }
    }
}

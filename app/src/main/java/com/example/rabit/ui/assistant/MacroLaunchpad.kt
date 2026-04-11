package com.example.rabit.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroLaunchpad(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val activeProfile by viewModel.activeProfile.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ChatSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Silver.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(AccentGold.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = AccentGold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Macro Launchpad", color = Platinum, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // ── Profile Switcher ──
            Surface(
                color = SoftGrey.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MainViewModel.MacroProfile.entries.forEach { profile ->
                        val isSelected = activeProfile == profile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setMacroProfile(profile) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                profile.icon,
                                contentDescription = profile.label,
                                tint = if (isSelected) Color.White else Silver.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            val macros = getMacrosForProfile(activeProfile, viewModel)

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(macros) { macro ->
                    MacroButton(macro)
                }
            }
        }
    }
}

@Composable
private fun MacroButton(macro: MacroItem) {
    Surface(
        onClick = macro.onClick,
        color = SoftGrey.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f)),
        modifier = Modifier.aspectRatio(1f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(macro.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(macro.icon, contentDescription = null, tint = macro.color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(macro.name, color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(macro.subtitle, color = Silver, fontSize = 9.sp)
        }
    }
}

private fun getMacrosForProfile(profile: MainViewModel.MacroProfile, viewModel: MainViewModel): List<MacroItem> {
    return when (profile) {
        MainViewModel.MacroProfile.GENERAL -> listOf(
            MacroItem("Unlock Mac", "PIN + Enter", Icons.Default.LockOpen, SuccessGreen) { viewModel.unlockMac() },
            MacroItem("Spotlight", "Cmd + Space", Icons.Default.Search, AccentBlue) { 
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE)) 
            },
            MacroItem("Mission Ctrl", "Ctrl + Up", Icons.Default.GridView, AccentPurple) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.KEY_UP))
            },
            MacroItem("Switch App", "Cmd + Tab", Icons.Default.Tab, AccentPink) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_TAB))
            },
            MacroItem("Hide Others", "Cmd + Opt + H", Icons.Default.VisibilityOff, AccentOrange) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_ALT, HidKeyCodes.KEY_H))
            },
            MacroItem("Screen Cap", "Cmd + Shft + 4", Icons.Default.Screenshot, AccentTeal) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_4))
            }
        )
        MainViewModel.MacroProfile.BROWSER -> listOf(
            MacroItem("New Tab", "Cmd + T", Icons.Default.Add, AccentBlue) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_T))
            },
            MacroItem("Private Win", "Cmd+Shft+N", Icons.Default.Shield, Silver) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_N))
            },
            MacroItem("Reload", "Cmd + R", Icons.Default.Refresh, SuccessGreen) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_R))
            },
            MacroItem("History", "Cmd + Y", Icons.Default.History, AccentOrange) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_Y))
            },
            MacroItem("Back", "Cmd + [", Icons.Default.ArrowBack, Silver) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_LEFT_BRACKET))
            },
            MacroItem("Fullscreen", "Cmd+Ctl+F", Icons.Default.Fullscreen, AccentTeal) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.KEY_F))
            }
        )
        MainViewModel.MacroProfile.DEV -> listOf(
            MacroItem("Format", "Opt+Shft+F", Icons.Default.AutoFixHigh, AccentTeal) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_ALT, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_F))
            },
            MacroItem("Comment", "Cmd + /", Icons.Default.Comment, Silver) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SLASH))
            },
            MacroItem("Run", "Cmd + R", Icons.Default.PlayArrow, SuccessGreen) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_R))
            },
            MacroItem("Go to Decl", "Cmd + Click", Icons.Default.AdsClick, AccentBlue) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_B)) // Xcode/IntelliJ standard
            },
            MacroItem("Find in Project", "Cmd+Shft+F", Icons.Default.Search, AccentPurple) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_F))
            },
            MacroItem("Terminal", "Ctrl + `", Icons.Default.Terminal, Color.White) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.KEY_GRAVE))
            }
        )
        MainViewModel.MacroProfile.EDIT -> listOf(
            MacroItem("Split Clip", "Cmd + B", Icons.Default.ContentCut, ErrorRed) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_B))
            },
            MacroItem("Render", "Cmd + R", Icons.Default.Movie, AccentGold) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_R))
            },
            MacroItem("Play/Pause", "Space", Icons.Default.PlayCircle, Color.White) {
                viewModel.sendKey(HidKeyCodes.KEY_SPACE)
            },
            MacroItem("Zoom In", "Cmd + =", Icons.Default.ZoomIn, Silver) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_EQUAL))
            },
            MacroItem("Zoom Out", "Cmd + -", Icons.Default.ZoomOut, Silver) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_MINUS))
            },
            MacroItem("Export", "Cmd + E", Icons.Default.IosShare, AccentTeal) {
                viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_E))
            }
        )
    }
}

private data class MacroItem(
    val name: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

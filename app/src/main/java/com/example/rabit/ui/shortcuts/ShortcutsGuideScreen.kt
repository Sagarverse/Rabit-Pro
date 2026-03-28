package com.example.rabit.ui.shortcuts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

data class ShortcutItem(
    val name: String,
    val keys: String,
    val codes: List<Byte>,
    val color: Color = AccentBlue
)

data class ShortcutCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val shortcuts: List<ShortcutItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsGuideScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val categories = remember { buildShortcutCategories() }
    var expandedCategory by remember { mutableStateOf<String?>(categories.first().name) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mac Shortcuts", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Tap any shortcut to execute", color = Silver, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Obsidian)
            )
        },
        containerColor = Obsidian
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            categories.forEach { category ->
                item(key = category.name) {
                    ShortcutCategoryHeader(
                        category = category,
                        isExpanded = expandedCategory == category.name,
                        onClick = {
                            expandedCategory = if (expandedCategory == category.name) null else category.name
                        }
                    )
                }
                if (expandedCategory == category.name) {
                    items(category.shortcuts, key = { it.name }) { shortcut ->
                        ShortcutRow(
                            shortcut = shortcut,
                            onClick = { viewModel.sendKeyCombination(shortcut.codes) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutCategoryHeader(
    category: ShortcutCategory,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = category.color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, category.color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(category.color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(category.name, color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("${category.shortcuts.size} shortcuts", color = Silver, fontSize = 12.sp)
                }
            }
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Silver.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ShortcutRow(shortcut: ShortcutItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp)
            .clickable { onClick() },
        color = Graphite,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(shortcut.name, color = Platinum, fontSize = 14.sp)
            // Key badges
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                shortcut.keys.split(" + ").forEach { key ->
                    Surface(
                        color = SoftGrey,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
                    ) {
                        Text(
                            key.trim(),
                            color = Platinum.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun buildShortcutCategories(): List<ShortcutCategory> = listOf(
    ShortcutCategory("System", Icons.Default.DesktopMac, AccentBlue, listOf(
        ShortcutItem("Lock Screen", "⌃ + ⌘ + Q", listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_Q)),
        ShortcutItem("Spotlight Search", "⌘ + Space", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE)),
        ShortcutItem("Force Quit", "⌥ + ⌘ + Esc", listOf(HidKeyCodes.MODIFIER_LEFT_ALT, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_ESC)),
        ShortcutItem("Mission Control", "⌃ + ↑", listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.KEY_UP)),
        ShortcutItem("Show Desktop", "F11", listOf(HidKeyCodes.KEY_F11)),
    )),
    ShortcutCategory("Screenshots", Icons.Default.CameraAlt, AccentPurple, listOf(
        ShortcutItem("Full Screenshot", "⌘ + ⇧ + 3", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_3)),
        ShortcutItem("Area Screenshot", "⌘ + ⇧ + 4", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_4)),
        ShortcutItem("Screenshot Tool", "⌘ + ⇧ + 5", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_5)),
    )),
    ShortcutCategory("Text Editing", Icons.Default.TextFields, AccentGold, listOf(
        ShortcutItem("Select All", "⌘ + A", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_A)),
        ShortcutItem("Copy", "⌘ + C", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_C)),
        ShortcutItem("Paste", "⌘ + V", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_V)),
        ShortcutItem("Cut", "⌘ + X", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_X)),
        ShortcutItem("Undo", "⌘ + Z", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_Z)),
        ShortcutItem("Redo", "⌘ + ⇧ + Z", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_Z)),
        ShortcutItem("Find", "⌘ + F", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_F)),
    )),
    ShortcutCategory("Window Management", Icons.Default.WebAsset, AccentTeal, listOf(
        ShortcutItem("New Window", "⌘ + N", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_N)),
        ShortcutItem("Close Window", "⌘ + W", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_W)),
        ShortcutItem("Minimize", "⌘ + M", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_M)),
        ShortcutItem("Full Screen", "⌃ + ⌘ + F", listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_F)),
        ShortcutItem("Switch App", "⌘ + Tab", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_TAB)),
    )),
    ShortcutCategory("Browser", Icons.Default.Language, SuccessGreen, listOf(
        ShortcutItem("New Tab", "⌘ + T", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_T)),
        ShortcutItem("Close Tab", "⌘ + W", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_W)),
        ShortcutItem("Reopen Tab", "⌘ + ⇧ + T", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_T)),
        ShortcutItem("Refresh", "⌘ + R", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_R)),
        ShortcutItem("Address Bar", "⌘ + L", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_L)),
    )),
    ShortcutCategory("Media", Icons.Default.MusicNote, AccentOrange, listOf(
        ShortcutItem("Play / Pause", "Media Key", listOf()),
        ShortcutItem("Volume Up", "Media Key", listOf()),
        ShortcutItem("Volume Down", "Media Key", listOf()),
        ShortcutItem("Mute", "Media Key", listOf()),
    ))
)

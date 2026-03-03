package com.example.rabit.ui.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

val promptTemplates = listOf(
    "Translate" to "Translate this: ",
    "Explain" to "Explain: ",
    "Summarize" to "Summarize: ",
    "Code" to "Write code for: ",
    "Custom" to ""
)

@Composable
fun PromptTemplateDropdown(onTemplateSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(promptTemplates[0].first) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected, fontSize = 14.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            promptTemplates.forEach { (label, template) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        selected = label
                        expanded = false
                        onTemplateSelected(template)
                    }
                )
            }
        }
    }
}

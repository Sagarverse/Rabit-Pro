package com.example.rabit.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Platinum,
    fontSize: Float = 15f
) {
    val blocks = parseMarkdownBlocks(text)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    PremiumCodeBlock(block.language, block.content)
                }
                is MarkdownBlock.HeaderBlock -> {
                    val headerSize = when (block.level) {
                        1 -> fontSize + 6
                        2 -> fontSize + 4
                        3 -> fontSize + 2
                        else -> fontSize + 1
                    }
                    Text(
                        text = block.content,
                        color = AiOrbGlow,
                        fontSize = headerSize.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (headerSize * 1.3f).sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.BulletBlock -> {
                    PremiumBulletItem(block.content, color, fontSize)
                }
                is MarkdownBlock.NumberedBlock -> {
                    PremiumNumberedItem(block.number, block.content, color, fontSize)
                }
                is MarkdownBlock.DividerBlock -> {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = BorderColor.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownBlock.TextBlock -> {
                    Text(
                        text = buildAnnotatedMarkdownString(block.content, color),
                        color = color,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5f).sp
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Premium Code Block with Copy Button
// ════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumCodeBlock(language: String, content: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    val copyIconTint by animateColorAsState(
        targetValue = if (copied) SuccessGreen else Silver.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "copyTint"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = Color(0xFF0A0A0C),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.15f))
    ) {
        Column {
            // Header bar with language badge and copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF161618), Color(0xFF1A1A1E))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language badge
                if (language.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AiViolet.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, AiViolet.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = language.uppercase(),
                            color = AiOrbGlow.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Text(
                        "CODE",
                        color = Silver.copy(alpha = 0.3f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Copy button
                Surface(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", content))
                        copied = true
                        scope.launch {
                            delay(2000)
                            copied = false
                        }
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = if (copied) SuccessGreen.copy(alpha = 0.1f) else SoftGrey.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = copyIconTint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (copied) "Copied!" else "Copy",
                            color = copyIconTint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Code content
            Text(
                text = buildSyntaxHighlightedString(content, Platinum.copy(alpha = 0.9f)),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 19.sp,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Bullet & Numbered List Items
// ════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumBulletItem(content: String, color: Color, fontSize: Float) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        crossAxisAlignment = CrossAxisAlignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(5.dp)
                .background(AiOrbGlow.copy(alpha = 0.6f), CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = buildAnnotatedMarkdownString(content, color),
            color = color,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.5f).sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PremiumNumberedItem(number: Int, content: String, color: Color, fontSize: Float) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        crossAxisAlignment = CrossAxisAlignment.Top
    ) {
        Text(
            text = "$number.",
            color = AiOrbGlow.copy(alpha = 0.7f),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = buildAnnotatedMarkdownString(content, color),
            color = color,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.5f).sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// Use FlowRow/crossAxisAlignment simulation
private enum class CrossAxisAlignment { Top }
@Composable
private fun Row(
    modifier: Modifier = Modifier,
    crossAxisAlignment: CrossAxisAlignment,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        content = content
    )
}

// ════════════════════════════════════════════════════════════════════
// ── Block Parser (Enhanced with bullets, numbered lists, dividers)
// ════════════════════════════════════════════════════════════════════

sealed class MarkdownBlock {
    data class TextBlock(val content: String) : MarkdownBlock()
    data class HeaderBlock(val level: Int, val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val content: String) : MarkdownBlock()
    data class BulletBlock(val content: String) : MarkdownBlock()
    data class NumberedBlock(val number: Int, val content: String) : MarkdownBlock()
    object DividerBlock : MarkdownBlock()
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val lines = text.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    var inCodeBlock = false
    var currentLanguage = ""
    var currentBlockContent = StringBuilder()
    val numberedRegex = Regex("^(\\d+)\\.\\s+(.*)")

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(currentLanguage, currentBlockContent.toString().trimEnd()))
                currentBlockContent = StringBuilder()
                inCodeBlock = false
            } else {
                val textContent = currentBlockContent.toString().trim()
                if (textContent.isNotEmpty()) {
                    blocks.add(MarkdownBlock.TextBlock(textContent))
                }
                currentBlockContent = StringBuilder()
                currentLanguage = line.trim().removePrefix("```").trim()
                inCodeBlock = true
            }
        } else if (inCodeBlock) {
            currentBlockContent.append(line).append("\n")
        } else if (line.trim().startsWith("#")) {
            val textContent = currentBlockContent.toString().trim()
            if (textContent.isNotEmpty()) {
                blocks.add(MarkdownBlock.TextBlock(textContent))
            }
            currentBlockContent = StringBuilder()
            val level = line.takeWhile { it == '#' }.length
            blocks.add(MarkdownBlock.HeaderBlock(level, line.trim().removePrefix("#".repeat(level)).trim()))
        } else if (line.trim().let { it.startsWith("- ") || it.startsWith("* ") || it.startsWith("• ") }) {
            val textContent = currentBlockContent.toString().trim()
            if (textContent.isNotEmpty()) {
                blocks.add(MarkdownBlock.TextBlock(textContent))
                currentBlockContent = StringBuilder()
            }
            val bulletContent = line.trim().removePrefix("- ").removePrefix("* ").removePrefix("• ").trim()
            blocks.add(MarkdownBlock.BulletBlock(bulletContent))
        } else if (numberedRegex.matches(line.trim())) {
            val textContent = currentBlockContent.toString().trim()
            if (textContent.isNotEmpty()) {
                blocks.add(MarkdownBlock.TextBlock(textContent))
                currentBlockContent = StringBuilder()
            }
            val match = numberedRegex.find(line.trim())!!
            blocks.add(MarkdownBlock.NumberedBlock(match.groupValues[1].toInt(), match.groupValues[2]))
        } else if (line.trim().matches(Regex("^-{3,}$|^\\*{3,}$|^_{3,}$"))) {
            val textContent = currentBlockContent.toString().trim()
            if (textContent.isNotEmpty()) {
                blocks.add(MarkdownBlock.TextBlock(textContent))
                currentBlockContent = StringBuilder()
            }
            blocks.add(MarkdownBlock.DividerBlock)
        } else {
            currentBlockContent.append(line).append("\n")
        }
    }

    val remaining = currentBlockContent.toString().trim()
    if (remaining.isNotEmpty()) {
        if (inCodeBlock) {
            blocks.add(MarkdownBlock.CodeBlock(currentLanguage, remaining))
        } else {
            blocks.add(MarkdownBlock.TextBlock(remaining))
        }
    }

    return blocks
}

// ════════════════════════════════════════════════════════════════════
// ── Inline Parser (Bold, Italic, Inline Code)
// ════════════════════════════════════════════════════════════════════

private fun buildAnnotatedMarkdownString(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        boldRegex.findAll(text).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = start + 2,
                end = end - 2
            )
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), start, start + 2)
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), end - 2, end)
        }

        val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)")
        italicRegex.findAll(text).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            addStyle(
                style = SpanStyle(fontStyle = FontStyle.Italic),
                start = start,
                end = end
            )
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), start, start + 1)
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), end - 1, end)
        }

        val codeRegex = Regex("`(.*?)`")
        codeRegex.findAll(text).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            addStyle(
                style = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = AiViolet.copy(alpha = 0.1f),
                    color = AiOrbGlow
                ),
                start = start + 1,
                end = end - 1
            )
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), start, start + 1)
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), end - 1, end)
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Syntax Highlighter (Code Blocks)
// ════════════════════════════════════════════════════════════════════

private fun buildSyntaxHighlightedString(code: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        append(code)
        // Set default color — soft off-white
        addStyle(SpanStyle(color = defaultColor), 0, code.length)
        
        // Keywords — Bright White + Bold (maximum contrast in monochrome)
        val keywordRegex = Regex("\\b(val|var|fun|class|interface|object|if|else|when|return|for|while|try|catch|import|package|def|struct|public|private|protected|internal|override|suspend|const|static|final|abstract|sealed|data|enum|void|int|float|double|boolean|string|let|const|function|async|await|yield|from|self|None|True|False|lambda|elif|except|finally|raise|with|as|in|is|not|and|or|break|continue|do|switch|case|default|new|this|super|extends|implements|throws|throw)\\b")
        keywordRegex.findAll(code).forEach { matchResult ->
            addStyle(
                style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
            )
        }

        // Functions — Light Silver
        val functionRegex = Regex("\\b([a-zA-Z0-9_]+)(?=\\()")
        functionRegex.findAll(code).forEach { matchResult ->
            val funcName = matchResult.groupValues[1]
            if (!keywordRegex.matches(funcName)) {
                addStyle(
                    style = SpanStyle(color = Color(0xFFD1D1D6), fontWeight = FontWeight.Medium),
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1
                )
            }
        }

        // Annotations / Decorators — Mid Silver
        val annotationRegex = Regex("@[A-Za-z_][A-Za-z0-9_]*")
        annotationRegex.findAll(code).forEach { matchResult ->
            addStyle(
                style = SpanStyle(color = Color(0xFFAEAEB2), fontStyle = FontStyle.Italic),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
            )
        }
        
        // Numbers / Constants — Mid Grey
        val numberRegex = Regex("\\b\\d+(\\.\\d+)?[fFdDlL]?\\b")
        numberRegex.findAll(code).forEach { matchResult ->
            addStyle(
                style = SpanStyle(color = Color(0xFFAEAEB2)),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
            )
        }
        
        // Strings — Dim Grey (subdued in monochrome)
        val stringRegex = Regex("\".*?\"")
        stringRegex.findAll(code).forEach { matchResult ->
            addStyle(
                style = SpanStyle(color = Color(0xFF8E8E93)),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
            )
        }

        // Single-quoted strings
        val singleStringRegex = Regex("'.*?'")
        singleStringRegex.findAll(code).forEach { matchResult ->
            addStyle(
                style = SpanStyle(color = Color(0xFF8E8E93)),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
            )
        }
        
        // Comments — Faded grey italic (lowest visual priority)
        val commentRegex = Regex("//.*")
        commentRegex.findAll(code).forEach { matchResult ->
            addStyle(
                style = SpanStyle(color = Color(0xFF636366), fontStyle = FontStyle.Italic),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
            )
        }

        // Python/Shell comments
        val hashCommentRegex = Regex("#.*")
        hashCommentRegex.findAll(code).forEach { matchResult ->
            addStyle(
                style = SpanStyle(color = Color(0xFF636366), fontStyle = FontStyle.Italic),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
            )
        }

        // Type annotations (after colons) — Light Silver
        val typeRegex = Regex(":\\s*([A-Z][A-Za-z0-9_<>?]*)")
        typeRegex.findAll(code).forEach { matchResult ->
            val group = matchResult.groups[1] ?: return@forEach
            addStyle(
                style = SpanStyle(color = Color(0xFFD1D1D6)),
                start = group.range.first,
                end = group.range.last + 1
            )
        }
    }
}

package com.example.rabit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SkeuoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    background: Brush = Brush.verticalGradient(
        listOf(
            Color(0xFFF2F2F7), // Platinum
            Color(0xFFE0E0E5),
            Color(0xFFB0B0B8)
        )
    ),
    shadowColor: Color = Color(0x22000000),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(cornerRadius), ambientColor = shadowColor, spotColor = shadowColor)
            .clip(RoundedCornerShape(cornerRadius))
            .background(background)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

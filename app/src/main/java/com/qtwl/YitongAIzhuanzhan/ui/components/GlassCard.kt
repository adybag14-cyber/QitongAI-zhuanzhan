package com.qtwl.YitongAIzhuanzhan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qtwl.YitongAIzhuanzhan.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    elevation: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == GlassBackgroundDark
    val surfaceColor = if (isDark) GlassSurfaceDarkMode else GlassSurface
    val borderColor = if (isDark) GlassBorderDark else GlassBorder
    val highlightColor = if (isDark) CrystalHighlightDark else CrystalHighlight

    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = GlassShadow, spotColor = GlassShadow)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        surfaceColor,
                        if (isDark) GlassSurfaceDarkMode2 else GlassSurfaceDark
                    )
                )
            )
    ) {
        // 水晶高光
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(highlightColor, Color.Transparent),
                        startY = 0f,
                        endY = 200f
                    )
                )
        )
        // 边框
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(borderColor)
        )
        // 内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .clip(shape)
                .background(Color.Transparent)
        ) {
            content()
        }
    }
}
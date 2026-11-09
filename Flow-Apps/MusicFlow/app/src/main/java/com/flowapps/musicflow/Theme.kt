package com.flowapps.musicflow

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FlowTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFFFF3D81), onBackground = Color(0xFFFFE8F2)
    ) else lightColorScheme(
        primary = Color(0xFFD6246A), onBackground = Color(0xFF2E0F22)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    val dark = isSystemInDarkTheme()
    val base = if (dark) Color(0xFF12061C) else Color(0xFFFFF0F6)
    val t = rememberInfiniteTransition(label = "orbs")
    val shift by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "s"
    )
    Box(
        Modifier.fillMaxSize().background(base).drawBehind {
            val r = size.width * 0.75f
            val c1 = Offset(size.width * (0.15f + 0.3f * shift), size.height * 0.18f)
            val c2 = Offset(size.width * (0.9f - 0.3f * shift), size.height * 0.75f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFFF3D81).copy(alpha = 0.55f), Color.Transparent), c1, r),
                radius = r, center = c1
            )
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF7B2FF7).copy(alpha = 0.5f), Color.Transparent), c2, r),
                radius = r, center = c2
            )
        },
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = if (dark) 0.22f else 0.5f), tint.copy(alpha = if (dark) 0.08f else 0.25f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.35f))),
                shape
            )
            .padding(16.dp),
        content = content
    )
}

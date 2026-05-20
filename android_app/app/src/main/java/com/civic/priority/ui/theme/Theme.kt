package com.civic.priority.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─── Color Palette (exact match to iOS Theme.swift) ───

object CivicColors {
    // Neon Yellow/Green accent
    val Primary = Color(0xFFD1FF00)       // rgb(0.82, 1.0, 0.0)
    val Secondary = Color(0xFF99CC00)     // rgb(0.6, 0.8, 0.0)

    // Deep dark navy/black background
    val Background = Color(0xFF0D121C)    // rgb(0.05, 0.07, 0.11)

    // Slightly lighter card background
    val CardBackground = Color(0xFF1C2430) // rgb(0.11, 0.14, 0.19)

    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFA6B3BF)  // rgb(0.65, 0.7, 0.75)

    // Login palette (mirrors LoginPalette in AuthView.swift)
    val Glow = Color(0xFF8CBF00)           // rgb(0.55, 0.75, 0.0)
    val GlowDeep = Color(0xFF598000)       // rgb(0.35, 0.50, 0.0)
    val Button = Color(0xFFD1FF00)         // rgb(0.82, 1.0, 0.0)
    val ButtonLt = Color(0xFF99CC00)       // rgb(0.6, 0.8, 0.0)
    val LoginCardBg = Color(0xFF141A24)    // rgb(0.08, 0.10, 0.14)
    val LoginFieldBg = Color(0xFF0F141C)   // rgb(0.06, 0.08, 0.11)
    val LoginBg = Color(0xFF080A12)        // rgb(0.03, 0.04, 0.07)
    val Subtitle = Color(0xFF99A6B3)       // rgb(0.6, 0.65, 0.7)

    val MainGradient = Brush.linearGradient(
        colors = listOf(Primary, Primary.copy(alpha = 0.8f))
    )

    fun scoreColor(score: Double): Color {
        return when {
            score >= 80 -> Color.Red
            score >= 50 -> Color(0xFFFF9800)
            score >= 20 -> Color.Yellow
            else -> Primary
        }
    }
}

// ─── Dark Color Scheme ───

private val DarkColorScheme = darkColorScheme(
    primary = CivicColors.Primary,
    secondary = CivicColors.Secondary,
    background = CivicColors.Background,
    surface = CivicColors.CardBackground,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = CivicColors.TextPrimary,
    onSurface = CivicColors.TextPrimary,
    surfaceVariant = CivicColors.CardBackground,
    onSurfaceVariant = CivicColors.TextSecondary,
    outline = Color.White.copy(alpha = 0.12f)
)

// ─── Theme ───

@Composable
fun CivicPriorityTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = CivicColors.TextPrimary
            ),
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = CivicColors.TextPrimary
            )
        ),
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}

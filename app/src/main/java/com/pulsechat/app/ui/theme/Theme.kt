package com.pulsechat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val dim: Color,
    val primary: Color,
    val accent: Color,
    val ok: Color,
    val danger: Color,
    val bubbleMine: Color,
    val bubbleOther: Color,
    val onBubble: Color,
    val isDark: Boolean,
)

val DarkColors = AppColors(
    bg = Color(0xFF0B0F1A),
    surface = Color(0xFF151B2B),
    surface2 = Color(0xFF1F2740),
    text = Color(0xFFEAF0FF),
    dim = Color(0xFF97A3C0),
    primary = Color(0xFF00D9FF),
    accent = Color(0xFF7C5CFF),
    ok = Color(0xFF2ED573),
    danger = Color(0xFFFF5252),
    bubbleMine = Color(0xFF6C4DF6),
    bubbleOther = Color(0xFF1E2637),
    onBubble = Color.White,
    isDark = true,
)

val LightColors = AppColors(
    bg = Color(0xFFF3F5FA),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE7ECF6),
    text = Color(0xFF0F1322),
    dim = Color(0xFF6B7385),
    primary = Color(0xFF0B84D6),
    accent = Color(0xFF6C4DF6),
    ok = Color(0xFF17A34A),
    danger = Color(0xFFDC2626),
    bubbleMine = Color(0xFF3B82F6),
    bubbleOther = Color(0xFFFFFFFF),
    onBubble = Color.White,
    isDark = false,
)

/** Chat wallpapers (gradient pairs). */
val Wallpapers: List<Pair<Color, Color>> = listOf(
    Color(0xFF0B0F1A) to Color(0xFF151B2B),
    Color(0xFF102A43) to Color(0xFF0A1A2B),
    Color(0xFF2B1055) to Color(0xFF3B2A7A),
    Color(0xFF1B4332) to Color(0xFF0A1F14),
    Color(0xFFF3F5FA) to Color(0xFFE7ECF6),
)

val LocalAppColors = staticCompositionLocalOf { DarkColors }

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

private val darkScheme = darkColorScheme(
    primary = DarkColors.primary,
    secondary = DarkColors.accent,
    background = DarkColors.bg,
    surface = DarkColors.surface,
    onBackground = DarkColors.text,
    onSurface = DarkColors.text,
)

private val lightScheme = lightColorScheme(
    primary = LightColors.primary,
    secondary = LightColors.accent,
    background = LightColors.bg,
    surface = LightColors.surface,
    onBackground = LightColors.text,
    onSurface = LightColors.text,
)

@Composable
fun PulseChatTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = if (dark) darkScheme else lightScheme, content = content)
    }
}

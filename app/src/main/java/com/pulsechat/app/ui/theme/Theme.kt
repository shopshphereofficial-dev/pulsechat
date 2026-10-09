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
    val bubbleMineEnd: Color,
    val bubbleOther: Color,
    val onBubble: Color,
    val border: Color,
    val isDark: Boolean,
)

val DarkColors = AppColors(
    bg = Color(0xFF070A14),
    surface = Color(0xFF111827),
    surface2 = Color(0xFF1B2333),
    text = Color(0xFFF2F5FF),
    dim = Color(0xFF8E9AB5),
    primary = Color(0xFF4C8DFF),
    accent = Color(0xFF8B5CF6),
    ok = Color(0xFF22C55E),
    danger = Color(0xFFEF4444),
    bubbleMine = Color(0xFF4F46E5),
    bubbleMineEnd = Color(0xFF7C3AED),
    bubbleOther = Color(0xFF1A2233),
    onBubble = Color.White,
    border = Color(0xFF232C40),
    isDark = true,
)

val LightColors = AppColors(
    bg = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEDF0F7),
    text = Color(0xFF0B1020),
    dim = Color(0xFF6B7280),
    primary = Color(0xFF2563EB),
    accent = Color(0xFF7C3AED),
    ok = Color(0xFF16A34A),
    danger = Color(0xFFDC2626),
    bubbleMine = Color(0xFF2563EB),
    bubbleMineEnd = Color(0xFF6D28D9),
    bubbleOther = Color(0xFFFFFFFF),
    onBubble = Color.White,
    border = Color(0xFFE2E6EF),
    isDark = false,
)

/** Chat wallpapers (gradient pairs). */
val Wallpapers: List<Pair<Color, Color>> = listOf(
    Color(0xFF070A14) to Color(0xFF141C30),
    Color(0xFF0B1B2B) to Color(0xFF07131F),
    Color(0xFF1B0F33) to Color(0xFF2C1B4D),
    Color(0xFF06231A) to Color(0xFF04140F),
    Color(0xFFF6F7FB) to Color(0xFFE6EAF3),
    Color(0xFF101B2E) to Color(0xFF243352),
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

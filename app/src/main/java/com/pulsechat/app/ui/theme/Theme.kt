package com.pulsechat.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cyan = Color(0xFF00E5FF)
val Violet = Color(0xFF7C4DFF)
val Green = Color(0xFF00E676)
val Red = Color(0xFFFF5252)
val Bg = Color(0xFF070B16)
val Surface1 = Color(0xFF111726)
val Surface2 = Color(0xFF1B2236)
val TextMain = Color(0xFFEAF0FF)
val TextDim = Color(0xFF97A3C0)

private val scheme = darkColorScheme(
    primary = Cyan,
    secondary = Violet,
    tertiary = Green,
    background = Bg,
    surface = Surface1,
    onPrimary = Color(0xFF00121A),
    onSecondary = Color.White,
    onBackground = TextMain,
    onSurface = TextMain,
)

@Composable
fun PulseChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

package com.pulsechat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsechat.app.data.Prefs
import com.pulsechat.app.data.Profile
import com.pulsechat.app.ui.theme.AppTheme
import com.pulsechat.app.ui.theme.Wallpapers

@Composable
fun SettingsScreen(
    profile: Profile?,
    prefs: Prefs,
    onThemeChanged: (Int) -> Unit,
    onWallpaperChanged: (Int) -> Unit,
    onSignOut: () -> Unit,
) {
    val c = AppTheme.colors
    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var wallpaper by remember { mutableStateOf(prefs.wallpaper) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Spacer(Modifier.height(24.dp))
        TopBar("Settings")
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Avatar(profile, size = 64, showOnline = true)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(profile?.label ?: "-", color = c.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(profile?.handle ?: "", color = c.primary)
            }
        }

        SectionHeader("Appearance")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("System" to 0, "Light" to 1, "Dark" to 2).forEach { (label, mode) ->
                val active = themeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) c.primary.copy(alpha = 0.22f) else c.surface)
                        .border(1.dp, if (active) c.primary else c.surface2, RoundedCornerShape(14.dp))
                        .clickable { themeMode = mode; prefs.themeMode = mode; onThemeChanged(mode) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = if (active) c.primary else c.text) }
            }
        }

        SectionHeader("Chat wallpaper")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Wallpapers.forEachIndexed { i, w ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(w.first, w.second)))
                        .border(if (wallpaper == i) 3.dp else 1.dp, if (wallpaper == i) c.primary else c.surface2, CircleShape)
                        .clickable { wallpaper = i; prefs.wallpaper = i; onWallpaperChanged(i) },
                )
            }
        }

        SectionHeader("Account")
        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = c.danger),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null, tint = c.onBubble)
            Spacer(Modifier.width(8.dp))
            Text("Sign out")
        }

        Spacer(Modifier.height(20.dp))
        Text("PulseChat v2.2", color = c.dim, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(40.dp))
    }
}

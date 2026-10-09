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
import com.pulsechat.app.data.Repo
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pulsechat.app.ui.theme.AppTheme
import com.pulsechat.app.ui.theme.Wallpapers

@Composable
fun SettingsScreen(
    profile: Profile?,
    repo: Repo,
    onProfileSaved: () -> Unit,
    prefs: Prefs,
    onThemeChanged: (Int) -> Unit,
    onWallpaperChanged: (Int) -> Unit,
    onSignOut: () -> Unit,
) {
    val c = AppTheme.colors
    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var wallpaper by remember { mutableStateOf(prefs.wallpaper) }
    var editing by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) { com.pulsechat.app.util.ImageUtil.compress(context, uri, 512, 80) }
                    if (bytes != null) {
                        withContext(Dispatchers.IO) { repo.uploadAvatar(bytes, "jpg", "image/jpeg") }
                        onProfileSaved()
                    }
                } catch (e: Exception) { uploadError = e.message }
            }
        }
    }
    LaunchedEffect(Unit) { blocked = withContext(Dispatchers.IO) { repo.blockedUsers() } }

    if (editing) {
        EditProfileScreen(profile, repo, onDone = { editing = false; onProfileSaved() })
        return
    }

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

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { editing = true },
            colors = ButtonDefaults.buttonColors(containerColor = c.accent),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Edit profile") }
        profile?.status?.let { Spacer(Modifier.height(6.dp)); Text(it, color = c.primary) }
        profile?.bio?.let { Spacer(Modifier.height(4.dp)); Text(it, color = c.dim, style = MaterialTheme.typography.bodySmall) }

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

        SectionHeader("Profile photo")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile, size = 56)
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { photoPicker.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = c.primary),
            ) { Text("Change photo") }
        }

        SectionHeader("Blocked users")
        if (blocked.isEmpty()) Text("No blocked users", color = c.dim, style = MaterialTheme.typography.bodySmall)
        else blocked.forEach { b ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Avatar(b, size = 36)
                Spacer(Modifier.width(10.dp))
                Text(b.handle, color = c.text, modifier = Modifier.weight(1f))
                TextButton(onClick = { scope.launch { withContext(Dispatchers.IO) { repo.unblock(b.id) }; blocked = withContext(Dispatchers.IO) { repo.blockedUsers() } } }) { Text("Unblock", color = c.ok) }
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


@Composable
fun EditProfileScreen(profile: Profile?, repo: Repo, onDone: () -> Unit) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(profile?.displayName ?: "") }
    var status by remember { mutableStateOf(profile?.status ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Spacer(Modifier.height(24.dp))
        TopBar("Edit profile", onBack = onDone)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile, size = 72)
            Spacer(Modifier.width(14.dp))
            Text(profile?.handle ?: "", color = c.primary, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = status, onValueChange = { status = it.take(80) }, label = { Text("Status (about)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = bio, onValueChange = { bio = it.take(200) }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = c.danger, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                busy = true; error = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { repo.updateProfile(name.trim(), bio.trim(), status.trim()) }
                        onDone()
                    } catch (e: Exception) { error = e.message } finally { busy = false }
                }
            },
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = c.ok),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
        Spacer(Modifier.height(40.dp))
    }
}

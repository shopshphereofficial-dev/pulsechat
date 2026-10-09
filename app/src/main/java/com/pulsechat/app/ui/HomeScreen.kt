package com.pulsechat.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsechat.app.data.ChatSummary
import com.pulsechat.app.data.Prefs
import com.pulsechat.app.data.Profile
import com.pulsechat.app.data.Repo
import com.pulsechat.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    repo: Repo,
    profile: Profile?,
    prefs: Prefs,
    onThemeChanged: (Int) -> Unit,
    onWallpaperChanged: (Int) -> Unit,
    onProfileSaved: () -> Unit,
    onOpenChat: (ChatTarget) -> Unit,
    onSignOut: () -> Unit,
) {
    val c = AppTheme.colors
    var tab by remember { mutableStateOf(0) }
    var showNewChat by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Box(Modifier.weight(1f)) {
            when {
                showNewChat -> NewChatScreen(
                    repo = repo,
                    onOpenChat = { showNewChat = false; onOpenChat(it) },
                    onBack = { showNewChat = false },
                )
                tab == 0 -> ChatListScreen(repo = repo, onOpenChat = onOpenChat, onNewChat = { showNewChat = true })
                tab == 1 -> FriendsScreen(repo = repo, onOpenChat = onOpenChat)
                else -> SettingsScreen(
                    profile = profile,
                    repo = repo,
                    onProfileSaved = onProfileSaved,
                    prefs = prefs,
                    onThemeChanged = onThemeChanged,
                    onWallpaperChanged = onWallpaperChanged,
                    onSignOut = onSignOut,
                )
            }
        }
        if (!showNewChat) {
            Row(
                modifier = Modifier.fillMaxWidth().background(c.surface).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val tabs = listOf(
                    Triple("Chats", Icons.Filled.Chat, 0),
                    Triple("Friends", Icons.Filled.People, 1),
                    Triple("Settings", Icons.Filled.Settings, 2),
                )
                tabs.forEach { (label, icon, i) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .clickable { tab = i }
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                    ) {
                        Icon(icon, contentDescription = label, tint = if (tab == i) c.primary else c.dim)
                        Text(label, color = if (tab == i) c.primary else c.dim, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListScreen(repo: Repo, onOpenChat: (ChatTarget) -> Unit, onNewChat: () -> Unit) {
    val c = AppTheme.colors
    var items by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { while (true) { tick++; delay(3000) } }
    LaunchedEffect(tick) {
        try {
            items = withContext(Dispatchers.IO) { repo.myConversations() }
            error = null
        } catch (e: Exception) { error = e.message }
        loading = false
    }

    val shown = items.filter { query.isBlank() || it.title.contains(query, true) || (it.lastMessage ?: "").contains(query, true) }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(24.dp))
        TopBar(
            title = "PulseChat",
            trailing = {
                IconButton(onClick = onNewChat) { Icon(Icons.Filled.Add, contentDescription = "New chat", tint = c.primary) }
            },
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search chats") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        error?.let { Text(it, color = c.danger, style = MaterialTheme.typography.bodySmall) }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (query.isBlank()) "No chats yet.\nTap + to start one." else "Nothing found", color = c.dim)
            }
            else -> LazyColumn {
                items(shown, key = { it.conversation.id }) { s ->
                    RowItem(onClick = { onOpenChat(ChatTarget(s.conversation.id, s.title, s.other, s.conversation.isGroup)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(s.other, size = 50, showOnline = !s.conversation.isGroup)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, color = c.text, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(s.lastMessage ?: "No messages yet", color = c.dim, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                            Text(shortTime(s.lastAt), color = c.dim, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewChatScreen(repo: Repo, onOpenChat: (ChatTarget) -> Unit, onBack: () -> Unit) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var groupMode by remember { mutableStateOf(false) }
    var groupTitle by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try { friends = withContext(Dispatchers.IO) { repo.friends() } } catch (e: Exception) { error = e.message }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(24.dp))
        TopBar(
            title = if (groupMode) "New group" else "New chat",
            onBack = onBack,
            trailing = {
                IconButton(onClick = { groupMode = !groupMode; selected = emptySet() }) {
                    Icon(Icons.Filled.Group, contentDescription = "Group", tint = c.primary)
                }
            },
        )
        if (groupMode) {
            OutlinedTextField(
                value = groupTitle,
                onValueChange = { groupTitle = it },
                label = { Text("Group name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        error?.let { Text(it, color = c.danger, style = MaterialTheme.typography.bodySmall) }
        if (loading) {
            CircularProgressIndicator()
        } else if (friends.isEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("No friends yet — Friends tab se add karo.", color = c.dim)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(friends, key = { it.id }) { p ->
                    RowItem(onClick = {
                        if (groupMode) {
                            selected = if (selected.contains(p.id)) selected - p.id else selected + p.id
                        } else {
                            scope.launch {
                                try {
                                    val cid = withContext(Dispatchers.IO) { repo.openDirect(p.id) }
                                    onOpenChat(ChatTarget(cid, p.handle, p, false))
                                } catch (e: Exception) { error = e.message }
                            }
                        }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(p, size = 46, showOnline = true)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.handle, color = c.text, fontWeight = FontWeight.Medium)
                                Text(presenceText(p), color = c.dim, style = MaterialTheme.typography.bodySmall)
                            }
                            if (groupMode && selected.contains(p.id)) Text("OK", color = c.ok, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (groupMode) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val cid = withContext(Dispatchers.IO) { repo.createGroup(groupTitle.ifBlank { "Group" }, selected.toList()) }
                                onOpenChat(ChatTarget(cid, groupTitle.ifBlank { "Group" }, null, true))
                            } catch (e: Exception) { error = e.message }
                        }
                    },
                    enabled = selected.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = c.ok),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Create group") }
            }
        }
    }
}

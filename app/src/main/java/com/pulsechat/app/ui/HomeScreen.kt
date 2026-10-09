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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsechat.app.data.ChatSummary
import com.pulsechat.app.data.Profile
import com.pulsechat.app.data.Repo
import com.pulsechat.app.ui.theme.Cyan
import com.pulsechat.app.ui.theme.Green
import com.pulsechat.app.ui.theme.Red
import com.pulsechat.app.ui.theme.Surface1
import com.pulsechat.app.ui.theme.Surface2
import com.pulsechat.app.ui.theme.TextDim
import com.pulsechat.app.ui.theme.TextMain
import com.pulsechat.app.ui.theme.Violet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    repo: Repo,
    profile: Profile?,
    onOpenChat: (ChatTarget) -> Unit,
    onSignOut: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var showNewChat by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when {
                showNewChat -> NewChatScreen(
                    repo = repo,
                    onOpenChat = { showNewChat = false; onOpenChat(it) },
                    onBack = { showNewChat = false },
                )
                tab == 0 -> ChatListScreen(repo = repo, onOpenChat = onOpenChat, onNewChat = { showNewChat = true })
                tab == 1 -> FriendsScreen(repo = repo, onOpenChat = onOpenChat)
                else -> ProfileScreen(profile = profile, onSignOut = onSignOut)
            }
        }
        if (!showNewChat) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Surface1).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val tabs = listOf("Chats", "Friends", "Profile")
                tabs.forEachIndexed { i, label ->
                    Text(
                        label,
                        color = if (tab == i) Cyan else TextDim,
                        fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { tab = i }
                            .padding(horizontal = 22.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListScreen(repo: Repo, onOpenChat: (ChatTarget) -> Unit, onNewChat: () -> Unit) {
    var items by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            tick++
            delay(3000)
        }
    }
    LaunchedEffect(tick) {
        try {
            items = withContext(Dispatchers.IO) { repo.myConversations() }
            error = null
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(Modifier.height(28.dp))
        TopBar(
            title = "Chats",
            trailing = { TextButton(onClick = onNewChat) { Text("New chat", color = Cyan) } },
        )
        Spacer(Modifier.height(8.dp))
        error?.let { Text(it, color = Red, style = MaterialTheme.typography.bodySmall) }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No chats yet.\nTap 'New chat' to start one.", color = TextDim)
            }
            else -> LazyColumn {
                items(items) { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface1)
                            .clickable {
                                onOpenChat(
                                    ChatTarget(s.conversation.id, s.title, s.other, s.conversation.isGroup)
                                )
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(s.other, size = 48, showOnline = !s.conversation.isGroup)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, color = TextMain, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                s.lastMessage ?: "No messages yet",
                                color = TextDim,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                        }
                        Text(shortTime(s.lastAt), color = TextDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun NewChatScreen(repo: Repo, onOpenChat: (ChatTarget) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var groupMode by remember { mutableStateOf(false) }
    var groupTitle by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            friends = withContext(Dispatchers.IO) { repo.friends() }
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    fun openDirect(p: Profile) {
        scope.launch {
            try {
                val cid = withContext(Dispatchers.IO) { repo.openDirect(p.id) }
                onOpenChat(ChatTarget(cid, p.handle, p, false))
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(Modifier.height(28.dp))
        TopBar(
            title = if (groupMode) "New group" else "New chat",
            onBack = onBack,
            trailing = {
                TextButton(onClick = { groupMode = !groupMode; selected = emptySet() }) {
                    Text(if (groupMode) "Direct" else "Group", color = Cyan)
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
            Text("Pick members (${selected.size})", color = TextDim, style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(it, color = Red, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(8.dp))
        if (loading) {
            CircularProgressIndicator()
        } else if (friends.isEmpty()) {
            Text("No friends yet. Add friends from the Friends tab.", color = TextDim)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(friends) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected.contains(p.id)) Surface2 else Surface1)
                            .clickable {
                                if (groupMode) {
                                    selected = if (selected.contains(p.id)) selected - p.id else selected + p.id
                                } else {
                                    openDirect(p)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(p, size = 44, showOnline = true)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.handle, color = TextMain, fontWeight = FontWeight.Medium)
                            Text(presenceText(p), color = TextDim, style = MaterialTheme.typography.bodySmall)
                        }
                        if (groupMode && selected.contains(p.id)) Text("OK", color = Green, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (groupMode) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val cid = withContext(Dispatchers.IO) {
                                    repo.createGroup(groupTitle.ifBlank { "Group" }, selected.toList())
                                }
                                onOpenChat(ChatTarget(cid, groupTitle.ifBlank { "Group" }, null, true))
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    },
                    enabled = selected.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Create group") }
            }
        }
    }
}

@Composable
fun ProfileScreen(profile: Profile?, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Spacer(Modifier.height(30.dp))
        TopBar("Profile")
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile, size = 72, showOnline = true)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(profile?.handle ?: "-", color = TextMain, style = MaterialTheme.typography.titleLarge)
                Text(profile?.displayName ?: "", color = TextDim)
                Text(presenceText(profile), color = Green, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(30.dp))
        Button(onClick = onSignOut, colors = ButtonDefaults.buttonColors(containerColor = Red)) {
            Text("Sign out")
        }
    }
}

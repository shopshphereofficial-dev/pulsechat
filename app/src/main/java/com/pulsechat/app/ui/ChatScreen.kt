package com.pulsechat.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulsechat.app.data.Message
import com.pulsechat.app.data.Profile
import com.pulsechat.app.data.Repo
import com.pulsechat.app.ui.theme.AppTheme
import com.pulsechat.app.ui.theme.Wallpapers
import com.pulsechat.app.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val REACTIONS = listOf("\u2764", "\uD83D\uDE02", "\uD83D\uDC4D", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDE4F")

@Composable
fun ChatScreen(
    repo: Repo,
    target: ChatTarget,
    myId: String,
    wallpaper: Int,
    onBack: () -> Unit,
    onStartCall: (Profile, String) -> Unit,
) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val wp = Wallpapers.getOrElse(wallpaper) { Wallpapers[0] }

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableStateOf(0) }
    var replyTo by remember { mutableStateOf<Message?>(null) }
    var actionMsg by remember { mutableStateOf<Message?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    var showMembers by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var otherTyping by remember { mutableStateOf(false) }
    var otherRead by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQ by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Message>>(emptyList()) }
    var editTarget by remember { mutableStateOf<Message?>(null) }
    var forwardMsg by remember { mutableStateOf<Message?>(null) }
    var conversations by remember { mutableStateOf<List<com.pulsechat.app.data.ChatSummary>>(emptyList()) }
    val listState = rememberLazyListState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pendingUri = uri
    }

    fun refresh() {
        scope.launch {
            try {
                messages = withContext(Dispatchers.IO) { repo.messages(target.conversationId) }
                error = null
                if (!target.isGroup && target.other != null) {
                    otherTyping = withContext(Dispatchers.IO) { repo.isTyping(target.conversationId, target.other.id) }
                    otherRead = withContext(Dispatchers.IO) { repo.otherLastRead(target.conversationId, target.other.id) }
                }
            } catch (e: Exception) { error = e.message }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { repo.markRead(target.conversationId) }
        while (true) { tick++; delay(2000) }
    }
    LaunchedEffect(tick) { refresh() }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) runCatching { listState.animateScrollToItem(messages.size - 1) }
    }

    val feed = remember(messages) {
        val out = ArrayList<Any>()
        var lastDay = ""
        messages.forEach { m ->
            val d = dayLabel(m.createdAt)
            if (d.isNotEmpty() && d != lastDay) { out.add(d); lastDay = d }
            out.add(m)
        }
        out
    }

    fun sendMedia(uri: Uri, low: Boolean) {
        uploading = true
        val replyId = replyTo?.id
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    if (low) ImageUtil.compress(context, uri) else ImageUtil.readBytes(context, uri)
                }
                if (bytes == null) { error = "Could not read image"; uploading = false; return@launch }
                val mime = "image/jpeg"
                val url = withContext(Dispatchers.IO) { repo.uploadMedia(bytes, "jpg", mime) }
                withContext(Dispatchers.IO) {
                    repo.sendMedia(target.conversationId, url, mime, "photo.jpg", replyTo = replyId)
                }
                replyTo = null
                refresh()
            } catch (e: Exception) { error = e.message }
            uploading = false
        }
    }

    val pinned = messages.lastOrNull { it.pinned }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(wp.first, wp.second)))) {
        Box(Modifier.padding(horizontal = 12.dp)) {
            TopBar(
                title = target.title,
                subtitle = when {
                    otherTyping -> "typing…"
                    target.isGroup -> "group chat"
                    else -> presenceText(target.other)
                },
                onBack = onBack,
                trailing = {
                    IconButton(onClick = { searchOpen = !searchOpen; searchQ = ""; searchResults = emptyList() }) {
                        Icon(Icons.Filled.Search, "Search", tint = c.dim)
                    }
                    val other = target.other
                    if (!target.isGroup && other != null) {
                        IconButton(onClick = { onStartCall(other, "voice") }) { Icon(Icons.Filled.Call, null, tint = c.primary) }
                        IconButton(onClick = { onStartCall(other, "video") }) { Icon(Icons.Filled.Videocam, null, tint = c.accent) }
                    }
                },
            )
        }
        if (searchOpen) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQ, onValueChange = { searchQ = it },
                    placeholder = { Text("Search messages") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = {
                        scope.launch {
                            searchResults = withContext(Dispatchers.IO) { repo.searchMessages(target.conversationId, searchQ) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent),
                ) { Text("Go") }
            }
            if (searchResults.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().background(c.surface).padding(10.dp)) {
                    Text("${searchResults.size} results", color = c.primary, style = MaterialTheme.typography.labelSmall)
                    searchResults.take(8).forEach { r ->
                        Text("\u2022 ${r.content ?: "Photo"}", color = c.dim, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
        if (!target.isGroup && target.other != null) {
            Text(
                "@${target.other.username ?: "user"} — tap for profile",
                color = c.dim, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 14.dp).clickable { showProfile = true },
            )
        } else {
            Text(
                "group — tap for members",
                color = c.dim, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 14.dp).clickable { showMembers = true },
            )
        }
        pinned?.let { p ->
            Row(Modifier.fillMaxWidth().background(c.surface.copy(alpha = 0.9f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Pinned: ${p.content ?: "Photo"}", color = c.primary, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.weight(1f))
                IconButton(onClick = { scope.launch { withContext(Dispatchers.IO) { repo.togglePin(p.id, false) }; refresh() } }) {
                    Icon(Icons.Filled.Close, null, tint = c.dim)
                }
            }
        }
        error?.let { Text(it, color = c.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 14.dp)) }

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), state = listState) {
            items(feed, key = { item -> if (item is Message) item.id else "d$item" }) { item ->
                if (item is String) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(c.surface.copy(alpha = 0.85f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(item, color = c.dim, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    val m = item as Message
                    val mine = m.senderId == myId
                    val quoted = m.replyTo?.let { rid -> messages.firstOrNull { it.id == rid } }
                    val seen = mine && otherRead.isNotEmpty() && m.createdAt <= otherRead
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (mine) c.bubbleMine else c.bubbleOther)
                                .clickable { actionMsg = m }
                                .padding(8.dp),
                        ) {
                            quoted?.let { q ->
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.bg.copy(alpha = 0.35f)).padding(6.dp)) {
                                    Text(q.content ?: "Photo", color = if (mine) c.onBubble else c.dim, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            if (m.hasMedia && m.isImage) {
                                AsyncImage(
                                    model = m.mediaUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)),
                                )
                            }
                            m.content?.takeIf { it.isNotBlank() }?.let { Text(it, color = if (mine) c.onBubble else c.text) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (m.editedAt != null) Text("edited ", color = if (mine) c.onBubble.copy(alpha = 0.7f) else c.dim, style = MaterialTheme.typography.labelSmall)
                                Text(shortTime(m.createdAt), color = if (mine) c.onBubble.copy(alpha = 0.7f) else c.dim, style = MaterialTheme.typography.labelSmall)
                                if (mine) {
                                    Spacer(Modifier.width(3.dp))
                                    Text(if (seen) "\u2713\u2713" else "\u2713", color = if (seen) c.primary else c.onBubble.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            m.reaction?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }

        replyTo?.let { r ->
            Row(Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Replying to", color = c.primary, style = MaterialTheme.typography.labelSmall)
                    Text(r.content ?: "Photo", color = c.dim, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                IconButton(onClick = { replyTo = null }) { Icon(Icons.Filled.Close, null, tint = c.dim) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { picker.launch("image/*") }) { Icon(Icons.Filled.AttachFile, "Attach", tint = c.primary) }
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    scope.launch { withContext(Dispatchers.IO) { repo.setTyping(target.conversationId) } }
                },
                placeholder = { Text(if (uploading) "Uploading…" else "Message") },
                modifier = Modifier.weight(1f), maxLines = 4,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                val t = text.trim()
                if (t.isNotEmpty()) {
                    val rid = replyTo?.id
                    text = ""
                    replyTo = null
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { repo.sendMessage(target.conversationId, t, rid) }
                            refresh()
                        } catch (e: Exception) { error = e.message }
                    }
                }
            }) { Icon(Icons.Filled.Send, "Send", tint = c.primary) }
        }
    }

    pendingUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingUri = null },
            confirmButton = {},
            dismissButton = {},
            title = { Text("Send photo") },
            text = {
                Column {
                    Text("Choose quality", color = c.dim, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { val u = uri; pendingUri = null; sendMedia(u, false) }, colors = ButtonDefaults.buttonColors(containerColor = c.primary), modifier = Modifier.fillMaxWidth()) { Text("Original") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { val u = uri; pendingUri = null; sendMedia(u, true) }, colors = ButtonDefaults.buttonColors(containerColor = c.ok), modifier = Modifier.fillMaxWidth()) { Text("Low quality (smaller)") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { pendingUri = null }, modifier = Modifier.fillMaxWidth()) { Text("Cancel", color = c.dim) }
                }
            },
        )
    }

    actionMsg?.let { m ->
        val mine = m.senderId == myId
        AlertDialog(
            onDismissRequest = { actionMsg = null },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { actionMsg = null }) { Text("Close") } },
            title = { Text("Message") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        REACTIONS.forEach { e ->
                            Text(e, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                                actionMsg = null
                                scope.launch { withContext(Dispatchers.IO) { repo.setReaction(m.id, e) }; refresh() }
                            }.padding(6.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { replyTo = m; actionMsg = null }) { Text("Reply", color = c.primary) }
                    if (!m.content.isNullOrBlank()) {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(m.content)); actionMsg = null }) { Text("Copy", color = c.primary) }
                    }
                    TextButton(onClick = {
                        actionMsg = null
                        scope.launch {
                            conversations = withContext(Dispatchers.IO) { repo.myConversations() }
                            forwardMsg = m
                        }
                    }) { Text("Forward", color = c.accent) }
                    TextButton(onClick = {
                        actionMsg = null
                        scope.launch { withContext(Dispatchers.IO) { repo.togglePin(m.id, !m.pinned) }; refresh() }
                    }) { Text(if (m.pinned) "Unpin" else "Pin", color = c.accent) }
                    if (mine) {
                        TextButton(onClick = { editTarget = m; actionMsg = null }) { Text("Edit", color = c.primary) }
                        TextButton(onClick = {
                            actionMsg = null
                            scope.launch { withContext(Dispatchers.IO) { repo.deleteMessage(m.id) }; refresh() }
                        }) { Text("Delete", color = c.danger) }
                    }
                    if (!mine && target.other != null) {
                        TextButton(onClick = { actionMsg = null; showProfile = true }) { Text("View profile", color = c.accent) }
                    }
                }
            },
        )
    }

    editTarget?.let { m ->
        var newText by remember { mutableStateOf(m.content ?: "") }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    val t = newText; editTarget = null
                    scope.launch { withContext(Dispatchers.IO) { repo.editMessage(m.id, t) }; refresh() }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Cancel") } },
            title = { Text("Edit message") },
            text = { OutlinedTextField(value = newText, onValueChange = { newText = it }, modifier = Modifier.fillMaxWidth()) },
        )
    }

    forwardMsg?.let { m ->
        AlertDialog(
            onDismissRequest = { forwardMsg = null },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { forwardMsg = null }) { Text("Close") } },
            title = { Text("Forward to") },
            text = {
                Column {
                    if (conversations.isEmpty()) Text("No chats", color = c.dim)
                    conversations.take(8).forEach { s ->
                        TextButton(onClick = {
                            forwardMsg = null
                            scope.launch {
                                withContext(Dispatchers.IO) { repo.forward(s.conversation.id, m) }
                                if (s.conversation.id == target.conversationId) refresh()
                            }
                        }) { Text(s.title, color = c.text) }
                    }
                }
            },
        )
    }

    if (showProfile && target.other != null) {
        val p = target.other
        var blocked by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { blocked = withContext(Dispatchers.IO) { repo.blockedIds() }.contains(p.id) }
        AlertDialog(
            onDismissRequest = { showProfile = false },
            confirmButton = { TextButton(onClick = { showProfile = false }) { Text("Close") } },
            dismissButton = {},
            title = { Text("Profile") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Avatar(p, size = 84, showOnline = true)
                    Spacer(Modifier.height(10.dp))
                    Text(p.label, color = c.text, fontWeight = FontWeight.Bold)
                    Text(p.handle, color = c.primary)
                    Text(presenceText(p), color = c.dim, style = MaterialTheme.typography.bodySmall)
                    p.status?.takeIf { it.isNotBlank() }?.let { Spacer(Modifier.height(8.dp)); Text(it, color = c.text) }
                    p.bio?.takeIf { it.isNotBlank() }?.let { Spacer(Modifier.height(6.dp)); Text(it, color = c.dim, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { if (blocked) repo.unblock(p.id) else repo.block(p.id) }
                                blocked = !blocked
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (blocked) c.ok else c.danger),
                    ) { Text(if (blocked) "Unblock" else "Block") }
                }
            },
        )
    }

    if (showMembers) {
        var members by remember { mutableStateOf<List<Profile>>(emptyList()) }
        LaunchedEffect(Unit) { members = withContext(Dispatchers.IO) { repo.memberProfiles(target.conversationId) } }
        AlertDialog(
            onDismissRequest = { showMembers = false },
            confirmButton = { TextButton(onClick = { showMembers = false }) { Text("Close") } },
            dismissButton = {},
            title = { Text("Members (${members.size})") },
            text = {
                Column {
                    members.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Avatar(p, size = 36, showOnline = true)
                            Spacer(Modifier.width(10.dp))
                            Text(p.handle, color = c.text)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { repo.leaveGroup(target.conversationId) }
                                showMembers = false
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = c.danger),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Leave group") }
                }
            },
        )
    }
}

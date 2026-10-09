package com.pulsechat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsechat.app.data.Message
import com.pulsechat.app.data.Profile
import com.pulsechat.app.data.Repo
import com.pulsechat.app.ui.theme.AppTheme
import com.pulsechat.app.ui.theme.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val wp = Wallpapers.getOrElse(wallpaper) { Wallpapers[0] }

    LaunchedEffect(Unit) { while (true) { tick++; delay(2000) } }
    LaunchedEffect(tick) {
        try {
            messages = withContext(Dispatchers.IO) { repo.messages(target.conversationId) }
            error = null
        } catch (e: Exception) { error = e.message }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            try { listState.animateScrollToItem(messages.size - 1) } catch (_: Exception) {}
        }
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

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(wp.first, wp.second)))) {
        Box(Modifier.padding(horizontal = 12.dp)) {
            TopBar(
                title = target.title,
                subtitle = if (target.isGroup) "group chat" else presenceText(target.other),
                onBack = onBack,
                trailing = {
                    val other = target.other
                    if (!target.isGroup && other != null) {
                        IconButton(onClick = { onStartCall(other, "voice") }) {
                            Icon(Icons.Filled.Call, contentDescription = "Call", tint = c.primary)
                        }
                        IconButton(onClick = { onStartCall(other, "video") }) {
                            Icon(Icons.Filled.Videocam, contentDescription = "Video", tint = c.accent)
                        }
                    }
                },
            )
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (mine) c.bubbleMine else c.bubbleOther)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Column {
                                Text(m.content, color = if (mine) c.onBubble else c.text)
                                Text(shortTime(m.createdAt), color = if (mine) c.onBubble.copy(alpha = 0.7f) else c.dim, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Message") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty()) {
                        text = ""
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { repo.sendMessage(target.conversationId, t) }
                                messages = withContext(Dispatchers.IO) { repo.messages(target.conversationId) }
                            } catch (e: Exception) { error = e.message }
                        }
                    }
                },
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = c.primary)
            }
        }
    }
}

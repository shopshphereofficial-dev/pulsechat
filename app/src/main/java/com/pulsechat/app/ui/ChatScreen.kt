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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.pulsechat.app.data.Message
import com.pulsechat.app.data.Repo
import com.pulsechat.app.ui.theme.Cyan
import com.pulsechat.app.ui.theme.Red
import com.pulsechat.app.ui.theme.Surface1
import com.pulsechat.app.ui.theme.TextDim
import com.pulsechat.app.ui.theme.TextMain
import com.pulsechat.app.ui.theme.Violet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatScreen(repo: Repo, target: ChatTarget, myId: String, onBack: () -> Unit) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            tick++
            delay(2000)
        }
    }
    LaunchedEffect(tick) {
        try {
            messages = withContext(Dispatchers.IO) { repo.messages(target.conversationId) }
            error = null
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            try { listState.animateScrollToItem(messages.size - 1) } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(horizontal = 14.dp)) {
            TopBar(
                title = target.title,
                subtitle = if (target.isGroup) "group chat" else presenceText(target.other),
                onBack = onBack,
            )
        }
        error?.let {
            Text(it, color = Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 14.dp))
        }
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            state = listState,
        ) {
            items(messages) { m ->
                val mine = m.senderId == myId
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (mine) Violet else Surface1)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Column {
                            if (target.isGroup && !mine) {
                                Text("member", color = Cyan, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(m.content, color = TextMain)
                            Text(shortTime(m.createdAt), color = TextDim, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Message") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty()) {
                        text = ""
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { repo.sendMessage(target.conversationId, t) }
                                messages = withContext(Dispatchers.IO) { repo.messages(target.conversationId) }
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan),
            ) { Text("Send") }
        }
    }
}

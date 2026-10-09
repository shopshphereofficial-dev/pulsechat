package com.pulsechat.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsechat.app.data.FriendRequest
import com.pulsechat.app.data.Profile
import com.pulsechat.app.data.Repo
import com.pulsechat.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FriendsScreen(repo: Repo, onOpenChat: (ChatTarget) -> Unit) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var requests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var friends by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var searched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            try {
                requests = withContext(Dispatchers.IO) { repo.incomingRequests() }
                friends = withContext(Dispatchers.IO) { repo.friends() }
                suggestions = withContext(Dispatchers.IO) { repo.allUsers() }
                error = null
            } catch (e: Exception) { error = e.message }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    fun messageUser(p: Profile) {
        scope.launch {
            try {
                val cid = withContext(Dispatchers.IO) { repo.openDirect(p.id) }
                onOpenChat(ChatTarget(cid, p.handle, p, false))
            } catch (e: Exception) { error = e.message }
        }
    }

    fun addFriend(p: Profile) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { repo.sendFriendRequest(p.id) }
                note = "Request sent to ${p.handle}"
                reload()
            } catch (e: Exception) { error = "Request bhejne mein dikkat (already bhej diya?)" }
        }
    }

    val friendIds = friends.map { it.id }.toSet()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(24.dp))
        TopBar("Friends", subtitle = "Search people by @username")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Username") },
                prefix = { Text("@", color = c.primary) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        try {
                            results = withContext(Dispatchers.IO) { repo.searchUsers(query) }
                            searched = true; error = null
                        } catch (e: Exception) { error = e.message }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent),
            ) { Text("Search") }
        }
        note?.let { Spacer(Modifier.height(6.dp)); Text(it, color = c.ok, style = MaterialTheme.typography.bodySmall) }
        error?.let { Spacer(Modifier.height(6.dp)); Text(it, color = c.danger, style = MaterialTheme.typography.bodySmall) }

        if (searched) {
            SectionHeader("Search results")
            if (results.isEmpty()) Text("Koi user nahi mila.", color = c.dim, style = MaterialTheme.typography.bodySmall)
            else results.forEach { p -> PersonRow(p, if (friendIds.contains(p.id)) "Chat" else "Add", { if (friendIds.contains(p.id)) messageUser(p) else addFriend(p) }, if (friendIds.contains(p.id)) null else { { messageUser(p) } }) }
        }

        SectionHeader("Requests")
        if (requests.isEmpty()) Text("No pending requests", color = c.dim, style = MaterialTheme.typography.bodySmall)
        else requests.forEach { req ->
            RowItem {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(req.profile, size = 44, showOnline = true)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(req.profile.handle, color = c.text, fontWeight = FontWeight.Medium)
                        Text(presenceText(req.profile), color = c.dim, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        scope.launch { try { withContext(Dispatchers.IO) { repo.acceptRequest(req.friendship.id) }; reload() } catch (e: Exception) { error = e.message } }
                    }) { Text("Accept", color = c.ok) }
                    TextButton(onClick = {
                        scope.launch { try { withContext(Dispatchers.IO) { repo.removeFriendship(req.friendship.id) }; reload() } catch (e: Exception) { error = e.message } }
                    }) { Text("No", color = c.danger) }
                }
            }
        }

        SectionHeader("My friends (${friends.size})")
        if (loading) CircularProgressIndicator()
        else if (friends.isEmpty()) Text("No friends yet — search a username above", color = c.dim, style = MaterialTheme.typography.bodySmall)
        else friends.forEach { p -> PersonRow(p, "Message", { messageUser(p) }, null) }

        if (!searched) {
            val others = suggestions.filter { !friendIds.contains(it.id) }
            if (others.isNotEmpty()) {
                SectionHeader("People on PulseChat")
                others.take(25).forEach { p -> PersonRow(p, "Add", { addFriend(p) }, { messageUser(p) }) }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun PersonRow(p: Profile, primary: String, onPrimary: () -> Unit, onMessage: (() -> Unit)?) {
    val c = AppTheme.colors
    RowItem {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p, size = 44, showOnline = true)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.handle, color = c.text, fontWeight = FontWeight.Medium)
                Text(presenceText(p), color = c.dim, style = MaterialTheme.typography.bodySmall)
            }
            if (onMessage != null) TextButton(onClick = onMessage) { Text("Chat", color = c.primary) }
            Button(onClick = onPrimary, colors = ButtonDefaults.buttonColors(containerColor = c.ok)) { Text(primary) }
        }
    }
}

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsechat.app.data.FriendRequest
import com.pulsechat.app.data.Profile
import com.pulsechat.app.data.Repo
import com.pulsechat.app.ui.theme.Cyan
import com.pulsechat.app.ui.theme.Green
import com.pulsechat.app.ui.theme.Red
import com.pulsechat.app.ui.theme.Surface1
import com.pulsechat.app.ui.theme.TextDim
import com.pulsechat.app.ui.theme.TextMain
import com.pulsechat.app.ui.theme.Violet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FriendsScreen(repo: Repo, onOpenChat: (ChatTarget) -> Unit) {
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
                val r = withContext(Dispatchers.IO) { repo.incomingRequests() }
                val f = withContext(Dispatchers.IO) { repo.friends() }
                val s = withContext(Dispatchers.IO) { repo.allUsers() }
                requests = r
                friends = f
                suggestions = s
                error = null
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    fun doSearch() {
        scope.launch {
            try {
                results = withContext(Dispatchers.IO) { repo.searchUsers(query) }
                searched = true
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun messageUser(p: Profile) {
        scope.launch {
            try {
                val cid = withContext(Dispatchers.IO) { repo.openDirect(p.id) }
                onOpenChat(ChatTarget(cid, p.handle, p, false))
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun addFriend(p: Profile) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { repo.sendFriendRequest(p.id) }
                note = "Request sent to ${p.handle}"
                results = results.filter { it.id != p.id }
            } catch (e: Exception) {
                error = "Request already sent or failed"
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Spacer(Modifier.height(28.dp))
        TopBar("Friends", subtitle = "Search people by @username")
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Username") },
                prefix = { Text("@", color = Cyan) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { doSearch() }, colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
                Text("Search")
            }
        }
        note?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = Green, style = MaterialTheme.typography.bodySmall)
        }
        error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = Red, style = MaterialTheme.typography.bodySmall)
        }

        if (searched) {
            Spacer(Modifier.height(14.dp))
            Text("Search results", color = TextMain, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            if (results.isEmpty()) {
                Text("No user found with that username.", color = TextDim, style = MaterialTheme.typography.bodySmall)
            } else {
                results.forEach { p -> PersonRow(p, primary = "Add", onPrimary = { addFriend(p) }, onMessage = { messageUser(p) }) }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Requests", color = TextMain, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        if (requests.isEmpty()) {
            Text("No pending requests", color = TextDim, style = MaterialTheme.typography.bodySmall)
        } else {
            requests.forEach { req ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp)).background(Surface1).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(req.profile, size = 42)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(req.profile.handle, color = TextMain)
                        Text(presenceText(req.profile), color = TextDim, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            try { withContext(Dispatchers.IO) { repo.acceptRequest(req.friendship.id) }; reload() }
                            catch (e: Exception) { error = e.message }
                        }
                    }) { Text("Accept", color = Green) }
                    TextButton(onClick = {
                        scope.launch {
                            try { withContext(Dispatchers.IO) { repo.removeFriendship(req.friendship.id) }; reload() }
                            catch (e: Exception) { error = e.message }
                        }
                    }) { Text("No", color = Red) }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("My friends (${friends.size})", color = TextMain, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        if (loading) {
            CircularProgressIndicator()
        } else if (friends.isEmpty()) {
            Text("No friends yet — search a username above", color = TextDim, style = MaterialTheme.typography.bodySmall)
        } else {
            friends.forEach { p -> PersonRow(p, primary = "Message", onPrimary = { messageUser(p) }, onMessage = null) }
        }

        if (!searched && suggestions.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("People on PulseChat", color = TextMain, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            suggestions.take(25).forEach { p ->
                PersonRow(p, primary = "Add", onPrimary = { addFriend(p) }, onMessage = { messageUser(p) })
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun PersonRow(
    p: Profile,
    primary: String,
    onPrimary: () -> Unit,
    onMessage: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp)).background(Surface1).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(p, size = 42, showOnline = true)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(p.handle, color = TextMain, fontWeight = FontWeight.Medium)
            Text(presenceText(p), color = TextDim, style = MaterialTheme.typography.bodySmall)
        }
        if (onMessage != null) {
            TextButton(onClick = onMessage) { Text("Chat", color = Cyan) }
        }
        Button(
            onClick = onPrimary,
            colors = ButtonDefaults.buttonColors(containerColor = Green),
        ) { Text(primary) }
    }
}

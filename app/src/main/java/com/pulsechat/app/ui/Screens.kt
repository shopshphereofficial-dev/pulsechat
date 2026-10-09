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
fun UsernameScreen(busy: Boolean, error: String?, onSubmit: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Choose your username", style = MaterialTheme.typography.headlineMedium, color = Cyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Friends will find you by this. It must be unique.", color = TextDim)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(24) },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(name.trim()) },
            enabled = !busy && name.trim().length >= 3,
            colors = ButtonDefaults.buttonColors(containerColor = Green),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
        if (busy) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = Red, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun HomeScreen(repo: Repo, profile: Profile?, onSignOut: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.CHATS) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.CHATS -> ChatsScreen()
                Tab.FRIENDS -> FriendsScreen(repo)
                Tab.PROFILE -> ProfileScreen(profile, onSignOut)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(Surface1).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val items = listOf(Tab.CHATS to "Chats", Tab.FRIENDS to "Friends", Tab.PROFILE to "Profile")
            items.forEach { (t, label) ->
                Text(
                    label,
                    color = if (tab == t) Cyan else TextDim,
                    fontWeight = if (tab == t) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { tab = t }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
fun ChatsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Chats", style = MaterialTheme.typography.headlineMedium, color = Cyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "1-on-1 and group chat is coming in the next step.\nAdd some friends first!",
            color = TextDim,
        )
    }
}

@Composable
fun ProfileScreen(profile: Profile?, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Text("Profile", style = MaterialTheme.typography.headlineMedium, color = Cyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("@${profile?.username ?: "-"}", color = TextMain, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(profile?.displayName ?: "", color = TextDim)
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = Red),
        ) { Text("Sign out") }
    }
}

@Composable
fun FriendsScreen(repo: Repo) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var requests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var friends by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }

    fun reload() {
        loading = true
        scope.launch {
            try {
                val r = withContext(Dispatchers.IO) { repo.incomingRequests() }
                val f = withContext(Dispatchers.IO) { repo.friends() }
                requests = r
                friends = f
                error = null
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Spacer(Modifier.height(30.dp))
        Text("Friends", style = MaterialTheme.typography.headlineMedium, color = Cyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search by username") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        try {
                            results = withContext(Dispatchers.IO) { repo.searchUsers(query) }
                            error = null
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
            ) { Text("Search") }
        }

        note?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Green, style = MaterialTheme.typography.bodySmall)
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Red, style = MaterialTheme.typography.bodySmall)
        }

        if (results.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Search results", color = TextMain, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            results.forEach { p ->
                UserRow(p) {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { repo.sendFriendRequest(p.id) }
                            note = "Request sent to @${p.username}"
                            results = results.filter { it.id != p.id }
                        } catch (e: Exception) {
                            error = "Could not send request (already sent?)"
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Requests", color = TextMain, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (requests.isEmpty()) {
            Text("No pending requests", color = TextDim, style = MaterialTheme.typography.bodySmall)
        } else {
            requests.forEach { req ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface1)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("@${req.profile.username ?: "-"}", color = TextMain)
                        Text(req.profile.displayName ?: "", color = TextDim, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { repo.acceptRequest(req.friendship.id) }
                                reload()
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    }) { Text("Accept", color = Green) }
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { repo.removeFriendship(req.friendship.id) }
                                reload()
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    }) { Text("Decline", color = Red) }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("My friends (${friends.size})", color = TextMain, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (loading) {
            CircularProgressIndicator()
        } else if (friends.isEmpty()) {
            Text("No friends yet — search a username above", color = TextDim, style = MaterialTheme.typography.bodySmall)
        } else {
            friends.forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface1)
                        .padding(12.dp),
                ) {
                    Column {
                        Text("@${p.username ?: "-"}", color = TextMain)
                        Text(p.displayName ?: "", color = TextDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun UserRow(p: Profile, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("@${p.username ?: "-"}", color = TextMain)
            Text(p.displayName ?: "", color = TextDim, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = Green),
        ) { Text("Add") }
    }
}

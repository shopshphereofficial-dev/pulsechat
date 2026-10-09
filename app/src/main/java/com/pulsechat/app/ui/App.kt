package com.pulsechat.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsechat.app.data.Api
import com.pulsechat.app.data.GoogleAuth
import com.pulsechat.app.data.Profile
import com.pulsechat.app.data.Repo
import com.pulsechat.app.data.Session
import com.pulsechat.app.ui.theme.Bg
import com.pulsechat.app.ui.theme.Cyan
import com.pulsechat.app.ui.theme.Green
import com.pulsechat.app.ui.theme.Red
import com.pulsechat.app.ui.theme.TextDim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Stage { LOADING, LOGIN, USERNAME, HOME }

@Composable
fun PulseChatApp() {
    val context = LocalContext.current
    val session = remember { Session(context) }
    val repo = remember { Repo(session) }
    val scope = rememberCoroutineScope()

    var stage by remember { mutableStateOf(Stage.LOADING) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<Profile?>(null) }
    var openChat by remember { mutableStateOf<ChatTarget?>(null) }

    LaunchedEffect(Unit) {
        if (session.isLoggedIn()) {
            val ok = withContext(Dispatchers.IO) { repo.refreshSession() }
            if (ok) {
                val p = withContext(Dispatchers.IO) { runCatching { repo.myProfile() }.getOrNull() }
                profile = p
                stage = if (p?.username.isNullOrEmpty()) Stage.USERNAME else Stage.HOME
            } else {
                session.clear()
                stage = Stage.LOGIN
            }
        } else {
            stage = Stage.LOGIN
        }
    }

    fun doLogin() {
        busy = true
        error = null
        scope.launch {
            try {
                val cred = withContext(Dispatchers.Main) { GoogleAuth.signIn(context) }
                val res = withContext(Dispatchers.IO) { Api.signInWithIdToken(cred.idToken) }
                session.accessToken = res.getString("access_token")
                session.refreshToken = res.getString("refresh_token")
                val u = res.optJSONObject("user")
                session.userId = u?.optString("id")
                session.email = u?.optString("email")
                val p = withContext(Dispatchers.IO) { runCatching { repo.myProfile() }.getOrNull() }
                profile = p
                stage = if (p?.username.isNullOrEmpty()) Stage.USERNAME else Stage.HOME
            } catch (e: Exception) {
                error = e.message ?: "Login failed"
            } finally {
                busy = false
            }
        }
    }

    fun claimUsername(name: String) {
        busy = true
        error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { repo.setUsername(name) }
                profile = withContext(Dispatchers.IO) { repo.myProfile() }
                stage = Stage.HOME
            } catch (e: Exception) {
                error = "Ye username already taken hai. Koi aur try karo."
            } finally {
                busy = false
            }
        }
    }

    fun signOut() {
        scope.launch {
            val t = session.accessToken
            withContext(Dispatchers.IO) { if (t != null) Api.signOut(t) }
            session.clear()
            profile = null
            openChat = null
            stage = Stage.LOGIN
        }
    }

    Surface(color = Bg, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            when (stage) {
                Stage.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                Stage.LOGIN -> LoginScreen(busy = busy, error = error, onLogin = { doLogin() })
                Stage.USERNAME -> UsernameScreen(busy = busy, error = error, onSubmit = { claimUsername(it) })
                Stage.HOME -> {
                    LaunchedEffect(Unit) {
                        while (true) {
                            withContext(Dispatchers.IO) { repo.heartbeat() }
                            delay(30000)
                        }
                    }
                    val chat = openChat
                    if (chat != null) {
                        ChatScreen(repo, chat, myId = session.userId ?: "", onBack = { openChat = null })
                    } else {
                        HomeScreen(
                            repo = repo,
                            profile = profile,
                            onOpenChat = { openChat = it },
                            onSignOut = { signOut() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(busy: Boolean, error: String?, onLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("PulseChat", style = MaterialTheme.typography.displaySmall, color = Cyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Chat with friends. Online status, groups, and more.", color = TextDim)
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onLogin,
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = Green),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue with Google") }
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

package com.pulsechat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsechat.app.data.CallInfo
import com.pulsechat.app.data.Profile
import com.pulsechat.app.data.Repo
import com.pulsechat.app.ui.theme.Bg
import com.pulsechat.app.ui.theme.Cyan
import com.pulsechat.app.ui.theme.Green
import com.pulsechat.app.ui.theme.Red
import com.pulsechat.app.ui.theme.Surface1
import com.pulsechat.app.ui.theme.TextDim
import com.pulsechat.app.ui.theme.TextMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun CallScreen(
    repo: Repo,
    call: CallInfo,
    other: Profile?,
    isCaller: Boolean,
    onClose: () -> Unit,
) {
    var status by remember { mutableStateOf(call.status) }
    var seconds by remember { mutableStateOf(0) }
    var tick by remember { mutableStateOf(0) }
    var ended by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            tick++
            delay(1500)
        }
    }
    LaunchedEffect(tick) {
        val c = runCatching { withContext(Dispatchers.IO) { repo.getCall(call.id) } }.getOrNull()
        if (c != null) {
            status = c.status
            if (c.status == "declined" || c.status == "ended") {
                ended = true
                onClose()
            }
        }
    }
    LaunchedEffect(status) {
        if (status == "accepted") {
            seconds = 0
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    val mm = seconds / 60
    val ss = seconds % 60
    val timer = String.format("%02d:%02d", mm, ss)

    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(other, size = 120)
            Spacer(Modifier.height(18.dp))
            Text(other?.handle ?: "Unknown", color = TextMain, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    ended -> "Call ended"
                    status == "accepted" -> timer
                    isCaller -> "Ringing…"
                    else -> "Incoming ${call.kind} call"
                },
                color = if (status == "accepted") Green else TextDim,
            )
            Spacer(Modifier.height(40.dp))

            if (status == "ringing" && !isCaller) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Button(
                        onClick = {
                            repo.setCallStatus(call.id, "accepted")
                            status = "accepted"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                    ) { Text("Accept") }
                    Button(
                        onClick = {
                            repo.setCallStatus(call.id, "declined")
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                    ) { Text("Decline") }
                }
            } else {
                Button(
                    onClick = {
                        repo.setCallStatus(call.id, "ended")
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                    modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                ) { Text("End call") }
            }

            Spacer(Modifier.height(28.dp))
            Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Surface1).padding(14.dp)) {
                Text(
                    "Voice/video ka poora call flow (ring, accept, decline, end) chal raha hai.\n" +
                        "Asli audio/video (WebRTC) agle step mein.",
                    color = TextDim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

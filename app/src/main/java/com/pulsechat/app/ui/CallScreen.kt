package com.pulsechat.app.ui

import android.Manifest
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pulsechat.app.call.CallEngine
import com.pulsechat.app.call.CallState
import com.pulsechat.app.data.CallInfo
import com.pulsechat.app.data.Profile
import com.pulsechat.app.data.Repo
import com.pulsechat.app.ui.theme.AppTheme
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import livekit.org.webrtc.RendererCommon

@Composable
fun CallScreen(
    repo: Repo,
    call: CallInfo,
    conversationId: String,
    other: Profile?,
    isCaller: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val engine = remember { CallEngine(context.applicationContext) }
    val room by engine.room.collectAsState()
    val state by engine.state.collectAsState()
    val micOn by engine.micOn.collectAsState()
    val camOn by engine.camOn.collectAsState()
    val remoteVideo by engine.remoteVideo.collectAsState()
    val localVideo by engine.localVideo.collectAsState()
    val remoteCount by engine.remoteCount.collectAsState()
    val engineError by engine.error.collectAsState()

    val isVideo = call.kind == "video"
    var status by remember { mutableStateOf(call.status) }
    var seconds by remember { mutableStateOf(0) }
    var tick by remember { mutableStateOf(0) }
    var err by remember { mutableStateOf<String?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val need = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (isVideo) need.add(Manifest.permission.CAMERA)
        permLauncher.launch(need.toTypedArray())
    }

    DisposableEffect(Unit) { onDispose { engine.stop() } }

    LaunchedEffect(Unit) { while (true) { tick++; delay(1500) } }
    LaunchedEffect(tick) {
        val cl = runCatching { withContext(Dispatchers.IO) { repo.getCall(call.id) } }.getOrNull()
            ?: return@LaunchedEffect
        if (cl.status != status) status = cl.status
        if (cl.status == "declined" || cl.status == "ended") {
            engine.stop()
            onClose()
        }
    }

    LaunchedEffect(status) {
        if (status == "accepted") {
            if (engine.state.value == CallState.IDLE && conversationId.isNotEmpty()) {
                try {
                    val creds = withContext(Dispatchers.IO) { repo.callToken(conversationId) }
                    engine.start(creds.first, creds.second, isVideo)
                } catch (e: Exception) {
                    err = e.message ?: "Call connect nahi ho paya"
                }
            }
            seconds = 0
            while (true) { delay(1000); seconds++ }
        }
    }

    LaunchedEffect(engineError) { if (engineError != null) err = engineError }

    val timer = String.format("%02d:%02d", seconds / 60, seconds % 60)
    val subtitle = when {
        state == CallState.FAILED -> err ?: "Connection failed"
        status == "accepted" && state == CallState.CONNECTED ->
            if (remoteCount > 0) timer else "Waiting for other side…"
        status == "accepted" && state == CallState.RECONNECTING -> "Reconnecting…"
        status == "accepted" -> "Connecting…"
        isCaller -> "Ringing…"
        else -> "Incoming ${call.kind} call"
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF070A14), Color(0xFF121A2E)))),
    ) {
        if (isVideo && room != null && remoteVideo != null) {
            key(remoteVideo) {
                VideoSurface(room!!, remoteVideo!!, false, Modifier.fillMaxSize())
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Avatar(other, size = 128)
                Spacer(Modifier.height(20.dp))
                Text(
                    other?.handle ?: "Unknown",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    subtitle,
                    color = if (status == "accepted" && state == CallState.CONNECTED) Color(0xFF22C55E) else Color(0xFF9AA6C0),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (isVideo && room != null && localVideo != null && camOn) {
            key(localVideo) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .systemBarsPadding()
                        .padding(14.dp)
                        .size(width = 108.dp, height = 156.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1B2333)),
                ) {
                    VideoSurface(room!!, localVideo!!, true, Modifier.fillMaxSize())
                }
            }
        }

        // top status pill
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                other?.handle ?: "Unknown",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(subtitle, color = Color(0xFF9AA6C0), style = MaterialTheme.typography.bodySmall)
        }

        // bottom controls
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (status == "ringing" && !isCaller) {
                Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                    CallButton(Icons.Filled.CallEnd, Color(0xFFEF4444), "Decline") {
                        repo.setCallStatus(call.id, "declined"); engine.stop(); onClose()
                    }
                    CallButton(Icons.Filled.Mic, Color(0xFF22C55E), "Accept") {
                        repo.setCallStatus(call.id, "accepted"); status = "accepted"
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (status == "accepted") {
                        CallButton(
                            if (micOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                            if (micOn) Color(0xFF2A3350) else Color.White,
                            "Mic",
                        ) { engine.toggleMic() }
                        if (isVideo) {
                            CallButton(
                                if (camOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                                if (camOn) Color(0xFF2A3350) else Color.White,
                                "Camera",
                            ) { engine.toggleCamera() }
                            if (camOn) {
                                CallButton(Icons.Filled.Cameraswitch, Color(0xFF2A3350), "Flip") {
                                    engine.flipCamera()
                                }
                            }
                        }
                    }
                    CallButton(Icons.Filled.CallEnd, Color(0xFFEF4444), "End") {
                        repo.setCallStatus(call.id, "ended"); engine.stop(); onClose()
                    }
                }
            }
        }
    }
}

@Composable
private fun CallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(bg)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color(0xFF9AA6C0), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun VideoSurface(room: Room, track: VideoTrack, mirror: Boolean, modifier: Modifier) {
    var view: TextureViewRenderer? by remember { mutableStateOf(null) }
    DisposableEffect(track) {
        onDispose { view?.let { v -> runCatching { track.removeRenderer(v) } } }
    }
    AndroidView(
        factory = { ctx ->
            TextureViewRenderer(ctx).apply {
                room.initVideoRenderer(this)
                setMirror(mirror)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                track.addRenderer(this)
                view = this
            }
        },
        modifier = modifier,
    )
}

package com.pulsechat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsechat.app.data.Profile
import com.pulsechat.app.ui.theme.Cyan
import com.pulsechat.app.ui.theme.Green
import com.pulsechat.app.ui.theme.Surface1
import com.pulsechat.app.ui.theme.TextDim
import com.pulsechat.app.ui.theme.TextMain
import com.pulsechat.app.ui.theme.Violet

data class ChatTarget(
    val conversationId: String,
    val title: String,
    val other: Profile?,
    val isGroup: Boolean,
)

@Composable
fun Avatar(profile: Profile?, size: Int = 48, showOnline: Boolean = false) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Violet, Cyan))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profile?.initial ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.4).sp,
            )
        }
        if (showOnline && profile?.isOnline() == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.3).dp)
                    .clip(CircleShape)
                    .background(Green),
            )
        }
    }
}

@Composable
fun Card2(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .padding(14.dp),
    ) { content() }
}

@Composable
fun TopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("Back", color = Cyan) }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = TextMain,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            subtitle?.let {
                Text(it, color = TextDim, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        trailing?.invoke()
    }
}

fun shortTime(iso: String?): String {
    if (iso == null || iso.length < 16) return ""
    return try { iso.substring(11, 16) } catch (e: Exception) { "" }
}

fun presenceText(p: Profile?): String {
    if (p == null) return ""
    if (p.isOnline()) return "online"
    if (p.lastSeenMs <= 0) return ""
    val diff = System.currentTimeMillis() - p.lastSeenMs
    val min = diff / 60000
    return when {
        min < 1 -> "last seen just now"
        min < 60 -> "last seen $min min ago"
        min < 1440 -> "last seen ${min / 60} h ago"
        else -> "last seen ${min / 1440} d ago"
    }
}

package com.pulsechat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.pulsechat.app.ui.theme.AppTheme

data class ChatTarget(
    val conversationId: String,
    val title: String,
    val other: Profile?,
    val isGroup: Boolean,
)

@Composable
fun Avatar(profile: Profile?, size: Int = 48, showOnline: Boolean = false) {
    val c = AppTheme.colors
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(c.accent, c.primary))),
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
                    .background(c.ok)
                    .border(2.dp, c.bg, CircleShape),
            )
        }
    }
}

@Composable
fun Card2(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface)
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
    val c = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = c.text)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            subtitle?.let { Text(it, color = c.dim, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionHeader(text: String) {
    val c = AppTheme.colors
    Text(
        text.uppercase(),
        color = c.dim,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
fun RowItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
    ) { content() }
}

fun shortTime(iso: String?): String {
    if (iso == null || iso.length < 16) return ""
    return try { iso.substring(11, 16) } catch (e: Exception) { "" }
}

fun dayLabel(iso: String?): String {
    if (iso == null || iso.length < 10) return ""
    return try { iso.substring(0, 10) } catch (e: Exception) { "" }
}

fun presenceText(p: Profile?): String {
    if (p == null) return ""
    if (p.isOnline()) return "online"
    if (p.lastSeenMs <= 0) return ""
    val min = (System.currentTimeMillis() - p.lastSeenMs) / 60000
    return when {
        min < 1 -> "last seen just now"
        min < 60 -> "last seen $min min ago"
        min < 1440 -> "last seen ${min / 60} h ago"
        else -> "last seen ${min / 1440} d ago"
    }
}

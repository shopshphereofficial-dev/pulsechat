package com.pulsechat.app.data

import org.json.JSONObject

data class Profile(
    val id: String,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val lastSeenMs: Long,
) {
    val handle: String get() = "@" + (username ?: "user")
    val label: String get() = displayName?.takeIf { it.isNotBlank() } ?: username ?: "user"
    val initial: String get() = label.trim().take(1).uppercase()

    fun isOnline(): Boolean = lastSeenMs > 0 && System.currentTimeMillis() - lastSeenMs < 70_000

    companion object {
        fun from(o: JSONObject): Profile = Profile(
            id = o.optString("id"),
            username = o.optString("username").ifEmpty { null },
            displayName = o.optString("display_name").ifEmpty { null },
            avatarUrl = o.optString("avatar_url").ifEmpty { null },
            lastSeenMs = o.optLong("last_seen_ms", 0L),
        )
    }
}

data class Friendship(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: String,
) {
    companion object {
        fun from(o: JSONObject): Friendship = Friendship(
            o.optString("id"),
            o.optString("requester_id"),
            o.optString("addressee_id"),
            o.optString("status"),
        )
    }
}

data class FriendRequest(val friendship: Friendship, val profile: Profile)

data class Conversation(
    val id: String,
    val isGroup: Boolean,
    val title: String?,
    val createdBy: String,
) {
    companion object {
        fun from(o: JSONObject): Conversation = Conversation(
            o.optString("id"),
            o.optBoolean("is_group", false),
            o.optString("title").ifEmpty { null },
            o.optString("created_by"),
        )
    }
}

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val createdAt: String,
) {
    companion object {
        fun from(o: JSONObject): Message = Message(
            o.optString("id"),
            o.optString("conversation_id"),
            o.optString("sender_id"),
            o.optString("content"),
            o.optString("created_at"),
        )
    }
}

data class ChatSummary(
    val conversation: Conversation,
    val title: String,
    val lastMessage: String?,
    val lastAt: String?,
    val other: Profile?,
)

package com.pulsechat.app.data

import org.json.JSONObject

/** Android's JSONObject.optString returns the literal string "null" for JSON null — this avoids that. */
internal fun jstr(o: JSONObject, key: String): String? {
    if (o.isNull(key)) return null
    val v = o.optString(key, "")
    return if (v.isEmpty() || v == "null") null else v
}

data class Profile(
    val id: String,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val lastSeenMs: Long,
    val bio: String?,
    val status: String?,
) {
    val handle: String get() = "@" + (username ?: "user")
    val label: String get() = displayName?.takeIf { it.isNotBlank() } ?: username ?: "PulseChat user"
    val initial: String get() = label.trim().take(1).uppercase()

    fun isOnline(): Boolean = lastSeenMs > 0 && System.currentTimeMillis() - lastSeenMs < 70_000

    companion object {
        fun from(o: JSONObject): Profile = Profile(
            id = o.optString("id"),
            username = jstr(o, "username"),
            displayName = jstr(o, "display_name"),
            avatarUrl = jstr(o, "avatar_url"),
            lastSeenMs = o.optLong("last_seen_ms", 0L),
            bio = jstr(o, "bio"),
            status = jstr(o, "status"),
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
            jstr(o, "title"),
            o.optString("created_by"),
        )
    }
}

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String?,
    val createdAt: String,
    val replyTo: String?,
    val mediaUrl: String?,
    val mediaType: String?,
    val mediaName: String?,
    val editedAt: String?,
    val reaction: String?,
    val pinned: Boolean,
) {
    val hasMedia: Boolean get() = !mediaUrl.isNullOrEmpty()
    val isImage: Boolean get() = mediaType?.startsWith("image") == true
    val isVideo: Boolean get() = mediaType?.startsWith("video") == true

    companion object {
        fun from(o: JSONObject): Message = Message(
            o.optString("id"),
            o.optString("conversation_id"),
            o.optString("sender_id"),
            jstr(o, "content"),
            o.optString("created_at"),
            jstr(o, "reply_to"),
            jstr(o, "media_url"),
            jstr(o, "media_type"),
            jstr(o, "media_name"),
            jstr(o, "edited_at"),
            jstr(o, "reaction"),
            o.optBoolean("pinned", false),
        )
    }
}

data class ChatSummary(
    val conversation: Conversation,
    val title: String,
    val lastMessage: String?,
    val lastAt: String?,
    val other: Profile?,
    val unread: Int,
)

data class CallInfo(
    val id: String,
    val callerId: String,
    val calleeId: String,
    val kind: String,
    val status: String,
) {
    companion object {
        fun from(o: JSONObject): CallInfo = CallInfo(
            o.optString("id"),
            o.optString("caller_id"),
            o.optString("callee_id"),
            o.optString("kind"),
            o.optString("status"),
        )
    }
}

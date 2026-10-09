package com.pulsechat.app.data

import org.json.JSONObject

data class Profile(
    val id: String,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?,
) {
    companion object {
        fun from(o: JSONObject): Profile = Profile(
            id = o.optString("id"),
            username = o.optString("username").ifEmpty { null },
            displayName = o.optString("display_name").ifEmpty { null },
            avatarUrl = o.optString("avatar_url").ifEmpty { null },
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
            id = o.optString("id"),
            requesterId = o.optString("requester_id"),
            addresseeId = o.optString("addressee_id"),
            status = o.optString("status"),
        )
    }
}

data class FriendRequest(val friendship: Friendship, val profile: Profile)

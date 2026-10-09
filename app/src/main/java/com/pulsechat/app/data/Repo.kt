package com.pulsechat.app.data

import org.json.JSONArray
import org.json.JSONObject

class Repo(private val session: Session) {

    private fun token(): String = session.accessToken ?: throw RuntimeException("Not logged in")
    private fun uid(): String = session.userId ?: throw RuntimeException("Not logged in")

    fun refreshSession(): Boolean {
        val rt = session.refreshToken ?: return false
        return try {
            val o = Api.refresh(rt)
            session.accessToken = o.getString("access_token")
            session.refreshToken = o.getString("refresh_token")
            o.optJSONObject("user")?.let { u ->
                session.userId = u.optString("id").ifEmpty { session.userId }
                session.email = u.optString("email").ifEmpty { session.email }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun myProfile(): Profile? {
        val text = Api.get("profiles?id=eq.${uid()}&select=*", token())
        val arr = JSONArray(text)
        return if (arr.length() > 0) Profile.from(arr.getJSONObject(0)) else null
    }

    fun setUsername(username: String) {
        Api.patch("profiles?id=eq.${uid()}", token(), JSONObject().put("username", username))
    }

    fun searchUsers(query: String): List<Profile> {
        // keep only characters safe for a PostgREST "ilike" pattern
        val q = query.trim()
            .filter { it.isLetterOrDigit() || it == '_' || it == '.' || it == '-' }
            .take(30)
        if (q.isEmpty()) return emptyList()
        val text = Api.get("profiles?username=ilike.*$q*&select=*&limit=25", token())
        val arr = JSONArray(text)
        val out = ArrayList<Profile>()
        for (i in 0 until arr.length()) {
            val p = Profile.from(arr.getJSONObject(i))
            if (p.id != uid() && !p.username.isNullOrEmpty()) out.add(p)
        }
        return out
    }

    fun sendFriendRequest(targetId: String) {
        Api.post(
            "friendships", token(),
            JSONObject()
                .put("requester_id", uid())
                .put("addressee_id", targetId)
                .put("status", "pending"),
        )
    }

    private fun profilesByIds(ids: List<String>): Map<String, Profile> {
        if (ids.isEmpty()) return emptyMap()
        val inList = ids.joinToString(",")
        val text = Api.get("profiles?id=in.($inList)&select=*", token())
        val arr = JSONArray(text)
        val map = HashMap<String, Profile>()
        for (i in 0 until arr.length()) {
            val p = Profile.from(arr.getJSONObject(i))
            map[p.id] = p
        }
        return map
    }

    fun incomingRequests(): List<FriendRequest> {
        val text = Api.get(
            "friendships?addressee_id=eq.${uid()}&status=eq.pending&select=*",
            token(),
        )
        val arr = JSONArray(text)
        val list = ArrayList<Friendship>()
        for (i in 0 until arr.length()) list.add(Friendship.from(arr.getJSONObject(i)))
        val map = profilesByIds(list.map { it.requesterId })
        return list.mapNotNull { f -> map[f.requesterId]?.let { FriendRequest(f, it) } }
    }

    fun friends(): List<Profile> {
        val me = uid()
        val text = Api.get(
            "friendships?status=eq.accepted&or=(requester_id.eq.$me,addressee_id.eq.$me)&select=*",
            token(),
        )
        val arr = JSONArray(text)
        val otherIds = ArrayList<String>()
        for (i in 0 until arr.length()) {
            val f = Friendship.from(arr.getJSONObject(i))
            otherIds.add(if (f.requesterId == me) f.addresseeId else f.requesterId)
        }
        val map = profilesByIds(otherIds)
        return otherIds.mapNotNull { map[it] }
    }

    fun acceptRequest(friendshipId: String) {
        Api.patch("friendships?id=eq.$friendshipId", token(), JSONObject().put("status", "accepted"))
    }

    fun removeFriendship(friendshipId: String) {
        Api.delete("friendships?id=eq.$friendshipId", token())
    }
}

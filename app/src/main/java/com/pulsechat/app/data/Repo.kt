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
        val arr = JSONArray(Api.get("profiles?id=eq.${uid()}&select=*", token()))
        return if (arr.length() > 0) Profile.from(arr.getJSONObject(0)) else null
    }

    fun setUsername(username: String) {
        Api.patch("profiles?id=eq.${uid()}", token(), JSONObject().put("username", username))
    }

    /** marks me as online (WhatsApp-style presence) */
    fun heartbeat() {
        try {
            Api.patch(
                "profiles?id=eq.${uid()}", token(),
                JSONObject().put("last_seen_ms", System.currentTimeMillis()),
            )
        } catch (_: Exception) {
        }
    }

    private fun parseProfiles(text: String): List<Profile> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map { Profile.from(arr.getJSONObject(it)) }
    }

    private fun sanitize(q: String): String =
        q.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(30)

    fun searchUsers(query: String): List<Profile> {
        val q = sanitize(query)
        if (q.isEmpty()) return emptyList()
        val me = uid()
        val map = LinkedHashMap<String, Profile>()
        try {
            parseProfiles(Api.get("profiles?username=ilike.*$q*&select=*&limit=20", token()))
                .forEach { if (it.id != me && !it.username.isNullOrEmpty()) map[it.id] = it }
        } catch (_: Exception) {
        }
        try {
            parseProfiles(Api.get("profiles?display_name=ilike.*$q*&select=*&limit=20", token()))
                .forEach { if (it.id != me && !it.username.isNullOrEmpty()) map[it.id] = it }
        } catch (_: Exception) {
        }
        return map.values.toList()
    }

    fun allUsers(): List<Profile> {
        val me = uid()
        return try {
            parseProfiles(Api.get("profiles?select=*&order=last_seen_ms.desc&limit=60", token()))
                .filter { it.id != me && !it.username.isNullOrEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
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
        val arr = JSONArray(Api.get("profiles?id=in.($inList)&select=*", token()))
        val map = HashMap<String, Profile>()
        for (i in 0 until arr.length()) {
            val p = Profile.from(arr.getJSONObject(i))
            map[p.id] = p
        }
        return map
    }

    fun incomingRequests(): List<FriendRequest> {
        val arr = JSONArray(
            Api.get("friendships?addressee_id=eq.${uid()}&status=eq.pending&select=*", token())
        )
        val list = (0 until arr.length()).map { Friendship.from(arr.getJSONObject(it)) }
        val map = profilesByIds(list.map { it.requesterId })
        return list.mapNotNull { f -> map[f.requesterId]?.let { FriendRequest(f, it) } }
    }

    fun friends(): List<Profile> {
        val me = uid()
        val arr = JSONArray(
            Api.get("friendships?status=eq.accepted&or=(requester_id.eq.$me,addressee_id.eq.$me)&select=*", token())
        )
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

    // ---------------- chat ----------------

    fun myConversations(): List<ChatSummary> {
        val me = uid()
        val memberRows = JSONArray(
            Api.get("conversation_members?user_id=eq.$me&select=conversation_id", token())
        )
        val convIds = (0 until memberRows.length())
            .map { memberRows.getJSONObject(it).getString("conversation_id") }
        if (convIds.isEmpty()) return emptyList()
        val inList = convIds.joinToString(",")

        val convArr = JSONArray(Api.get("conversations?id=in.($inList)&select=*", token()))
        val convs = (0 until convArr.length()).map { Conversation.from(convArr.getJSONObject(it)) }

        val allMembers = JSONArray(
            Api.get("conversation_members?conversation_id=in.($inList)&select=conversation_id,user_id", token())
        )
        val membersByConv = HashMap<String, MutableList<String>>()
        for (i in 0 until allMembers.length()) {
            val o = allMembers.getJSONObject(i)
            membersByConv.getOrPut(o.getString("conversation_id")) { mutableListOf() }
                .add(o.getString("user_id"))
        }

        val lastByConv = HashMap<String, JSONObject>()
        try {
            val msgs = JSONArray(
                Api.get(
                    "messages?conversation_id=in.($inList)&select=conversation_id,content,created_at,sender_id" +
                        "&order=created_at.desc&limit=300", token()
                )
            )
            for (i in 0 until msgs.length()) {
                val o = msgs.getJSONObject(i)
                val cid = o.getString("conversation_id")
                if (!lastByConv.containsKey(cid)) lastByConv[cid] = o
            }
        } catch (_: Exception) {
        }

        val profMap = profilesByIds(membersByConv.values.flatten().distinct())

        return convs.map { c ->
            val members = membersByConv[c.id] ?: emptyList()
            val otherId = members.firstOrNull { it != me }
            val other = otherId?.let { profMap[it] }
            val last = lastByConv[c.id]
            ChatSummary(
                conversation = c,
                title = if (c.isGroup) (c.title ?: "Group") else (other?.handle ?: "@user"),
                lastMessage = last?.optString("content"),
                lastAt = last?.optString("created_at"),
                other = other,
            )
        }.sortedByDescending { it.lastAt ?: "" }
    }

    fun messages(conversationId: String): List<Message> {
        val arr = JSONArray(
            Api.get("messages?conversation_id=eq.$conversationId&select=*&order=created_at.asc&limit=300", token())
        )
        return (0 until arr.length()).map { Message.from(arr.getJSONObject(it)) }
    }

    fun sendMessage(conversationId: String, content: String) {
        Api.post(
            "messages", token(),
            JSONObject()
                .put("conversation_id", conversationId)
                .put("sender_id", uid())
                .put("content", content),
        )
    }

    fun memberProfiles(conversationId: String): List<Profile> {
        val arr = JSONArray(
            Api.get("conversation_members?conversation_id=eq.$conversationId&select=user_id", token())
        )
        val ids = (0 until arr.length()).map { arr.getJSONObject(it).getString("user_id") }
        return profilesByIds(ids).values.toList()
    }

    fun openDirect(otherId: String): String {
        val me = uid()
        for (s in myConversations()) {
            if (!s.conversation.isGroup && s.other?.id == otherId) return s.conversation.id
        }
        val created = JSONArray(
            Api.post("conversations", token(), JSONObject().put("is_group", false).put("created_by", me))
        )
        val cid = created.getJSONObject(0).getString("id")
        addMember(cid, me)
        addMember(cid, otherId)
        return cid
    }

    fun createGroup(title: String, memberIds: List<String>): String {
        val me = uid()
        val created = JSONArray(
            Api.post(
                "conversations", token(),
                JSONObject().put("is_group", true).put("title", title).put("created_by", me),
            )
        )
        val cid = created.getJSONObject(0).getString("id")
        addMember(cid, me)
        memberIds.forEach { addMember(cid, it) }
        return cid
    }

    private fun addMember(conversationId: String, userId: String) {
        Api.post(
            "conversation_members", token(),
            JSONObject().put("conversation_id", conversationId).put("user_id", userId),
        )
    }
}

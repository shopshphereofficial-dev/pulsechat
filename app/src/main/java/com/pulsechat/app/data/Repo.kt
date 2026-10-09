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

    fun updateProfile(displayName: String?, bio: String?, status: String?) {
        val body = JSONObject()
        if (displayName != null) body.put("display_name", displayName)
        if (bio != null) body.put("bio", bio)
        if (status != null) body.put("status", status)
        Api.patch("profiles?id=eq.${uid()}", token(), body)
    }

    /** uploads an image/file to Storage and returns its public URL */
    fun uploadMedia(bytes: ByteArray, ext: String, mime: String): String {
        val path = "${uid()}/${System.currentTimeMillis()}_${(0..9999).random()}.$ext"
        return Api.upload(path, token(), bytes, mime)
    }

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

    /** If they already sent us a request, accept it instead of creating a duplicate row. */
    fun sendFriendRequest(targetId: String) {
        val me = uid()
        val existing = JSONArray(
            Api.get("friendships?select=*&or=(and(requester_id.eq.$me,addressee_id.eq.$targetId),and(requester_id.eq.$targetId,addressee_id.eq.$me))", token())
        )
        if (existing.length() > 0) {
            val f = Friendship.from(existing.getJSONObject(0))
            if (f.status == "pending" && f.addresseeId == me) {
                acceptRequest(f.id)
                return
            }
            return // already friends or already requested
        }
        Api.post(
            "friendships", token(),
            JSONObject()
                .put("requester_id", me)
                .put("addressee_id", targetId)
                .put("status", "pending"),
        )
    }

    private fun profilesByIds(ids: List<String>): Map<String, Profile> {
        if (ids.isEmpty()) return emptyMap()
        val inList = ids.distinct().joinToString(",")
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
        val seen = HashSet<String>()
        val list = (0 until arr.length())
            .map { Friendship.from(arr.getJSONObject(it)) }
            .filter { seen.add(it.requesterId) }
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
        return otherIds.distinct().mapNotNull { map[it] }
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
            Api.get(
                "conversation_members?user_id=eq.$me&select=conversation_id,last_read_at,pinned,archived,muted",
                token()
            )
        )
        val myRead = HashMap<String, String>()
        val myPinned = HashMap<String, Boolean>()
        val myArchived = HashMap<String, Boolean>()
        val myMuted = HashMap<String, Boolean>()
        val convIds = (0 until memberRows.length()).map {
            val o = memberRows.getJSONObject(it)
            val cid = o.getString("conversation_id")
            myRead[cid] = o.optString("last_read_at", "")
            myPinned[cid] = o.optBoolean("pinned", false)
            myArchived[cid] = o.optBoolean("archived", false)
            myMuted[cid] = o.optBoolean("muted", false)
            cid
        }.distinct()
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
        val unread = HashMap<String, Int>()
        try {
            val msgs = JSONArray(
                Api.get(
                    "messages?conversation_id=in.($inList)&select=conversation_id,content,created_at,sender_id,media_type" +
                        "&order=created_at.desc&limit=500", token()
                )
            )
            for (i in 0 until msgs.length()) {
                val o = msgs.getJSONObject(i)
                val cid = o.getString("conversation_id")
                if (!lastByConv.containsKey(cid)) lastByConv[cid] = o
                val sender = o.optString("sender_id")
                val created = o.optString("created_at")
                val read = myRead[cid] ?: ""
                if (sender != me && created > read) unread[cid] = (unread[cid] ?: 0) + 1
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
                lastMessage = last?.let { r ->
                    val ct = jstr(r, "content")
                    val mt = jstr(r, "media_type")
                    when {
                        ct != null && ct.isNotBlank() -> ct
                        mt != null && mt.startsWith("image") -> "Photo"
                        mt != null && mt.startsWith("video") -> "Video"
                        mt != null -> "Attachment"
                        else -> null
                    }
                },
                lastAt = last?.optString("created_at"),
                other = other,
                unread = unread[c.id] ?: 0,
                pinned = myPinned[c.id] ?: false,
                muted = myMuted[c.id] ?: false,
                archived = myArchived[c.id] ?: false,
            )
        }.sortedWith(compareByDescending<ChatSummary> { it.pinned }.thenByDescending { it.lastAt ?: "" })
    }

    fun messages(conversationId: String): List<Message> {
        val arr = JSONArray(
            Api.get("messages?conversation_id=eq.$conversationId&select=*&order=created_at.asc&limit=300", token())
        )
        return (0 until arr.length()).map { Message.from(arr.getJSONObject(it)) }
    }

    fun sendMessage(conversationId: String, content: String, replyTo: String? = null) {
        val body = JSONObject()
            .put("conversation_id", conversationId)
            .put("sender_id", uid())
            .put("content", content)
        if (replyTo != null) body.put("reply_to", replyTo)
        Api.post("messages", token(), body)
    }

    fun sendMedia(
        conversationId: String,
        mediaUrl: String,
        mediaType: String,
        mediaName: String,
        caption: String? = null,
        replyTo: String? = null,
    ) {
        val body = JSONObject()
            .put("conversation_id", conversationId)
            .put("sender_id", uid())
            .put("media_url", mediaUrl)
            .put("media_type", mediaType)
            .put("media_name", mediaName)
        if (!caption.isNullOrBlank()) body.put("content", caption)
        if (replyTo != null) body.put("reply_to", replyTo)
        Api.post("messages", token(), body)
    }

    fun deleteMessage(id: String) {
        Api.delete("messages?id=eq.$id", token())
    }

    fun memberProfiles(conversationId: String): List<Profile> {
        val arr = JSONArray(
            Api.get("conversation_members?conversation_id=eq.$conversationId&select=user_id", token())
        )
        val ids = (0 until arr.length()).map { arr.getJSONObject(it).getString("user_id") }
        return profilesByIds(ids).values.toList()
    }

    fun openDirect(otherId: String): String {
        val res = Api.rpc("start_direct", token(), JSONObject().put("other", otherId))
        return res.trim().trim('"')
    }

    fun createGroup(title: String, memberIds: List<String>): String {
        val arr = JSONArray()
        memberIds.distinct().forEach { arr.put(it) }
        val res = Api.rpc(
            "create_group", token(),
            JSONObject().put("grp_title", title).put("members", arr),
        )
        return res.trim().trim('"')
    }

    fun usernameTaken(name: String): Boolean {
        val res = Api.rpc("username_taken", token(), JSONObject().put("name", name.lowercase()))
        return res.trim() == "true"
    }

    private fun addMember(conversationId: String, userId: String) {
        Api.post(
            "conversation_members", token(),
            JSONObject().put("conversation_id", conversationId).put("user_id", userId),
        )
    }

    // ---------------- calls ----------------

    fun startCall(calleeId: String, kind: String): CallInfo {
        val me = uid()
        val res = JSONArray(
            Api.post(
                "calls", token(),
                JSONObject()
                    .put("caller_id", me)
                    .put("callee_id", calleeId)
                    .put("kind", kind)
                    .put("status", "ringing"),
            )
        )
        return CallInfo.from(res.getJSONObject(0))
    }

    fun getCall(id: String): CallInfo? {
        val arr = JSONArray(Api.get("calls?id=eq.$id&select=*", token()))
        return if (arr.length() > 0) CallInfo.from(arr.getJSONObject(0)) else null
    }

    fun incomingCall(): CallInfo? {
        val me = uid()
        val arr = JSONArray(
            Api.get("calls?callee_id=eq.$me&status=eq.ringing&select=*&order=created_at.desc&limit=1", token())
        )
        return if (arr.length() > 0) CallInfo.from(arr.getJSONObject(0)) else null
    }

    fun setCallStatus(id: String, status: String) {
        try {
            Api.patch("calls?id=eq.$id", token(), JSONObject().put("status", status))
        } catch (_: Exception) {
        }
    }

    /** LiveKit call credentials for a conversation (server mints the JWT). */
    fun callToken(conversationId: String): Pair<String, String> {
        val body = org.json.JSONObject().put("p_conversation", conversationId)
        val text = Api.rpc("livekit_call_token", token(), body)
        val o = org.json.JSONObject(text)
        val url = o.optString("url")
        val tok = o.optString("token")
        if (url.isEmpty() || tok.isEmpty()) throw RuntimeException("Call server not ready")
        return Pair(url, tok)
    }

    fun profileById(id: String): Profile? = profilesByIds(listOf(id))[id]

    // ---------------- read / typing ----------------

    fun markRead(conversationId: String) {
        try {
            Api.patch(
                "conversation_members?conversation_id=eq.$conversationId&user_id=eq.${uid()}", token(),
                JSONObject().put("last_read_at", isoNow()),
            )
        } catch (_: Exception) {
        }
    }

    fun setTyping(conversationId: String) {
        try {
            Api.patch(
                "conversation_members?conversation_id=eq.$conversationId&user_id=eq.${uid()}", token(),
                JSONObject().put("typing_at", isoNow()),
            )
        } catch (_: Exception) {
        }
    }

    fun isTyping(conversationId: String, who: String): Boolean {
        return try {
            val arr = JSONArray(
                Api.get("conversation_members?conversation_id=eq.$conversationId&user_id=eq.$who&select=typing_at", token())
            )
            if (arr.length() == 0) return false
            val t = jstr(arr.getJSONObject(0), "typing_at") ?: return false
            if (t.length < 19) return false
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val parsed = fmt.parse(t.substring(0, 19)) ?: return false
            System.currentTimeMillis() - parsed.time < 6000
        } catch (e: Exception) {
            false
        }
    }

    fun otherLastRead(conversationId: String, who: String): String {
        return try {
            val arr = JSONArray(
                Api.get("conversation_members?conversation_id=eq.$conversationId&user_id=eq.$who&select=last_read_at", token())
            )
            if (arr.length() == 0) "" else arr.getJSONObject(0).optString("last_read_at", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun isoNow(): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return fmt.format(java.util.Date())
    }

    // ---------------- message actions ----------------

    fun editMessage(id: String, text: String) {
        Api.patch("messages?id=eq.$id", token(), JSONObject().put("content", text).put("edited_at", isoNow()))
    }

    fun setReaction(id: String, emoji: String?) {
        val body = JSONObject()
        if (emoji == null) body.put("reaction", JSONObject.NULL) else body.put("reaction", emoji)
        Api.patch("messages?id=eq.$id", token(), body)
    }

    fun togglePin(id: String, pinned: Boolean) {
        Api.patch("messages?id=eq.$id", token(), JSONObject().put("pinned", pinned))
    }

    fun forward(conversationId: String, m: Message) {
        val body = JSONObject()
            .put("conversation_id", conversationId)
            .put("sender_id", uid())
        if (m.content != null) body.put("content", m.content)
        if (m.mediaUrl != null) {
            body.put("media_url", m.mediaUrl)
            body.put("media_type", m.mediaType)
            body.put("media_name", m.mediaName)
        }
        Api.post("messages", token(), body)
    }

    fun searchMessages(conversationId: String, q: String): List<Message> {
        val query = q.trim().replace("%", "")
        if (query.isEmpty()) return emptyList()
        val arr = JSONArray(
            Api.get("messages?conversation_id=eq.$conversationId&content=ilike.*$query*&select=*&order=created_at.desc&limit=50", token())
        )
        return (0 until arr.length()).map { Message.from(arr.getJSONObject(it)) }
    }

    // ---------------- blocks ----------------

    fun blockedIds(): Set<String> {
        return try {
            val arr = JSONArray(Api.get("blocks?blocker_id=eq.${uid()}&select=blocked_id", token()))
            (0 until arr.length()).map { arr.getJSONObject(it).getString("blocked_id") }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun block(userId: String) {
        Api.post("blocks", token(), JSONObject().put("blocker_id", uid()).put("blocked_id", userId))
    }

    fun unblock(userId: String) {
        Api.delete("blocks?blocker_id=eq.${uid()}&blocked_id=eq.$userId", token())
    }

    // ---------------- groups ----------------

    fun leaveGroup(conversationId: String) {
        Api.delete("conversation_members?conversation_id=eq.$conversationId&user_id=eq.${uid()}", token())
    }

    fun addGroupMember(conversationId: String, userId: String) {
        try {
            Api.post(
                "conversation_members", token(),
                JSONObject().put("conversation_id", conversationId).put("user_id", userId),
            )
        } catch (_: Exception) {
        }
    }

    fun renameGroup(conversationId: String, title: String) {
        Api.patch("conversations?id=eq.$conversationId", token(), JSONObject().put("title", title))
    }

    // ---------------- per-chat settings ----------------

    private fun setMemberFlag(conversationId: String, field: String, value: Boolean) {
        try {
            Api.patch(
                "conversation_members?conversation_id=eq.$conversationId&user_id=eq.${uid()}", token(),
                JSONObject().put(field, value),
            )
        } catch (_: Exception) {
        }
    }

    fun setPinned(conversationId: String, v: Boolean) = setMemberFlag(conversationId, "pinned", v)
    fun setArchived(conversationId: String, v: Boolean) = setMemberFlag(conversationId, "archived", v)
    fun setMuted(conversationId: String, v: Boolean) = setMemberFlag(conversationId, "muted", v)

    fun deleteChatForMe(conversationId: String) {
        try {
            Api.delete("conversation_members?conversation_id=eq.$conversationId&user_id=eq.${uid()}", token())
        } catch (_: Exception) {
        }
    }

    fun clearChatForMe(conversationId: String) {
        // removes my membership; history stays for the other member
        deleteChatForMe(conversationId)
    }

    // ---------------- avatar ----------------

    fun uploadAvatar(bytes: ByteArray, ext: String, mime: String): String {
        val path = "${uid()}/avatar_${System.currentTimeMillis()}.$ext"
        val url = Api.upload(path, token(), bytes, mime)
        Api.patch("profiles?id=eq.${uid()}", token(), JSONObject().put("avatar_url", url))
        return url
    }

    fun blockedUsers(): List<Profile> {
        return try {
            val arr = JSONArray(Api.get("blocks?blocker_id=eq.${uid()}&select=blocked_id", token()))
            val ids = (0 until arr.length()).map { arr.getJSONObject(it).getString("blocked_id") }
            ids.mapNotNull { profileById(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

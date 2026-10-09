package com.pulsechat.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object Api {

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun signInWithIdToken(idToken: String): JSONObject {
        val body = JSONObject()
            .put("provider", "google")
            .put("id_token", idToken)
            .toString()
        val req = Request.Builder()
            .url("${Supabase.URL}/auth/v1/token?grant_type=id_token")
            .addHeader("apikey", Supabase.ANON_KEY)
            .post(body.toRequestBody(JSON))
            .build()
        return executeJson(req)
    }

    fun refresh(refreshToken: String): JSONObject {
        val body = JSONObject().put("refresh_token", refreshToken).toString()
        val req = Request.Builder()
            .url("${Supabase.URL}/auth/v1/token?grant_type=refresh_token")
            .addHeader("apikey", Supabase.ANON_KEY)
            .post(body.toRequestBody(JSON))
            .build()
        return executeJson(req)
    }

    fun signOut(accessToken: String) {
        try {
            val req = Request.Builder()
                .url("${Supabase.URL}/auth/v1/logout")
                .addHeader("apikey", Supabase.ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .post("{}".toRequestBody(JSON))
                .build()
            client.newCall(req).execute().close()
        } catch (_: Exception) {
        }
    }

    /** calls a Postgres function via PostgREST /rpc */
    fun rpc(name: String, token: String, body: JSONObject): String {
        val req = Request.Builder()
            .url("${Supabase.URL}/rest/v1/rpc/$name")
            .addHeader("apikey", Supabase.ANON_KEY)
            .addHeader("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(JSON))
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException(errorMessage(text, resp.code))
            return text
        }
    }

    /** uploads bytes to Supabase Storage and returns the public URL */
    fun upload(path: String, token: String, bytes: ByteArray, mime: String): String {
        val req = Request.Builder()
            .url("${Supabase.URL}/storage/v1/object/media/$path")
            .addHeader("apikey", Supabase.ANON_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", mime)
            .addHeader("x-upsert", "true")
            .post(bytes.toRequestBody(mime.toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException(errorMessage(text, resp.code))
        }
        return "${Supabase.URL}/storage/v1/object/public/media/$path"
    }

    fun get(path: String, token: String): String = rest("GET", path, token, null, null)

    fun post(path: String, token: String, body: JSONObject): String =
        rest("POST", path, token, body, "return=representation")

    fun patch(path: String, token: String, body: JSONObject): String =
        rest("PATCH", path, token, body, "return=representation")

    fun delete(path: String, token: String): String = rest("DELETE", path, token, null, null)

    private fun rest(
        method: String,
        path: String,
        token: String,
        body: JSONObject?,
        prefer: String?,
    ): String {
        val b = Request.Builder()
            .url("${Supabase.URL}/rest/v1/$path")
            .addHeader("apikey", Supabase.ANON_KEY)
            .addHeader("Authorization", "Bearer $token")
        if (prefer != null) b.addHeader("Prefer", prefer)
        when (method) {
            "GET" -> b.get()
            "POST" -> b.post((body ?: JSONObject()).toString().toRequestBody(JSON))
            "PATCH" -> b.patch((body ?: JSONObject()).toString().toRequestBody(JSON))
            "DELETE" -> b.delete()
        }
        client.newCall(b.build()).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException(errorMessage(text, resp.code))
            return text
        }
    }

    private fun executeJson(req: Request): JSONObject {
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException(errorMessage(text, resp.code))
            return JSONObject(text)
        }
    }

    private fun errorMessage(text: String, code: Int): String {
        return try {
            val o = JSONObject(text)
            val m = o.optString("msg")
            if (m.isNotEmpty()) m
            else o.optString("message").ifEmpty {
                o.optString("error_description").ifEmpty { "Error $code" }
            }
        } catch (_: Exception) {
            "Error $code"
        }
    }
}

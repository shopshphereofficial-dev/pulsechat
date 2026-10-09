package com.pulsechat.app.data

import android.content.Context

class Session(context: Context) {

    private val sp = context.getSharedPreferences("pulsechat_session", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = sp.getString("access", null)
        set(v) { sp.edit().putString("access", v).apply() }

    var refreshToken: String?
        get() = sp.getString("refresh", null)
        set(v) { sp.edit().putString("refresh", v).apply() }

    var userId: String?
        get() = sp.getString("uid", null)
        set(v) { sp.edit().putString("uid", v).apply() }

    var email: String?
        get() = sp.getString("email", null)
        set(v) { sp.edit().putString("email", v).apply() }

    fun isLoggedIn(): Boolean = !refreshToken.isNullOrEmpty()

    fun clear() = sp.edit().clear().apply()
}

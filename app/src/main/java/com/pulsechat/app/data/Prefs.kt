package com.pulsechat.app.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("pulsechat_prefs", Context.MODE_PRIVATE)

    /** 0 = system, 1 = light, 2 = dark */
    var themeMode: Int
        get() = sp.getInt("theme", 0)
        set(v) { sp.edit().putInt("theme", v).apply() }

    var wallpaper: Int
        get() = sp.getInt("wallpaper", 0)
        set(v) { sp.edit().putInt("wallpaper", v).apply() }

    var displayName: String
        get() = sp.getString("display_name", "") ?: ""
        set(v) { sp.edit().putString("display_name", v).apply() }
}

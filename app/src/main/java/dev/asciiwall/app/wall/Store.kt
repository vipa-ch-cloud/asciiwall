package dev.asciiwall.app.wall

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import dev.asciiwall.app.core.Config
import dev.asciiwall.app.core.Presets
import org.json.JSONArray
import org.json.JSONObject

const val PREFS = "asciiwall"
const val KEY_ACTIVE = "active_json"
const val KEY_CUSTOM = "custom_json"

fun prefs(ctx: Context): SharedPreferences = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

fun loadActive(sp: SharedPreferences): Config {
    val s = sp.getString(KEY_ACTIVE, null)
    if (s != null) runCatching { return Config.fromJson(JSONObject(s), builtin = Presets.byId(JSONObject(s).optString("id")) != null) }
    return Presets.all.first()
}

fun saveActive(sp: SharedPreferences, c: Config) {
    sp.edit().putString(KEY_ACTIVE, c.sanitized().toJson().toString()).apply()
}

fun loadCustom(sp: SharedPreferences): List<Config> {
    val s = sp.getString(KEY_CUSTOM, null) ?: return emptyList()
    return runCatching {
        val a = JSONArray(s)
        (0 until a.length()).map { Config.fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())
}

fun saveCustom(sp: SharedPreferences, list: List<Config>) {
    val a = JSONArray()
    list.forEach { a.put(it.toJson()) }
    sp.edit().putString(KEY_CUSTOM, a.toString()).apply()
}

/** Акцентный цвет Material You (Android 12+), иначе лавандовый. */
fun systemAccent(ctx: Context): Int =
    if (Build.VERSION.SDK_INT >= 31) runCatching { ctx.getColor(android.R.color.system_accent1_300) }.getOrDefault(DEFAULT_ACCENT)
    else DEFAULT_ACCENT

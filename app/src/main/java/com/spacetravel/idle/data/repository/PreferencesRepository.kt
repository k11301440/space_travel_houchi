package com.spacetravel.idle.data.repository

import android.content.Context
import android.content.SharedPreferences

enum class ThemeSetting { SYSTEM, DARK, LIGHT }
enum class ExploreModeSetting { STANDARD, HIGH_PRECISION }

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("spacetravel_prefs", Context.MODE_PRIVATE)

    var theme: ThemeSetting
        get() = ThemeSetting.valueOf(prefs.getString("theme", ThemeSetting.DARK.name) ?: ThemeSetting.DARK.name)
        set(value) = prefs.edit().putString("theme", value.name).apply()

    var uiScale: Float
        get() = prefs.getFloat("ui_scale", 1.0f)
        set(value) = prefs.edit().putFloat("ui_scale", value).apply()

    var language: String
        get() = prefs.getString("language", "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString("language", value).apply()

    var exploreMode: ExploreModeSetting
        get() = ExploreModeSetting.valueOf(prefs.getString("explore_mode", ExploreModeSetting.STANDARD.name) ?: ExploreModeSetting.STANDARD.name)
        set(value) = prefs.edit().putString("explore_mode", value.name).apply()

    var equippedSkin: String
        get() = prefs.getString("equipped_skin", "skin_default") ?: "skin_default"
        set(value) = prefs.edit().putString("equipped_skin", value).apply()

    var equippedFlame: String
        get() = prefs.getString("equipped_flame", "flame_blue") ?: "flame_blue"
        set(value) = prefs.edit().putString("equipped_flame", value).apply()

    var equippedLight: String
        get() = prefs.getString("equipped_light", "light_cyan") ?: "light_cyan"
        set(value) = prefs.edit().putString("equipped_light", value).apply()

    var isTutorialDone: Boolean
        get() = prefs.getBoolean("tutorial_done", false)
        set(value) = prefs.edit().putBoolean("tutorial_done", value).apply()

    var lastActiveAt: Long
        get() = prefs.getLong("last_active_at", 0L)
        set(value) = prefs.edit().putLong("last_active_at", value).apply()

    var activeExpeditionStart: Long
        get() = prefs.getLong("active_expedition_start", -1L)
        set(value) = prefs.edit().putLong("active_expedition_start", value).apply()

    var activeExpeditionMode: String
        get() = prefs.getString("active_expedition_mode", "A") ?: "A"
        set(value) = prefs.edit().putString("active_expedition_mode", value).apply()

    var debugTimeOffsetMs: Long
        get() = prefs.getLong("debug_time_offset_ms", 0L)
        set(value) = prefs.edit().putLong("debug_time_offset_ms", value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

package com.kozzzak.ios.keyboard

import android.content.Context

class KeyboardPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
    var language: String
        get() = prefs.getString("language", "ru") ?: "ru"
        set(v) { prefs.edit().putString("language", v).apply() }
    var theme: String
        get() = prefs.getString("theme", "system") ?: "system"
        set(v) { prefs.edit().putString("theme", v).apply() }
    var height: Int
        get() = prefs.getInt("height", 54)
        set(v) { prefs.edit().putInt("height", v.coerceIn(44, 68)).apply() }
    fun enabled(key: String, default: Boolean = true) = prefs.getBoolean(key, default)
    fun set(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun words(language: String): Set<String> = prefs.getStringSet("words_$language", emptySet())?.toSet() ?: emptySet()
    fun saveWords(language: String, words: Set<String>) {
        prefs.edit().putStringSet("words_$language", words.filter { it.length in 1..48 && it.all(Char::isLetter) }.take(2000).toSet()).apply()
    }
    fun recentEmoji(): List<String> = prefs.getString("recent_emoji", "")!!.split('|').filter(String::isNotEmpty)
    fun rememberEmoji(emoji: String) { prefs.edit().putString("recent_emoji", (listOf(emoji) + recentEmoji()).distinct().take(40).joinToString("|")).apply() }
    fun reset() { prefs.edit().clear().apply() }
}

package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences

/** Android storage boundary for game progress. Serialization stays explicit in the caller. */
class GameRepository(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun contains(key: String): Boolean = preferences.contains(key)
    fun getBoolean(key: String, fallback: Boolean): Boolean = preferences.getBoolean(key, fallback)
    fun getFloat(key: String, fallback: Float): Float = preferences.getFloat(key, fallback)
    fun getInt(key: String, fallback: Int): Int = preferences.getInt(key, fallback)
    fun getLong(key: String, fallback: Long): Long = preferences.getLong(key, fallback)
    fun getString(key: String, fallback: String?): String? = preferences.getString(key, fallback)
    fun getStringSet(key: String, fallback: Set<String>): Set<String>? =
        preferences.getStringSet(key, fallback)?.toSet()

    fun edit(block: SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply(block).apply()
    }

    private companion object {
        const val FILE_NAME = "game_prefs"
    }
}

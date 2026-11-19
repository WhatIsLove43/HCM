package com.example.hcmanager

import android.content.Context

object SaveManager {
    private const val PREFS_NAME = "hcmanager"
    private const val KEY_GAME = "saved_game"

    fun save(context: Context, game: SavedGame) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GAME, game.toJson())
            .apply()
    }

    fun load(context: Context): SavedGame? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GAME, null) ?: return null
        return try {
            SavedGame.fromJson(json)
        } catch (e: Exception) {
            null  // повреждённое сохранение — не падаем, начинаем заново
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_GAME)
            .apply()
    }
}
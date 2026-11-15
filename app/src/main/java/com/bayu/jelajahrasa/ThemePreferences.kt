package com.bayu.jelajahrasa

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferences {

    private const val PREF_NAME = "jelajah_rasa_preferences"
    private const val KEY_THEME = "selected_theme"

    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    fun getTheme(context: Context): String {
        return context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        ).getString(KEY_THEME, SYSTEM) ?: SYSTEM
    }

    fun saveTheme(context: Context, theme: String) {
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putString(KEY_THEME, theme)
            .apply()

        applyTheme(theme)
    }

    fun applyTheme(theme: String) {
        val mode = when (theme) {
            LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
package com.bayu.jelajahrasa

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferences {

    private const val PREF_NAME = "jelajah_rasa_preferences"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_RECOMMENDATION_MODE = "recommendation_mode"

    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    const val MODE_FAST = "fast"
    const val MODE_ACCURATE = "accurate"

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

    fun getRecommendationMode(context: Context): String {
        return context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        ).getString(KEY_RECOMMENDATION_MODE, MODE_ACCURATE) ?: MODE_ACCURATE
    }

    fun saveRecommendationMode(context: Context, mode: String) {
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putString(KEY_RECOMMENDATION_MODE, mode)
            .apply()
    }
}

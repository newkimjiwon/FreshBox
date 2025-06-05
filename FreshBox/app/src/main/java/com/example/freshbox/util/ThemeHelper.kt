// File: app/src/main/java/com/example/freshbox/util/ThemeHelper.kt
package com.example.freshbox.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    // 테마 모드 상수 (AppCompatDelegate 값 사용)
    const val LIGHT_MODE = AppCompatDelegate.MODE_NIGHT_NO
    const val DARK_MODE = AppCompatDelegate.MODE_NIGHT_YES
    const val DEFAULT_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // 또는 MODE_NIGHT_UNSPECIFIED

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): Int {
        return getPreferences(context).getInt(KEY_THEME_MODE, DEFAULT_MODE)
    }

    fun setThemeMode(context: Context, themeMode: Int) {
        getPreferences(context).edit().putInt(KEY_THEME_MODE, themeMode).apply()
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    fun applyTheme(themeMode: Int) {
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    // 앱 시작 시 저장된 테마 적용을 위해 MyApplication 등에서 호출
    fun applyStoredTheme(context: Context) {
        val storedMode = getThemeMode(context)
        applyTheme(storedMode)
    }
}
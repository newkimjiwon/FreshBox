// File: app/src/main/java/com/example/freshbox/util/ThemeHelper.kt
package com.example.freshbox.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate // 다크 모드/라이트 모드 설정을 위해 필요

// 앱 전체에서 테마 관련 설정을 관리하는 싱글톤 객체
object ThemeHelper {

    // SharedPreferences 파일 이름과 테마 모드를 저장할 키 이름 정의
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    // 테마 모드 상수 정의 (AppCompatDelegate의 미리 정의된 상수 값 사용)
    const val LIGHT_MODE = AppCompatDelegate.MODE_NIGHT_NO       // 라이트 모드
    const val DARK_MODE = AppCompatDelegate.MODE_NIGHT_YES        // 다크 모드
    const val DEFAULT_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // 시스템 설정 따름 (기본값)

    // SharedPreferences 인스턴스를 가져오는 приват 함수
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 저장된 테마 모드 값을 불러오는 함수
    // 저장된 값이 없으면 DEFAULT_MODE를 반환
    fun getThemeMode(context: Context): Int {
        return getPreferences(context).getInt(KEY_THEME_MODE, DEFAULT_MODE)
    }

    // 사용자가 선택한 테마 모드를 SharedPreferences에 저장하고,
    // 즉시 AppCompatDelegate를 통해 앱의 현재 테마를 변경하는 함수
    fun setThemeMode(context: Context, themeMode: Int) {
        getPreferences(context).edit().putInt(KEY_THEME_MODE, themeMode).apply() // 선택된 모드 저장
        AppCompatDelegate.setDefaultNightMode(themeMode) // 앱 전체의 기본 야간 모드 설정 변경
    }

    // 전달받은 테마 모드를 앱에 적용하는 함수 (저장 없이 적용만)
    // AppCompatDelegate.setDefaultNightMode는 다음에 생성되는 Activity부터 영향을 미침
    // 즉각적인 변경을 위해서는 Activity의 recreate()가 필요할 수 있음
    fun applyTheme(themeMode: Int) {
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    // 앱 시작 시 (예: MyApplication의 onCreate) 호출되어
    // SharedPreferences에 저장된 테마 설정을 불러와 앱에 적용하는 함수
    fun applyStoredTheme(context: Context) {
        val storedMode = getThemeMode(context) // 저장된 모드 불러오기
        applyTheme(storedMode) // 해당 모드로 테마 적용
    }
}
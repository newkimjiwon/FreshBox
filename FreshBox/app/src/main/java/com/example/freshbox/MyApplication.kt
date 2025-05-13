package com.example.freshbox

import android.app.Application

class MyApplication : Application() {
    // 데이터베이스 인스턴스를 앱 전역에서 쉽게 접근하기 위해 lazy 초기화 사용 가능
    // val database by lazy { AppDatabase.getDatabase(this) }
    // val repository by lazy { FoodRepository(database.foodDao()) }

    override fun onCreate() {
        super.onCreate()
        // 앱 시작 시 필요한 초기화 작업
    }
}
// File: app/src/main/java/com/example/freshbox/MyApplication.kt
package com.example.freshbox

import android.app.Application
import android.util.Log
import androidx.work.* // WorkManager import 추가
import com.example.freshbox.util.NotificationHelper
import com.example.freshbox.util.ThemeHelper
import com.example.freshbox.worker.ExpiryCheckWorker // Worker import 추가
import java.util.concurrent.TimeUnit

class MyApplication : Application() { // Configuration.Provider 구현은 필수 아님

    override fun onCreate() {
        super.onCreate()
        ThemeHelper.applyStoredTheme(applicationContext)

        // 알림 채널 생성
        NotificationHelper.createNotificationChannel(applicationContext)

        // WorkManager 작업 예약
        scheduleExpiryCheckWorker()
    }

    private fun scheduleExpiryCheckWorker() {
        val constraints = Constraints.Builder()
            // .setRequiresCharging(true) // 필요시 제약 조건 추가 (예: 충전 중일 때만)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // 네트워크 필요 없음
            .build()

        // 하루에 한 번 실행되도록 주기적인 작업 요청 빌드 (예: 24시간 간격)
        // 최소 반복 간격은 15분입니다.
        val repeatingRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
            1, TimeUnit.DAYS // 예: 하루에 한 번 (또는 테스트를 위해 더 짧게 TimeUnit.HOURS 등)
            // 15, TimeUnit.MINUTES // 테스트용 최소 간격
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "expiryCheckWork", // 작업의 고유 이름
            ExistingPeriodicWorkPolicy.KEEP, // 동일 이름의 작업이 이미 있다면 유지 (또는 REPLACE)
            repeatingRequest
        )
        Log.d("MyApplication", "ExpiryCheckWorker scheduled.")
    }
}
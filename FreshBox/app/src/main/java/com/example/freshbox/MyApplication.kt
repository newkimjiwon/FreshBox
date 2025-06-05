// File: app/src/main/java/com/example/freshbox/MyApplication.kt
package com.example.freshbox

import android.app.Application
import android.util.Log
import androidx.work.* // WorkManager 관련 클래스들을 사용하기 위해 import 합니다.
import com.example.freshbox.util.NotificationHelper // 알림 생성을 도와주는 유틸리티 클래스 import
import com.example.freshbox.util.ThemeHelper // 테마 설정을 관리하는 유틸리티 클래스 import
import com.example.freshbox.worker.ExpiryCheckWorker // 유통기한 확인을 위한 백그라운드 작업자 import
import java.util.concurrent.TimeUnit // 시간 단위를 지정하기 위해 import (예: DAYS, MINUTES)

// Application 클래스를 상속받아 앱 전체의 생명주기 동안 유지되는 커스텀 Application 클래스를 정의합니다.
// 앱의 전역적인 초기화 작업을 수행하기에 적합합니다.
// WorkManager의 커스텀 설정을 위한 Configuration.Provider 구현은 현재 필수는 아닙니다.
class MyApplication : Application() {

    // Application 객체가 생성될 때 호출되는 메서드입니다.
    // 앱의 다른 컴포넌트(Activity, Service 등)가 생성되기 전에 실행됩니다.
    override fun onCreate() {
        super.onCreate() // 부모 클래스의 onCreate()를 반드시 호출해야 합니다.

        // 앱 시작 시 저장된 사용자 테마 설정을 불러와 적용합니다.
        // ThemeHelper는 SharedPreferences 등을 사용하여 사용자가 선택한 테마(라이트/다크/시스템)를 관리합니다.
        ThemeHelper.applyStoredTheme(applicationContext)

        // 유통기한 만료 알림을 위한 알림 채널을 생성합니다.
        // Android 8.0 (API 26) 이상에서는 알림을 보내기 전에 반드시 알림 채널을 등록해야 합니다.
        // NotificationHelper는 알림 채널 생성 및 알림 표시 로직을 담당합니다.
        NotificationHelper.createNotificationChannel(applicationContext)

        // WorkManager를 사용하여 유통기한 확인 백그라운드 작업을 예약합니다.
        scheduleExpiryCheckWorker()
    }

    // 유통기한 확인 백그라운드 작업을 예약하는 приват 함수입니다.
    private fun scheduleExpiryCheckWorker() {
        // 작업 실행을 위한 제약 조건 설정 (선택 사항)
        val constraints = Constraints.Builder()
            // .setRequiresCharging(true) // 예: 기기가 충전 중일 때만 작업을 실행하도록 설정
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // 이 작업은 네트워크 연결이 필요 없음을 명시
            .build()

        // 주기적인 작업 요청 생성
        // ExpiryCheckWorker 클래스를 하루에 한 번 실행하도록 설정합니다.
        // WorkManager의 최소 반복 간격은 15분입니다.
        val repeatingRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
            1, TimeUnit.DAYS // 작업 반복 간격: 1일 (테스트 시에는 더 짧은 간격으로 변경 가능)
            // 15, TimeUnit.MINUTES // 테스트용 최소 간격 예시
        )
            .setConstraints(constraints) // 위에서 정의한 제약 조건 적용
            // .setInitialDelay(10, TimeUnit.SECONDS) // 초기 지연 시간 설정 (예: 앱 시작 후 10초 뒤 첫 실행) - 선택 사항
            .build()

        // WorkManager에 주기적인 작업을 등록(enqueue)합니다.
        // enqueueUniquePeriodicWork를 사용하면 동일한 이름의 작업이 중복으로 예약되는 것을 방지합니다.
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "expiryCheckWork", // 작업의 고유한 이름. 이 이름을 사용하여 작업을 조회하거나 취소할 수 있습니다.
            ExistingPeriodicWorkPolicy.KEEP, // 동일한 이름의 작업이 이미 예약되어 있을 경우 기존 작업을 유지합니다.
            // (REPLACE로 설정하면 기존 작업을 취소하고 새 작업으로 대체합니다.)
            repeatingRequest // 실제 실행할 작업 요청
        )
        // 작업이 예약되었음을 로그로 남깁니다.
        Log.d("MyApplication", "ExpiryCheckWorker scheduled to run periodically.")
    }
}
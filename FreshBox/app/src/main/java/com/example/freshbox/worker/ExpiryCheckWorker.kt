// File: app/src/main/java/com/example/freshbox/worker/ExpiryCheckWorker.kt
package com.example.freshbox.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker // 코루틴 기반 Worker를 만들기 위해 상속
import androidx.work.WorkerParameters // Worker에 필요한 파라미터를 전달받기 위해 사용
import com.example.freshbox.data.AppDatabase // Room 데이터베이스 인스턴스를 가져오기 위해 import
import com.example.freshbox.repository.FoodRepository // 데이터 관련 로직을 처리하는 Repository import
import com.example.freshbox.util.NotificationHelper // 알림 생성 및 표시를 도와주는 유틸리티 import
import java.util.Calendar // 날짜 및 시간 계산을 위해 사용
import java.util.TimeZone // 사용자의 현재 시간대를 고려하기 위해 사용

// CoroutineWorker를 상속받아 비동기 작업을 코루틴으로 처리합니다.
class ExpiryCheckWorker(
    appContext: Context, // Application Context, Worker는 Application Context를 사용해야 함
    workerParams: WorkerParameters // Worker 실행에 필요한 설정 및 데이터
) : CoroutineWorker(appContext, workerParams) {

    // 실제 백그라운드 작업이 수행되는 메서드입니다.
    // 이 메서드는 백그라운드 스레드에서 호출됩니다 (코루틴 컨텍스트).
    override suspend fun doWork(): Result {
        Log.d("ExpiryCheckWorker", "Work starting...") // 작업 시작 로그
        try {
            // 데이터베이스 접근을 위한 DAO 인스턴스 가져오기
            val foodDao = AppDatabase.getDatabase(applicationContext).foodDao()
            val categoryDao = AppDatabase.getDatabase(applicationContext).categoryDao() // CategoryDao도 가져옴 (FoodRepository 생성자에 필요)

            // Repository 인스턴스 생성. FoodDao와 CategoryDao를 전달합니다.
            // (실제 앱에서는 Hilt나 Koin과 같은 의존성 주입(DI) 라이브러리를 사용하면 더 좋습니다.)
            val repository = FoodRepository(foodDao, categoryDao)

            // 오늘 날짜의 시작과 끝 시간을 나타내는 타임스탬프 계산
            // 사용자의 현재 시간대를 기준으로 "오늘"을 정의합니다.
            val today = Calendar.getInstance(TimeZone.getDefault()) // 현재 시간대의 Calendar 객체 생성

            // 오늘의 시작 시간 (00:00:00.000)
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            val startOfDayMillis = today.timeInMillis // 밀리초 단위 타임스탬프

            // 오늘의 종료 시간 (23:59:59.999)
            today.set(Calendar.HOUR_OF_DAY, 23)
            today.set(Calendar.MINUTE, 59)
            today.set(Calendar.SECOND, 59)
            today.set(Calendar.MILLISECOND, 999)
            val endOfDayMillis = today.timeInMillis // 밀리초 단위 타임스탬프

            Log.d("ExpiryCheckWorker", "Checking items expiring between $startOfDayMillis and $endOfDayMillis")

            // Repository를 통해 오늘 유통기한이 만료되는 식품 목록을 가져옵니다.
            // 이 메서드는 FoodRepository 및 FoodDao에 정의되어 있어야 하며, suspend 함수여야 합니다.
            val expiringItems = repository.getItemsExpiringToday(startOfDayMillis, endOfDayMillis)
            Log.d("ExpiryCheckWorker", "Found ${expiringItems.size} items expiring today.")

            // 만료되는 식품이 있다면 알림을 표시합니다.
            if (expiringItems.isNotEmpty()) {
                NotificationHelper.showExpiryNotification(applicationContext, expiringItems)
            }

            Log.d("ExpiryCheckWorker", "Work finished successfully.")
            return Result.success() // 작업 성공 결과 반환
        } catch (e: Exception) {
            // 작업 중 오류 발생 시 로그를 남기고 실패 결과를 반환합니다.
            Log.e("ExpiryCheckWorker", "Error during work: ${e.message}", e)
            return Result.failure() // 작업 실패 결과 반환
        }
    }
}
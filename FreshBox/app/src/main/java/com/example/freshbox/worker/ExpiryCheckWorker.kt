// File: app/src/main/java/com/example/freshbox/worker/ExpiryCheckWorker.kt
package com.example.freshbox.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.freshbox.data.AppDatabase
import com.example.freshbox.repository.FoodRepository
import com.example.freshbox.util.NotificationHelper
import java.util.Calendar
import java.util.TimeZone

class ExpiryCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ExpiryCheckWorker", "Work starting...")
        try {
            // Repository 인스턴스 생성 (DI를 사용한다면 더 좋음)
            val foodDao = AppDatabase.getDatabase(applicationContext).foodDao()
            val categoryDao = AppDatabase.getDatabase(applicationContext).categoryDao() // Repository 생성자 변경으로 추가
            val repository = FoodRepository(foodDao, categoryDao)

            // 오늘 날짜의 시작과 끝 타임스탬프 계산
            val today = Calendar.getInstance(TimeZone.getDefault())
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            val startOfDayMillis = today.timeInMillis

            today.set(Calendar.HOUR_OF_DAY, 23)
            today.set(Calendar.MINUTE, 59)
            today.set(Calendar.SECOND, 59)
            today.set(Calendar.MILLISECOND, 999)
            val endOfDayMillis = today.timeInMillis

            Log.d("ExpiryCheckWorker", "Checking items expiring between $startOfDayMillis and $endOfDayMillis")

            val expiringItems = repository.getItemsExpiringToday(startOfDayMillis, endOfDayMillis)
            Log.d("ExpiryCheckWorker", "Found ${expiringItems.size} items expiring today.")

            if (expiringItems.isNotEmpty()) {
                NotificationHelper.showExpiryNotification(applicationContext, expiringItems)
            }

            Log.d("ExpiryCheckWorker", "Work finished successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.e("ExpiryCheckWorker", "Error during work: ${e.message}", e)
            return Result.failure()
        }
    }
}
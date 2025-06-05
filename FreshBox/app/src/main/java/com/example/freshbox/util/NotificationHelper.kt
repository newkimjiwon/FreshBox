// File: app/src/main/java/com/example/freshbox/util/NotificationHelper.kt
package com.example.freshbox.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.freshbox.R
import com.example.freshbox.data.FoodItem
import com.example.freshbox.ui.list.MainActivity // 알림 클릭 시 이동할 Activity
import android.util.Log

object NotificationHelper {

    private const val CHANNEL_ID = "freshbox_expiry_channel"
    private const val CHANNEL_NAME = "유통기한 알림"
    private const val CHANNEL_DESC = "유통기한 만료 예정 식품 알림"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showExpiryNotification(context: Context, expiringItems: List<FoodItem>) {
        if (expiringItems.isEmpty()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // TODO: 필요하다면 Intent에 추가 데이터 전달 (예: 특정 화면으로 이동시키기 위함)
            // intent.putExtra("notification_source", "expiry_alert")
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "오늘 유통기한 만료 식품 알림!"
        val contentText = if (expiringItems.size == 1) {
            "${expiringItems.first().name}의 유통기한이 오늘까지입니다."
        } else {
            "오늘 유통기한이 만료되는 식품이 ${expiringItems.size}개 있습니다: ${expiringItems.take(3).joinToString { it.name }} 등"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.bg_calendar_day_expiry) // TODO: 알림용 아이콘 추가 (예: ic_stat_expiry)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText)) // 긴 텍스트 표시
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // 알림 클릭 시 실행될 Intent
            .setAutoCancel(true) // 클릭 시 알림 자동 제거

        try {
            with(NotificationManagerCompat.from(context)) {
                // TODO: Android 13 (API 33) 이상에서는 POST_NOTIFICATIONS 권한 필요
                // 권한이 없으면 notify 호출 시 SecurityException 발생 가능
                notify(NOTIFICATION_ID, builder.build())
                Log.d("NotificationHelper", "Notification shown for ${expiringItems.size} items.")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Failed to show notification due to missing permission: ${e.message}")
            // TODO: 사용자에게 권한 요청 안내 또는 설정으로 이동 유도
        }
    }
}
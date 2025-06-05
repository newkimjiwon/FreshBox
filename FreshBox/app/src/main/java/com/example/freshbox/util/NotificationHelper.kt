// File: app/src/main/java/com/example/freshbox/util/NotificationHelper.kt
package com.example.freshbox.util

import android.app.NotificationChannel // Android 8.0 (API 26) 이상에서 알림 채널을 생성하기 위해 필요
import android.app.NotificationManager // 시스템 알림 서비스를 관리
import android.app.PendingIntent // 알림을 클릭했을 때 실행될 Intent를 지연 실행하기 위해 필요
import android.content.Context
import android.content.Intent
import android.os.Build // 현재 안드로이드 버전을 확인하기 위해 필요
import androidx.core.app.NotificationCompat // 다양한 버전의 안드로이드에서 알림을 쉽게 만들 수 있도록 도와주는 호환성 클래스
import androidx.core.app.NotificationManagerCompat // 알림을 실제로 표시하고 관리하는 호환성 클래스
import com.example.freshbox.R // R.drawable.ic_notification_icon 등 리소스 참조를 위해 필요
import com.example.freshbox.data.FoodItem // 알림 내용에 식품 정보를 사용하기 위해 import
import com.example.freshbox.ui.list.MainActivity // 알림 클릭 시 이동할 기본 Activity
import android.util.Log // 디버깅 로그를 위해 사용

// 알림 관련 작업을 도와주는 싱글톤 객체
object NotificationHelper {

    // 알림 채널을 위한 상수 정의
    private const val CHANNEL_ID = "freshbox_expiry_channel" // 알림 채널의 고유 ID
    private const val CHANNEL_NAME = "유통기한 알림" // 사용자에게 보여질 알림 채널의 이름
    private const val CHANNEL_DESC = "유통기한 만료 예정 식품 알림" // 사용자에게 보여질 알림 채널의 설명
    private const val NOTIFICATION_ID = 1001 // 알림의 고유 ID (여러 알림을 구분하거나 업데이트할 때 사용)

    /**
     * Android 8.0 (Oreo, API 26) 이상 버전에서 알림을 표시하기 전에 호출되어야 하는 함수입니다.
     * 이 버전부터는 모든 알림이 특정 채널에 속해야 합니다.
     * @param context ApplicationContext 또는 Activity Context.
     */
    fun createNotificationChannel(context: Context) {
        // Android Oreo (API 26) 이상에서만 알림 채널 생성 로직 실행
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 알림의 중요도 설정 (기본 중요도)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            // 알림 채널 객체 생성
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC // 채널 설명 설정
            }
            // 시스템의 NotificationManager 서비스 가져오기
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // 생성한 알림 채널을 시스템에 등록
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "Notification channel $CHANNEL_ID created.")
        }
    }

    /**
     * 유통기한 만료 (또는 임박) 식품에 대한 시스템 알림을 생성하고 표시합니다.
     * @param context ApplicationContext 또는 Activity Context.
     * @param expiringItems 알림에 표시할 식품 정보 리스트.
     */
    fun showExpiryNotification(context: Context, expiringItems: List<FoodItem>) {
        // 알림을 받을 아이템이 없으면 아무것도 하지 않음
        if (expiringItems.isEmpty()) {
            Log.d("NotificationHelper", "No items to notify.")
            return
        }

        // 알림을 클릭했을 때 실행될 Intent 생성 (예: MainActivity 실행)
        val intent = Intent(context, MainActivity::class.java).apply {
            // Activity 스택 관리 플래그 설정 (새 작업으로 시작하거나 기존 작업 지우고 시작)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // TODO: 필요하다면 Intent에 추가 데이터 전달 (예: 특정 화면으로 이동, 특정 아이템 정보 등)
            // intent.putExtra("notification_action", "view_expired_items")
        }
        // Intent를 즉시 실행하지 않고, 특정 시점(예: 알림 클릭)에 실행되도록 하는 PendingIntent 생성
        // FLAG_IMMUTABLE: 생성된 PendingIntent는 변경 불가능
        // FLAG_UPDATE_CURRENT: 동일한 PendingIntent가 이미 있다면, extra 데이터만 업데이트
        // requestCode는 PendingIntent를 구분하는 데 사용 (여기서는 0으로 고정, 필요시 동적 ID 사용)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 알림 제목 설정
        val title = "오늘 유통기한 만료 식품 알림!" // strings.xml에서 가져오는 것이 좋음
        // 알림 내용 설정 (만료 아이템 개수에 따라 다르게)
        val contentText = if (expiringItems.size == 1) {
            "${expiringItems.first().name}의 유통기한이 오늘까지입니다."
        } else {
            // 여러 개인 경우, 처음 3개 아이템 이름과 총 개수를 보여줌
            "오늘 유통기한이 만료되는 식품이 ${expiringItems.size}개 있습니다: ${expiringItems.take(3).joinToString { it.name }} 등"
        }

        // 더 긴 내용을 보여주기 위한 BigTextStyle 설정
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(expiringItems.joinToString(separator = "\n") { "${it.name} (수량: ${it.quantity})" } + "\n확인해주세요!") // 여러 줄로 표시될 내용
            .setBigContentTitle(title) // 확장된 알림의 제목 (일반 제목과 같게 설정)
        // .setSummaryText("총 ${expiringItems.size}개 만료") // 요약 텍스트 (선택 사항)

        // NotificationCompat.Builder를 사용하여 알림 객체 구성
        val builder = NotificationCompat.Builder(context, CHANNEL_ID) // 알림 채널 ID 지정
            .setSmallIcon(R.drawable.bg_calendar_day_expiry) // 상태 표시줄에 표시될 작은 아이콘 (필수)
            // TODO: 적절한 알림 전용 아이콘으로 교체 (예: ic_stat_warning)
            .setContentTitle(title)                           // 알림 제목
            .setContentText(contentText)                      // 알림 요약 내용 (한 줄)
            .setStyle(bigTextStyle)                           // 확장 가능한 알림 스타일 (긴 텍스트 표시용)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 알림 중요도 (기본값)
            .setContentIntent(pendingIntent)                  // 사용자가 알림을 탭했을 때 실행될 작업
            .setAutoCancel(true)                              // 사용자가 알림을 탭하면 알림이 자동으로 사라지도록 설정

        try {
            // NotificationManagerCompat를 사용하여 알림을 시스템에 게시
            with(NotificationManagerCompat.from(context)) {
                // TODO: Android 13 (API 33) 이상에서는 POST_NOTIFICATIONS 런타임 권한 필요.
                //       권한이 없으면 여기서 SecurityException 발생 가능.
                //       권한 확인 및 요청 로직이 알림을 보내기 전에 수행되어야 함.
                notify(NOTIFICATION_ID, builder.build()) // 알림 ID와 빌드된 알림 객체 전달
                Log.d("NotificationHelper", "Notification shown for ${expiringItems.size} items. ID: $NOTIFICATION_ID")
            }
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS 권한이 없을 경우 SecurityException 발생 가능
            Log.e("NotificationHelper", "Failed to show notification due to missing permission: ${e.message}")
            // TODO: 사용자에게 권한이 없어 알림을 표시할 수 없음을 알리거나, 설정으로 안내하는 등의 후속 처리
        }
    }
}
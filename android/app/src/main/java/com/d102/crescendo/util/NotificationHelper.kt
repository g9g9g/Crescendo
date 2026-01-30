package com.d102.crescendo.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.d102.crescendo.R

object NotificationHelper {
    // TODO: 악보 업로드 채널 변수명 변경?

    private const val CHANNEL_ID = "sheet_upload_channel"
    private const val CHANNEL_NAME = "악보 업로드"
    private const val CHANNEL_DESCRIPTION = "악보 업로드 완료 알림"

    const val EVALUATION_CHANNEL_ID = "evaluation_channel"
    private const val EVALUATION_CHANNEL_NAME = "연주 평가 알림"
    private const val EVALUATION_CHANNEL_DESCRIPTION = "AI 연주 평가 완료 및 오류 알림"

    /**
     * 알림 채널 생성 (Android 8.0 이상 필수)
     * Application 클래스에서 한 번만 호출하면 됩니다.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val evalChannel = NotificationChannel(
                EVALUATION_CHANNEL_ID,
                EVALUATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = EVALUATION_CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(evalChannel)
        }
    }

    /**
     * 알림 권한 확인
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13 미만에서는 권한이 필요 없음
            true
        }
    }

    /**
     * 악보 등록 완료 알림 표시
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showSheetRegistrationSuccessNotification(
        context: Context,
        sheetTitle: String
    ) {
        // 권한 확인
        if (!hasNotificationPermission(context)) {
            Log.w("NotificationHelper", "알림 권한이 없습니다. 알림을 표시할 수 없습니다.")
            return
        }
        // 알림 클릭 시 앱 메인 화면으로 이동하는 Intent (선택사항)
        // 만약 특정 화면으로 이동하려면 Intent를 수정하세요
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 알림 생성
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check) // 체크 아이콘 사용
            .setContentTitle("악보 등록 완료")
            .setContentText("\"$sheetTitle\" 악보가 등록되었습니다.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("\"$sheetTitle\" 악보가 성공적으로 등록되었습니다. 이제 연주할 수 있습니다."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // 알림 클릭 시 자동 제거
            .setContentIntent(pendingIntent)
            .build()

        // 알림 표시
        NotificationManagerCompat.from(context).notify(
            System.currentTimeMillis().toInt(), // 고유 ID로 시간 사용
            notification
        )
    }

    /**
     * 악보 등록 실패 알림 표시
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showSheetRegistrationErrorNotification(
        context: Context,
        errorMessage: String
    ) {
        // 권한 확인
        if (!hasNotificationPermission(context)) {
            Log.w("NotificationHelper", "알림 권한이 없습니다. 알림을 표시할 수 없습니다.")
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("악보 등록 실패")
            .setContentText(errorMessage)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(errorMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
    /**
     * FCM (onMessageReceived)을 위한 범용 알림 표시 함수
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showFcmNotification(
        context: Context,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w("NotificationHelper", "알림 권한이 없습니다.")
            return
        }

        // 알림 클릭 시 앱 메인 화면으로 이동
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, EVALUATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_crescendo_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

            // 헤드업 알림을 위한 필수 설정
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

            // 소리/진동 (헤드업 조건)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // 소리 + 진동

            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }

    /**
     * [!!]연주 평가 실패 알림 표시
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showEvaluationErrorNotification(
        context: Context,
        errorMessage: String
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w("NotificationHelper", "알림 권한이 없습니다.")
            return
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, EVALUATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("연주 평가 실패")
            .setContentText(errorMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}
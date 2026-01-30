package com.d102.crescendo.fcm

import android.util.Log
import androidx.annotation.RequiresPermission
import com.d102.crescendo.domain.repository.TokenRepository
import com.d102.crescendo.domain.usecase.fcm.UpdateFcmTokenUseCase
import com.d102.crescendo.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TAG = "Crescendo_FCM"

@AndroidEntryPoint // Hilt가 의존성을 주입하도록 설정
class CrescendoFirebaseMessagingService : FirebaseMessagingService() {
    // 서비스는 안드로이드 시스템이 관리하는 컴포넌트이므로

    // 힐트는 필드 주입을 한다. 안드로이스 시스템이 만들 때 적절한 순간에 끼어든다.
    @Inject
    lateinit var updateFcmTokenUseCase: UpdateFcmTokenUseCase

    @Inject
    lateinit var tokenRepository: TokenRepository

    /**
     * 새 토큰이 발급될 때 (앱 설치, 토큰 만료 등)
     * 이 토큰을 우리 서버(POST /api/fcm)로 전송
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: $token")
        CoroutineScope(Dispatchers.IO).launch {
            tokenRepository.saveFcmToken(token)  // 로컬 저장
            Log.d(TAG, "onNewToken: FCM Token saved to DataStore")
            updateFcmTokenUseCase(token)  // 서버 저장
                .onSuccess { Log.d(TAG, "onNewToken: Token update SUCCESS") }
                .onFailure { Log.e(TAG, "onNewToken: Token update FAILED", it) }
        }
    }

    @RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "onMessageReceived: From: ${remoteMessage.from}")
        Log.d(TAG, "onMessageReceived: Type: ${remoteMessage.messageType}")
        Log.d(TAG, "onMessageReceived: Data: ${remoteMessage.data}")

        // 1. 수신한 메시지에서 Title과 Body 추출
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]

            Log.d(TAG, "onMessageReceived (Data): Title: $title, Body: $body")

            // 2. NotificationHelper 호출
            // (주의: NotificationHelper.showEvaluationSuccessNotification 함수가
            // title, body를 받도록 수정하거나,
            // 이전의 showFcmNotification 함수를 사용해야 함)

            if (!title.isNullOrBlank() && !body.isNullOrBlank()) {
                // 이전에 사용했던 범용 알림 함수를 다시 사용 (가정)
                NotificationHelper.showFcmNotification(
                    context = this,
                    title = title,
                    body = body
                )
            } else {
                Log.w(TAG, "onMessageReceived: Data에 title 또는 body가 없음")
            }

        } else if (remoteMessage.notification != null) {
            // (혹시 모를) notification 페이로드 처리 (기존 로직)
            val title = remoteMessage.notification?.title
            val body = remoteMessage.notification?.body
            Log.d(TAG, "onMessageReceived (Notification): Title: $title, Body: $body")

            if (!title.isNullOrBlank() && !body.isNullOrBlank()) {
                NotificationHelper.showFcmNotification(
                    context = this,
                    title = title,
                    body = body
                )
            }
        }
    }
}
package com.d102.crescendo.domain.repository

/**
 * FCM 토큰 관리 Repository 인터페이스
 */
interface FcmRepository {

    /**
     * FCM 토큰을 서버에 전송 (등록/갱신)
     */
    suspend fun updateFcmToken(token: String): Result<Unit>
}
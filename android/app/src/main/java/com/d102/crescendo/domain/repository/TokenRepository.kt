package com.d102.crescendo.domain.repository

import com.d102.crescendo.domain.model.AuthToken
import kotlinx.coroutines.flow.Flow

interface TokenRepository {
    /**
     * 인증 토큰 (Access, Refresh)을 DataStore에 저장
     */
    suspend fun saveAuthTokens(tokens: AuthToken)

    /**
     * Access Token만 Flow로 관찰
     * (SplashViewModel이 앱 인증 상태를 확인하는 데 사용)
     */
    fun getAccessToken(): Flow<String?>

    /**
     * Refresh Token만 (재발급용)
     */
    fun getRefreshToken(): Flow<String?>

    /**
     * 모든 토큰을 삭제. (로그아웃 시)
     */
    suspend fun clearTokens()

    fun isFirstLogin(): Flow<Boolean>


    suspend fun saveFcmToken(fcmToken: String)
    fun getFcmToken(): Flow<String?>

    /**
     * [추가] 온보딩 완료 시, 'firstLoginYn' 플래그만 false로 업데이트
     */
    suspend fun updateFirstLoginStatus(isFirstLogin: Boolean)
}
package com.d102.crescendo.domain.repository

import com.d102.crescendo.domain.model.AuthToken

interface AuthRepository {

    /**
     * Google ID 토큰으로 서버에 로그인을 요청
     * @param idToken 구글에서 발급받은 ID 토큰
     * @return 로그인 성공 시 Result<AuthToken> 반환
     */
    suspend fun googleLogin(idToken: String): Result<AuthToken>

    /**
     * Access Token을 재발급
     * (현재 로컬에 저장된 Refresh Token을 사용한다고 가정)
     * @return 재발급 성공 시 Result<AuthToken> 반환
     */
    suspend fun reissueToken(): Result<AuthToken>

    /**
     * 서버에 로그아웃을 요청합니다.
     * (현재 로컬에 저장된 Refresh Token을 서버로 전송)
     * @return 로그아웃 성공 시 Result<Unit> 반환
     */
    suspend fun logout(): Result<Unit>
}
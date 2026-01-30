package com.d102.crescendo.domain.model

/**
 * Domain 계층에서 사용하는 인증 토큰 모델
 * (data 계층의 TokenResponse DTO와 분리됨)
 */
data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val firstLoginYn: Boolean
)
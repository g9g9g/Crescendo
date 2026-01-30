package com.d102.crescendo.domain.usecase.auth

import android.util.Log
import com.d102.crescendo.domain.repository.TokenRepository
import com.d102.crescendo.presentation.ui.screen.splash.SplashUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val TAG = "Crescendo_GetAuthStatusFlowUseCase"
class GetAuthStatusFlowUseCase @Inject constructor(
    private val tokenRepository: TokenRepository
) {
    operator fun invoke(): Flow<SplashUiState> {
        val accessTokenFlow = tokenRepository.getAccessToken()
        val isFirstLoginFlow = tokenRepository.isFirstLogin()

        return combine(accessTokenFlow, isFirstLoginFlow) { token, isFirstLogin ->
            when {
                !token.isNullOrEmpty() && isFirstLogin -> {
                    // 1순위 토큰 O, 최초 O -> 온보딩
                    Log.d(TAG, "invoke: 온보딩으로 이동")
                    SplashUiState.NavigateToOnboarding
                }
                !token.isNullOrEmpty() && !isFirstLogin -> {
                    // 2순위: 토큰 O, 최초 X -> 메인
                    Log.d(TAG, "invoke: 메인으로 이동")
                    SplashUiState.NavigateToMain
                }
                else -> {
                    Log.d(TAG, "invoke: 로그인 화면으로 이동")
                    SplashUiState.NavigateToLogin
                }
            }
        }
    }
}
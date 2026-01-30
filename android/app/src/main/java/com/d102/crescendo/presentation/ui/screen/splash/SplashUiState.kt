package com.d102.crescendo.presentation.ui.screen.splash

sealed interface SplashUiState {
    // 초기 확인 상태
    data object Loading : SplashUiState

    // 토큰 유효로 로그인 된 상태
    data object NavigateToMain : SplashUiState

    // 로그아웃된 상태
    data object NavigateToLogin : SplashUiState

    // 온보딩으로
    data object NavigateToOnboarding : SplashUiState
}
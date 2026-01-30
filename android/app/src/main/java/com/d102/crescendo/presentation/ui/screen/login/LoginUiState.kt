package com.d102.crescendo.presentation.ui.screen.login

/**
 * LoginScreen의 UI 상태를 나타냅니다.
 */
sealed interface LoginUiState {
    /** 기본, 대기 상태 */
    data object Idle : LoginUiState
    /** 로그인 시도 중 (로딩 스피너 표시) */
    data object Loading : LoginUiState
    /** 로그인 실패 */
    data class Error(val message: String) : LoginUiState


    /**
     * 로그인 성공 상태.
     * UI가 이 상태를 감지하면 onLoginSuccess 콜백을 호출
     * @param isFirstLogin 서버에서 받은 최초 로그인 여부 값
     */
    data class Success(val isFirstLogin: Boolean) : LoginUiState
}
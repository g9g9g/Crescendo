package com.d102.crescendo.presentation.ui.screen.profile.edit

/**
 * 프로필 수정 API 통신의 상태
 */
sealed interface ProfileEditUiState {
    /** 기본, 대기 상태 */
    data object Idle : ProfileEditUiState

    /** 닉네임 중복확인 또는 프로필 수정 제출 중 */
    data object Loading : ProfileEditUiState

    /** API 통신 실패 */
    data class Error(val message: String) : ProfileEditUiState

    /**
     이 상태가 되면 onCompleteClick 콜백을 호출
     */
    data object Complete : ProfileEditUiState
}
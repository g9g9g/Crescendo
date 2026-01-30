package com.d102.crescendo.presentation.ui.screen.profile

import com.d102.crescendo.domain.model.onboarding.Genre
import com.d102.crescendo.domain.model.onboarding.Instrument
import com.d102.crescendo.domain.model.profile.UserProfile

/**
 * 프로필 화면의 API 통신 상태
 */
sealed interface ProfileUiState {
    data object Idle: ProfileUiState

    data object Loading : ProfileUiState

    /** API 요청 성공 */
    data class Success(
        val userProfile: UserProfile,
        val genres: List<Genre>,
        val instruments: List<Instrument>,
    ) : ProfileUiState

    /** 로그아웃 성공 시 로그인으로 이동 **/
    data object NavigateToLogin : ProfileUiState

    /** API 요청 실패 */
    data class Error(val message: String) : ProfileUiState
}
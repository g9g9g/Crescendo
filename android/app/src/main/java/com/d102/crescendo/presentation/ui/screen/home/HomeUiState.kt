package com.d102.crescendo.presentation.ui.screen.home

import com.d102.crescendo.domain.model.onboarding.Genre
import com.d102.crescendo.domain.model.onboarding.Instrument
import com.d102.crescendo.domain.model.profile.UserProfile
import com.d102.crescendo.domain.model.sheet.PopularSheet
import com.d102.crescendo.domain.model.sheet.RecentPractice // 👈 2단계 도메인 모델
import com.d102.crescendo.domain.model.sheet.TodayRecommendSheet

/**
 * 홈 화면의 API 통신 상태
 */
sealed interface HomeUiState {
    data object Idle : HomeUiState
    /** API 요청 전, 로딩 중 */
    data object Loading : HomeUiState

    /**
     * API 요청 성공
     */
    data class Success(
        val userProfile: UserProfile,
        val recentPracticeList: List<RecentPractice>,
        val todayRecommendSheets: List<TodayRecommendSheet>,
        val genres: List<Genre>,
        val instruments: List<Instrument>
    ) : HomeUiState


    /** API 요청 실패 */
    data class Error(val message: String) : HomeUiState
}
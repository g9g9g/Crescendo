package com.d102.crescendo.presentation.ui.screen.onboarding

import com.d102.crescendo.domain.model.onboarding.Genre
import com.d102.crescendo.domain.model.onboarding.Instrument
import com.d102.crescendo.domain.model.onboarding.OnboardingRecommendSheet


sealed interface OnboardingUiState {
    /** 초기 데이터 로딩 중 */
    data object Loading : OnboardingUiState

    /** 온보딩 진행 중인 상태. 모든 필요한 데이터를 포함합니다. */
    data class Content(
        val currentStep: OnboardingStep = OnboardingStep.SELECT_GENRE,
        val genres: List<Genre> = emptyList(),
        val selectedGenreIds: Set<Int> = emptySet(),
        val instruments: List<Instrument> = emptyList(),
        val selectedInstrumentId: Int? = null,
        val isNextButtonEnabled: Boolean = false,
        // 추천 악보 18개
        val onboardingRecommendSheets: List<OnboardingRecommendSheet> = emptyList(),
        // 사용자 선택
        val selectedOnboardingSheetIds: Set<Long> = emptySet(),
        val isLoading: Boolean = false,  // 완료 버튼 API 호출

    ) : OnboardingUiState

    /** 온보딩 완료 -> 메인 화면으로 네비게이션을 트리거하는 상태 */
    data object Complete : OnboardingUiState

    /** 데이터 로딩 실패 등 에러 상태 */
    data class Error(val message: String?) : OnboardingUiState
}


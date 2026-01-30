package com.d102.crescendo.presentation.ui.screen.ranking

import com.d102.crescendo.domain.model.onboarding.Instrument
import com.d102.crescendo.domain.model.rank.Ranker

/**
 * 랭킹 화면의 API 통신 상태
 */
sealed interface RankingUiState {
    data object Loading : RankingUiState
    data class Success(
        val topRankers: List<Ranker>,
        val instruments: List<Instrument>,
    ) : RankingUiState
    data class Error(val message: String) : RankingUiState
}
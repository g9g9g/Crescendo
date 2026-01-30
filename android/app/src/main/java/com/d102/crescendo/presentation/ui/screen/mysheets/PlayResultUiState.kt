package com.d102.crescendo.presentation.ui.screen.mysheets

import com.d102.crescendo.domain.model.performance.PlayResult

sealed interface PlayResultUiState {
    object Idle : PlayResultUiState
    object Loading : PlayResultUiState
    data class Success(val playResult: PlayResult) : PlayResultUiState
    data class Error(val message: String) : PlayResultUiState
}
package com.d102.crescendo.presentation.ui.screen.servicesheets.sections

sealed interface OstUiState {
    data object Loading : OstUiState
    data class Success(val count: Int) : OstUiState
    data class Error(val message: String?) : OstUiState
}
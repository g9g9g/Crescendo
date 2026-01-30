package com.d102.crescendo.presentation.ui.screen.servicesheets.sections

sealed interface KidsUiState {
    data object Loading : KidsUiState
    data class Success(val count: Int) : KidsUiState
    data class Error(val message: String?) : KidsUiState
}
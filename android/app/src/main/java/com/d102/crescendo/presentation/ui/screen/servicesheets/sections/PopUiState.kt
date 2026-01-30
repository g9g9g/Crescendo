package com.d102.crescendo.presentation.ui.screen.servicesheets.sections

sealed interface PopUiState {
    data object Loading : PopUiState
    data class Success(val count: Int) : PopUiState
    data class Error(val message: String?) : PopUiState
}
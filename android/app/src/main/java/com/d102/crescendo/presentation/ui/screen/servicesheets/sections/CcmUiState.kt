package com.d102.crescendo.presentation.ui.screen.servicesheets.sections

sealed interface CcmUiState {
    data object Loading : CcmUiState
    data class Success(val count: Int) : CcmUiState
    data class Error(val message: String?) : CcmUiState
}
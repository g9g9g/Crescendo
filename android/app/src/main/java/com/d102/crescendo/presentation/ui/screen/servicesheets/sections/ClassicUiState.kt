package com.d102.crescendo.presentation.ui.screen.servicesheets.sections

sealed interface ClassicUiState {
    object Loading : ClassicUiState
    data class Success(val count: Int) : ClassicUiState
    data class Error(val message: String?) : ClassicUiState
}
package com.d102.crescendo.presentation.ui.screen.servicesheets.sections

sealed interface NewageUiState {
    data object Loading : NewageUiState
    data class Success(val count: Int) : NewageUiState
    data class Error(val message: String?) : NewageUiState
}
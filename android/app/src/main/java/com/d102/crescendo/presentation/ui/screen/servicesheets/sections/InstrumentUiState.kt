package com.d102.crescendo.presentation.ui.screen.servicesheets.sections

sealed interface InstrumentUiState {
    object Loading : InstrumentUiState
    data class Success(val count: Int) : InstrumentUiState
    data class Error(val message: String?) : InstrumentUiState
}
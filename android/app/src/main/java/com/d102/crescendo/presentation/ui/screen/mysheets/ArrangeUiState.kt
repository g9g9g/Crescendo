package com.d102.crescendo.presentation.ui.screen.mysheets

sealed interface ArrangeUiState {
    object Idle : ArrangeUiState
    object Loading : ArrangeUiState
    object Success : ArrangeUiState
    data class Error(val message: String) : ArrangeUiState
}
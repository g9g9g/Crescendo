package com.d102.crescendo.presentation.ui.screen.servicesheets.sections

sealed interface LevelUiState {
    object Loading : LevelUiState
    data class Success(val count: Int) : LevelUiState
    data class Error(val message: String?) : LevelUiState
}
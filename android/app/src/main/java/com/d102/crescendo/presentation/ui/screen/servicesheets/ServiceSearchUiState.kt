package com.d102.crescendo.presentation.ui.screen.servicesheets

sealed interface ServiceSearchUiState {
    data object Loading : ServiceSearchUiState
    data class Success(
        val totalCount: Int,
        val loadedCount: Int
    ) : ServiceSearchUiState
    data class Error(val message: String?) : ServiceSearchUiState
}

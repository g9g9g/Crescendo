package com.d102.crescendo.presentation.ui.screen.servicesheets

import com.d102.crescendo.domain.model.sheet.ServiceSheetDetail

sealed interface ServiceSheetDetailUiState {
    data object Idle : ServiceSheetDetailUiState
    data object Loading : ServiceSheetDetailUiState
    data class Success(val data: ServiceSheetDetail) : ServiceSheetDetailUiState
    data class Error(val message: String) : ServiceSheetDetailUiState
}
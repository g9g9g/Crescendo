package com.d102.crescendo.presentation.ui.screen.mysheets

import com.d102.crescendo.domain.model.sheet.MySheetDetail

/**
 * 내 악보 상세 UI 상태
 */
sealed interface MySheetDetailUiState {
    data object Loading : MySheetDetailUiState
    data class Success(val sheetDetail: MySheetDetail) : MySheetDetailUiState
    data class Error(val message: String) : MySheetDetailUiState
}
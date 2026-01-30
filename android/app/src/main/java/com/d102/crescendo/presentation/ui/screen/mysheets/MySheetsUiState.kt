package com.d102.crescendo.presentation.ui.screen.mysheets

import com.d102.crescendo.domain.model.sheet.MySheet

/**
 * 내 악보 화면의 UI 상태
 */
sealed interface MySheetsUiState {
    /**
     * 로딩 중인 상태
     */
    object Loading : MySheetsUiState

    /**
     * 데이터 로딩에 성공한 상태
     * @property sheetList 내 악보 목록
     */
    data class Success(
        val totalCount: Int,
        val sheetList: List<MySheet> = emptyList()
    ) : MySheetsUiState

    /**
     * 에러가 발생한 상태
     * @property message 에러 메시지
     */
    data class Error(
        val message: String? = null
    ) : MySheetsUiState
}
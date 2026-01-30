package com.d102.crescendo.presentation.ui.screen.practice

// Practice 스크린의 상태
sealed interface PracticeUiState {
    data object Loading : PracticeUiState
    // 성공 시 S3 XML 주소와 시작 마디를 전달
    // TODO: 필요한 정보 더 있는지 확인
    data class Success(
        val xmlUrl: String,
        val startMeasure: Int
    ) : PracticeUiState
    data class Error(val message: String) : PracticeUiState
}
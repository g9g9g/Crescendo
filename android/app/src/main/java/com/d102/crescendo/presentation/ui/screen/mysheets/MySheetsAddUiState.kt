package com.d102.crescendo.presentation.ui.screen.mysheets

sealed interface MySheetsAddUiState {
    data object Idle : MySheetsAddUiState
    data object FileUploading : MySheetsAddUiState
    data class FileUploadSuccess(val fileUrl: String) : MySheetsAddUiState
    data class FileUploadError(val message: String) : MySheetsAddUiState
    data object SheetRegistering : MySheetsAddUiState
    data object SheetRegistrationSuccess : MySheetsAddUiState
    data class SheetRegistrationError(val message: String) : MySheetsAddUiState
}
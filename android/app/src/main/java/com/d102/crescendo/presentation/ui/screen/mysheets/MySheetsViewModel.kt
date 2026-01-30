package com.d102.crescendo.presentation.ui.screen.mysheets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.domain.usecase.sheet.DeleteUserSheetUseCase
import com.d102.crescendo.domain.usecase.sheet.SearchMySheetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MySheetsViewModel"

@HiltViewModel
class MySheetsViewModel @Inject constructor(
    private val searchMySheetsUseCase: SearchMySheetsUseCase,
    private val deleteUserSheetUseCase: DeleteUserSheetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MySheetsUiState>(MySheetsUiState.Loading)
    val uiState: StateFlow<MySheetsUiState> = _uiState.asStateFlow()

    init {
        // 화면 진입 시 전체 조회
        loadAllMySheets()
    }

    /**
     * 전체 악보 목록 로드 (초기 로드)
     */
    private fun loadAllMySheets() {
        searchMySheets()
    }

    /**
     * 내 악보 검색 (필터 적용)
     */
    fun applyFilters(
        genreId: Int? = null,
        tierCode: String? = null,
        instrumentId: Int? = null,
        sourceType: String? = null
    ) {
        searchMySheets(
            genreId = genreId,
            tierCode = tierCode,
            instrumentId = instrumentId,
            sourceType = sourceType
        )
    }

    /**
     * 내 악보 검색 API 호출
     */
    private fun searchMySheets(
        query: String? = null,
        genreId: Int? = null,
        tierCode: String? = null,
        instrumentId: Int? = null,
        sourceType: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = MySheetsUiState.Loading
            Log.d(
                TAG,
                "내 악보 검색 파라미터 query=$query, genreId=$genreId, tierCode=$tierCode, instrumentId=$instrumentId, sourceType=$sourceType"
            )

            val result = searchMySheetsUseCase(
                query = query,
                genreId = genreId,
                tierCode = tierCode,
                instrumentId = instrumentId,
                sourceType = sourceType
            )

            result.fold(
                onSuccess = { searchResult ->
                    Log.d(TAG, "내 악보 검색 성공 총 갯수: ${searchResult.totalCount}")
                    _uiState.value = MySheetsUiState.Success(
                        totalCount = searchResult.totalCount,
                        sheetList = searchResult.sheetList
                    )
                },
                onFailure = { exception ->
                    Log.e(TAG, "내 악보 검색 실패 에러: ${exception.message}")
                    _uiState.value = MySheetsUiState.Error(
                        exception.message ?: "악보 검색 실패"
                    )
                }
            )
        }
    }

    /**
     * 검색 초기화 및 전체 조회
     */
    fun clearSearch() {
        loadAllMySheets()
    }

    /**
     * 내 악보 삭제
     *
     * @param userSheetId 삭제할 사용자 악보 ID
     * @param onResult 삭제 성공 여부 콜백 (true: 성공, false: 실패)
     */
    fun deleteMySheet(
        userSheetId: Int,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            Log.d(TAG, "내 악보 삭제 요청 userSheetId=$userSheetId")

            val result = deleteUserSheetUseCase(userSheetId)

            result.fold(
                onSuccess = {
                    Log.d(TAG, "내 악보 삭제 성공 userSheetId=$userSheetId")
                    // 삭제 후 목록 다시 조회
                    loadAllMySheets()
                    onResult(true)
                },
                onFailure = { e ->
                    Log.e(TAG, "내 악보 삭제 실패 userSheetId=$userSheetId, message=${e.message}")
                    onResult(false)
                }
            )
        }
    }
}
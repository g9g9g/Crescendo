package com.d102.crescendo.presentation.ui.screen.mysheets

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.text.input.TextFieldValue
import com.d102.crescendo.domain.model.sheet.MySheet
import com.d102.crescendo.domain.repository.SheetRepository
import com.d102.crescendo.domain.usecase.sheet.GetMySheetSearchSuggestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MySheetSearchVM"

@OptIn(FlowPreview::class)
@HiltViewModel
class MySheetSearchViewModel @Inject constructor(
    private val sheetRepository: SheetRepository,
    private val getMySheetSearchSuggestionsUseCase: GetMySheetSearchSuggestionsUseCase
) : ViewModel() {

    // 검색어 입력 필드
    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery: StateFlow<TextFieldValue> = _searchQuery.asStateFlow()

    // 자동완성 제안 목록
    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    // 검색 결과 리스트
    private val _searchResults = MutableStateFlow<List<MySheet>>(emptyList())
    val searchResults: StateFlow<List<MySheet>> = _searchResults.asStateFlow()

    // 검색 모드 여부
    private val _isSearchMode = MutableStateFlow(false)
    val isSearchMode: StateFlow<Boolean> = _isSearchMode.asStateFlow()

    // 🔥 중복 검색 / TextField 역주입 방지 플래그
    private var isExecutingSearch = false

    init {
        // 검색어 입력 시 자동완성
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collectLatest { query ->
                    // 검색 결과 모드거나 검색 실행 중이면 자동완성 안 띄움
                    if (_isSearchMode.value || isExecutingSearch) return@collectLatest

                    if (query.text.isNotBlank()) {
                        fetchSuggestions(query.text)
                    } else {
                        _searchSuggestions.value = emptyList()
                    }
                }
        }
    }

    /**
     * 검색어 업데이트
     */
    fun updateSearchQuery(newValue: TextFieldValue) {
        // 🔥 검색 실행 중에 TextField에서 역으로 들어오는 값 무시
        if (isExecutingSearch) return

        _searchQuery.value = newValue

        // 검색어가 비어있으면 검색 모드 종료
        if (newValue.text.isEmpty()) {
            _isSearchMode.value = false
            _searchResults.value = emptyList()
        }
    }

    /**
     * 자동완성 제안 가져오기
     */
    private suspend fun fetchSuggestions(query: String) {
        getMySheetSearchSuggestionsUseCase(query)
            .onSuccess { suggestions ->
                _searchSuggestions.value = suggestions
                Log.d(TAG, "자동완성 제안: $suggestions")
            }
            .onFailure { e ->
                Log.e(TAG, "자동완성 제안 실패", e)
                _searchSuggestions.value = emptyList()
            }
    }

    /**
     * 자동완성 제안 선택
     * 1) UI(TextField)에 먼저 반영
     * 2) 그 다음 검색 실행
     */
    fun selectSuggestion(suggestion: String) {
        // 자동완성 숨기기
        _searchSuggestions.value = emptyList()

        // 🔥 먼저 검색 실행 플래그 올려두기
        isExecutingSearch = true

        // 텍스트필드 값 먼저 바꾸기 (커서는 맨 뒤로)
        _searchQuery.value = TextFieldValue(
            text = suggestion,
            selection = TextRange(suggestion.length)
        )

        // 검색 모드 진입
        _isSearchMode.value = true

        // 실제 검색 실행
        viewModelScope.launch {
            runSearch(suggestion)
            isExecutingSearch = false
        }
    }

    /**
     * 검색 실행 (사용자 직접 입력용)
     */
    fun executeSearch() {
        val query = _searchQuery.value.text.trim()
        if (query.isEmpty()) return

        // 결과 모드 + 자동완성 닫기
        _isSearchMode.value = true
        _searchSuggestions.value = emptyList()

        if (isExecutingSearch) return
        isExecutingSearch = true

        viewModelScope.launch {
            runSearch(query)
            isExecutingSearch = false
        }
    }

    /**
     * 실제 검색 로직 공통 함수
     */
    private suspend fun runSearch(query: String) {
        Log.d(TAG, "내 악보 검색 실행: query=$query")

        sheetRepository.searchMySheets(
            query = query,
            genreId = null,
            tierCode = null,
            instrumentId = null,
            sourceType = null
        )
            .onSuccess { result ->
                _searchResults.value = result.sheetList
                Log.d(
                    TAG,
                    "내 악보 검색 성공: totalCount=${result.totalCount}, items=${result.sheetList.size}"
                )
            }
            .onFailure { e ->
                Log.e(TAG, "내 악보 검색 실패", e)
                _searchResults.value = emptyList()
            }
    }

    /**
     * 검색 초기화
     */
    fun clearSearch() {
        _searchQuery.value = TextFieldValue("")
        _searchResults.value = emptyList()
        _searchSuggestions.value = emptyList()
        _isSearchMode.value = false
        isExecutingSearch = false
    }

    /**
     * 자동완성 제안 숨기기
     */
    fun clearSuggestions() {
        _searchSuggestions.value = emptyList()
    }
}

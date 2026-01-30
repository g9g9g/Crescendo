package com.d102.crescendo.presentation.ui.screen.servicesheets

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.data.local.SearchHistoryDataSource
import com.d102.crescendo.domain.model.sheet.PopularSheet
import com.d102.crescendo.domain.model.sheet.ServiceSheet
import com.d102.crescendo.domain.model.sheet.TrendingSearch
import com.d102.crescendo.domain.usecase.sheet.GetPopularSheetsUseCase
import com.d102.crescendo.domain.usecase.sheet.GetRecommendedSearchKeywordsUseCase
import com.d102.crescendo.domain.usecase.sheet.GetSearchSuggestionsUseCase
import com.d102.crescendo.domain.usecase.sheet.GetTrendingSearchesUseCase
import com.d102.crescendo.domain.usecase.sheet.SearchServiceSheetsUseCase
import com.d102.crescendo.presentation.ui.screen.servicesheets.sections.CcmUiState
import com.d102.crescendo.presentation.ui.screen.servicesheets.sections.ClassicUiState
import com.d102.crescendo.presentation.ui.screen.servicesheets.sections.InstrumentUiState
import com.d102.crescendo.presentation.ui.screen.servicesheets.sections.KidsUiState
import com.d102.crescendo.presentation.ui.screen.servicesheets.sections.LevelUiState
import com.d102.crescendo.presentation.ui.screen.servicesheets.sections.NewageUiState
import com.d102.crescendo.presentation.ui.screen.servicesheets.sections.OstUiState
import com.d102.crescendo.presentation.ui.screen.servicesheets.sections.PopUiState
import com.d102.crescendo.presentation.ui.screen.servicesheets.sections.SortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ServiceSheetSearchViewM"

@OptIn(FlowPreview::class)
@HiltViewModel
class ServiceSheetSearchViewModel @Inject constructor(
    private val searchServiceSheets: SearchServiceSheetsUseCase,
    private val getPopularSheetsUseCase: GetPopularSheetsUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val getRecommendedSearchKeywordsUseCase: GetRecommendedSearchKeywordsUseCase,
    private val searchHistoryDataSource: SearchHistoryDataSource,
    private val getTrendingSearchesUseCase: GetTrendingSearchesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ServiceSearchUiState>(ServiceSearchUiState.Loading)
    val uiState: StateFlow<ServiceSearchUiState> = _uiState

    // 전체 리스트
    private val _allItems = MutableStateFlow<List<ServiceSheet>>(emptyList())
    val allItems: StateFlow<List<ServiceSheet>> = _allItems

    // 인기 악보 리스트
    private val _popularSheets = MutableStateFlow<List<PopularSheet>>(emptyList())
    val popularSheets: StateFlow<List<PopularSheet>> = _popularSheets

    // 검색어 자동완성
    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions

    // 추천 검색어 (홈 상단에 노출)
    private val _recommendedKeywords = MutableStateFlow<List<String>>(emptyList())
    val recommendedKeywords: StateFlow<List<String>> = _recommendedKeywords

    // 검색어 (TextFieldValue로 커서 위치 제어)
    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery: StateFlow<TextFieldValue> = _searchQuery

    // 검색 결과
    private val _searchResults = MutableStateFlow<List<ServiceSheet>>(emptyList())
    val searchResults: StateFlow<List<ServiceSheet>> = _searchResults

    // 실시간 인기 검색어 키워드 (Top 10)
    private val _trendingSearches = MutableStateFlow<List<String>>(emptyList())
    val trendingSearches: StateFlow<List<String>> = _trendingSearches

    // 생성 시각 (예: "2025-11-17T03:02:00")
    private val _trendingGeneratedAt = MutableStateFlow<String?>(null)
    val trendingGeneratedAt: StateFlow<String?> = _trendingGeneratedAt

    // 검색 모드 여부
    private val _isSearchMode = MutableStateFlow(false)
    val isSearchMode: StateFlow<Boolean> = _isSearchMode

    // 클래식 악보 리스트
    private val _classicSheets = MutableStateFlow<List<ServiceSheet>>(emptyList())
    val classicSheets: StateFlow<List<ServiceSheet>> = _classicSheets

    // 클래식 악보 UI 상태
    private val _classicUiState = MutableStateFlow<ClassicUiState>(ClassicUiState.Loading)
    val classicUiState: StateFlow<ClassicUiState> = _classicUiState

    // 팝 악보 리스트
    private val _popSheets = MutableStateFlow<List<ServiceSheet>>(emptyList())
    val popSheets: StateFlow<List<ServiceSheet>> = _popSheets

    // 팝 악보 UI 상태
    private val _popUiState = MutableStateFlow<PopUiState>(PopUiState.Loading)
    val popUiState: StateFlow<PopUiState> = _popUiState

    // OST 악보 리스트
    private val _ostSheets = MutableStateFlow<List<ServiceSheet>>(emptyList())
    val ostSheets: StateFlow<List<ServiceSheet>> = _ostSheets

    // OST 악보 UI 상태
    private val _ostUiState = MutableStateFlow<OstUiState>(OstUiState.Loading)
    val ostUiState: StateFlow<OstUiState> = _ostUiState


    // 뉴에이지 악보 리스트
    private val _newageSheets = MutableStateFlow<List<ServiceSheet>>(emptyList())
    val newageSheets: StateFlow<List<ServiceSheet>> = _newageSheets

    // 뉴에이지 악보 UI 상태
    private val _newageUiState = MutableStateFlow<NewageUiState>(NewageUiState.Loading)
    val newageUiState: StateFlow<NewageUiState> = _newageUiState


    // 동요 악보 리스트
    private val _kidsSheets = MutableStateFlow<List<ServiceSheet>>(emptyList())
    val kidsSheets: StateFlow<List<ServiceSheet>> = _kidsSheets

    // 동요 악보 UI 상태
    private val _kidsUiState = MutableStateFlow<KidsUiState>(KidsUiState.Loading)
    val kidsUiState: StateFlow<KidsUiState> = _kidsUiState

    // CCM 악보 리스트
    private val _ccmSheets = MutableStateFlow<List<ServiceSheet>>(emptyList())
    val ccmSheets: StateFlow<List<ServiceSheet>> = _ccmSheets

    // CCM 악보 UI 상태
    private val _ccmUiState = MutableStateFlow<CcmUiState>(CcmUiState.Loading)
    val ccmUiState: StateFlow<CcmUiState> = _ccmUiState

    // 정렬 상태 관리
    private val _selectedSort = MutableStateFlow(SortType.LATEST)
    val selectedSort: StateFlow<SortType> = _selectedSort

    private val _showSortDropdown = MutableStateFlow(false)
    val showSortDropdown: StateFlow<Boolean> = _showSortDropdown

    // 선택된 티어 필터
    private val _selectedTierFilter = MutableStateFlow<TierFilter>(TierFilter.ALL)
    val selectedTierFilter: StateFlow<TierFilter> = _selectedTierFilter

    val recentSearches: StateFlow<List<String>> =
        searchHistoryDataSource.searchHistories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 필터링된 악보
    // _allLevelSheets를 별도로 관리하지 않고, allItems를 직접 필터링
    val filteredLevelSheets: StateFlow<List<ServiceSheet>> = combine(
        _allItems,
        _selectedTierFilter
    ) { sheets, filter ->
        when (filter) {
            TierFilter.ALL -> sheets
            TierFilter.BRONZE -> sheets.filter { it.tierCode.equals("bronze", ignoreCase = true) }
            TierFilter.SILVER -> sheets.filter { it.tierCode.equals("silver", ignoreCase = true) }
            TierFilter.GOLD -> sheets.filter { it.tierCode.equals("gold", ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 난이도별 악보 UI 상태
    private val _levelUiState = MutableStateFlow<LevelUiState>(LevelUiState.Loading)
    val levelUiState: StateFlow<LevelUiState> = _levelUiState

    // 선택된 악기 필터
    private val _selectedInstrumentFilter = MutableStateFlow<InstrumentFilter>(InstrumentFilter.ALL)
    val selectedInstrumentFilter: StateFlow<InstrumentFilter> = _selectedInstrumentFilter

    // 필터링된 악기별 악보
    val filteredInstrumentSheets: StateFlow<List<ServiceSheet>> = combine(
        _allItems,
        _selectedInstrumentFilter
    ) { sheets, filter ->
        when (filter) {
            InstrumentFilter.ALL -> sheets
            InstrumentFilter.PIANO -> sheets.filter { it.instrumentId == 1 }
            InstrumentFilter.GUITAR -> sheets.filter { it.instrumentId == 2 }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 악기별 악보 UI 상태
    private val _instrumentUiState = MutableStateFlow<InstrumentUiState>(InstrumentUiState.Loading)
    val instrumentUiState: StateFlow<InstrumentUiState> = _instrumentUiState

    // 검색 실행 중 플래그 추가
    private var isExecutingSearch = false

    private var page: Int = 1
    private val size: Int = 1000
    private var totalCount: Int = 0
    private var isLoading = false

    // 캐시 관리
    private var lastLoadedTime = 0L
    private val CACHE_DURATION = 10 * 60 * 1000L // 10분

    private var isPopularSheetsLoaded = false // 인기 악보 로드 여부

    init {
        // 검색어 변경 감지 - debounce 적용 (300ms)
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collect { textFieldValue ->
                    // 검색모드일 때나 검색 실행 중일 때는 자동완성 호출 금지
                    if (_isSearchMode.value || isExecutingSearch) return@collect

                    val query = textFieldValue.text
                    if (query.isNotBlank()) {
                        fetchSearchSuggestions(query)
                    } else {
                        _searchSuggestions.value = emptyList()
                    }
                }
        }
    }

    fun clearAllSearchHistory() {
        viewModelScope.launch {
            searchHistoryDataSource.clearAll()
        }
    }

    fun removeSearchHistory(keyword: String) {
        viewModelScope.launch {
            searchHistoryDataSource.removeQuery(keyword)
        }
    }

    /**
     * 전체 목록이 준비됐는지 보장하는 헬퍼
     * - 이미 캐시가 유효하면 바로 true
     * - 아니면 loadIfNeeded() 호출 후, uiState가 Success/Error 될 때까지 대기
     * - 성공이면 true, 실패면 false
     */
    private suspend fun ensureAllItemsLoaded(): Boolean {
        val now = System.currentTimeMillis()
        val cacheAge = now - lastLoadedTime

        // 유효한 캐시가 있으면 바로 사용
        if (_allItems.value.isNotEmpty() && cacheAge <= CACHE_DURATION) {
            Log.d(TAG, "ensureAllItemsLoaded: 캐시 사용 (${_allItems.value.size}개)")
            return true
        }

        Log.d(TAG, "ensureAllItemsLoaded: 전체 목록 새로 로드 필요")

        // 이미 로딩 중이면 결과 대기
        if (_uiState.value is ServiceSearchUiState.Loading) {
            return when (
                uiState.first { it is ServiceSearchUiState.Success || it is ServiceSearchUiState.Error }
            ) {
                is ServiceSearchUiState.Success -> {
                    Log.d(TAG, "ensureAllItemsLoaded: 전체 목록 로드 성공 (${_allItems.value.size}개)")
                    true
                }
                is ServiceSearchUiState.Error -> {
                    Log.e(TAG, "ensureAllItemsLoaded: 전체 목록 로드 실패")
                    false
                }
                else -> false
            }
        }

        // 새로 로드 시작
        initialLoad()

        // 로드 완료 대기
        return when (
            uiState.first { it is ServiceSearchUiState.Success || it is ServiceSearchUiState.Error }
        ) {
            is ServiceSearchUiState.Success -> {
                Log.d(TAG, "ensureAllItemsLoaded: 전체 목록 로드 성공 (${_allItems.value.size}개)")
                true
            }
            is ServiceSearchUiState.Error -> {
                Log.e(TAG, "ensureAllItemsLoaded: 전체 목록 로드 실패")
                false
            }
            else -> false
        }
    }

    /**
     * 악기별 악보 로드 (allItems 재사용)
     */
    fun loadInstrumentSheets() {
        viewModelScope.launch {
            _instrumentUiState.value = InstrumentUiState.Loading
            Log.d(TAG, "악기별 악보 로드 시작 (allItems 기반)")

            val ok = ensureAllItemsLoaded()
            if (!ok) {
                val msg = (uiState.value as? ServiceSearchUiState.Error)?.message
                _instrumentUiState.value = InstrumentUiState.Error(msg)
                return@launch
            }

            _instrumentUiState.value = InstrumentUiState.Success(_allItems.value.size)
            Log.d(TAG, "악기별 악보 로드 완료: ${_allItems.value.size}개")
        }
    }

    /**
     * 악기 필터 변경
     */
    fun updateInstrumentFilter(filter: InstrumentFilter) {
        _selectedInstrumentFilter.value = filter
        Log.d(TAG, "악기 필터 변경: $filter")
    }

    /**
     * 난이도별 악보 로드 (allItems 재사용)
     */
    fun loadLevelSheets() {
        viewModelScope.launch {
            _levelUiState.value = LevelUiState.Loading

            // allItems가 비어있으면 먼저 로드
            if (_allItems.value.isEmpty()) {
                Log.d(TAG, "난이도별 악보 - allItems 먼저 로드")
                loadIfNeeded()
            } else {
                Log.d(TAG, "난이도별 악보 - allItems 재사용: ${_allItems.value.size}개")
            }

            // allItems 로드 완료 대기
            _uiState.collect { state ->
                if (state is ServiceSearchUiState.Success) {
                    _levelUiState.value = LevelUiState.Success(_allItems.value.size)
                    Log.d(TAG, "난이도별 악보 로드 완료: ${_allItems.value.size}개")
                    return@collect
                } else if (state is ServiceSearchUiState.Error) {
                    _levelUiState.value = LevelUiState.Error(state.message)
                    return@collect
                }
            }
        }
    }

    /**
     * 티어 필터 변경
     */
    fun updateTierFilter(filter: TierFilter) {
        _selectedTierFilter.value = filter
    }

    fun updateSortType(sortType: SortType) {
        _selectedSort.value = sortType
        _showSortDropdown.value = false
    }

    fun toggleSortDropdown() {
        _showSortDropdown.value = !_showSortDropdown.value
        Log.d("ServiceSheetSearchViewM", "정렬 드롭다운 토글: ${_showSortDropdown.value}")
    }

    fun hideSortDropdown() {
        _showSortDropdown.value = false
    }

    fun resetSortType() {
        _selectedSort.value = SortType.LATEST
        _showSortDropdown.value = false
        Log.d(TAG, "정렬 상태 초기화: LATEST")
    }

    /**
     * CCM 악보 조회 (genreId = 6)
     */
    fun loadCcmSheets() {
        viewModelScope.launch {
            _ccmUiState.value = CcmUiState.Loading
            Log.d(TAG, "CCM 악보 로드 시작 (allItems 기반)")

            val ok = ensureAllItemsLoaded()
            if (!ok) {
                val msg = (uiState.value as? ServiceSearchUiState.Error)?.message
                _ccmUiState.value = CcmUiState.Error(msg)
                return@launch
            }

            val ccms = _allItems.value.filter { it.genreId == 6 }
            _ccmSheets.value = ccms
            _ccmUiState.value = CcmUiState.Success(ccms.size)

            Log.d(TAG, "CCM 악보 로드 성공 (필터링): ${ccms.size}개")
        }
    }


    /**
     * 동요 악보 조회 (genreId = 5)
     */
    fun loadKidsSheets() {
        viewModelScope.launch {
            _kidsUiState.value = KidsUiState.Loading
            Log.d(TAG, "동요 악보 로드 시작 (allItems 기반)")

            val ok = ensureAllItemsLoaded()
            if (!ok) {
                val msg = (uiState.value as? ServiceSearchUiState.Error)?.message
                _kidsUiState.value = KidsUiState.Error(msg)
                return@launch
            }

            val kids = _allItems.value.filter { it.genreId == 5 }
            _kidsSheets.value = kids
            _kidsUiState.value = KidsUiState.Success(kids.size)

            Log.d(TAG, "동요 악보 로드 성공 (필터링): ${kids.size}개")
        }
    }


    /**
     * 뉴에이지 악보 조회 (genreId = 3)
     */
    fun loadNewageSheets() {
        viewModelScope.launch {
            _newageUiState.value = NewageUiState.Loading
            Log.d(TAG, "뉴에이지 악보 로드 시작 (allItems 기반)")

            val ok = ensureAllItemsLoaded()
            if (!ok) {
                val msg = (uiState.value as? ServiceSearchUiState.Error)?.message
                _newageUiState.value = NewageUiState.Error(msg)
                return@launch
            }

            val newages = _allItems.value.filter { it.genreId == 3 }
            _newageSheets.value = newages
            _newageUiState.value = NewageUiState.Success(newages.size)

            Log.d(TAG, "뉴에이지 악보 로드 성공 (필터링): ${newages.size}개")
        }
    }


    /**
     * OST 악보 조회 (genreId = 4)
     */
    fun loadOstSheets() {
        viewModelScope.launch {
            _ostUiState.value = OstUiState.Loading
            Log.d(TAG, "OST 악보 로드 시작 (allItems 기반)")

            val ok = ensureAllItemsLoaded()
            if (!ok) {
                val msg = (uiState.value as? ServiceSearchUiState.Error)?.message
                _ostUiState.value = OstUiState.Error(msg)
                return@launch
            }

            val osts = _allItems.value.filter { it.genreId == 4 }
            _ostSheets.value = osts
            _ostUiState.value = OstUiState.Success(osts.size)

            Log.d(TAG, "OST 악보 로드 성공 (필터링): ${osts.size}개")
        }
    }



    /**
     * 팝 악보 조회 (genreId = 2)
     */
    fun loadPopSheets() {
        viewModelScope.launch {
            _popUiState.value = PopUiState.Loading
            Log.d(TAG, "팝 악보 로드 시작 (allItems 기반)")

            val ok = ensureAllItemsLoaded()
            if (!ok) {
                val msg = (uiState.value as? ServiceSearchUiState.Error)?.message
                _popUiState.value = PopUiState.Error(msg)
                return@launch
            }

            val pops = _allItems.value.filter { it.genreId == 2 }
            _popSheets.value = pops
            _popUiState.value = PopUiState.Success(pops.size)

            Log.d(TAG, "팝 악보 로드 성공 (필터링): ${pops.size}개")
        }
    }

    /**
     * 검색어 업데이트
     */
    fun updateSearchQuery(newValue: TextFieldValue) {
        // 검색 실행 중일 때는 TextField의 값 변경을 무시
        // (자동검색어 선택으로 설정된 값이 덮어씌워지는 것을 방지)
        if (isExecutingSearch) return

        val prev = _searchQuery.value
        _searchQuery.value = newValue
        // 텍스트가 바뀌는 순간, 결과 리스트는 숨기고 자동완성 모드로 전환
        if (newValue.text != prev.text) {
            _isSearchMode.value = false
        }
    }

    /**
     * 자동완성 검색어 선택 (커서를 텍스트 끝으로 이동)
     */
    fun selectSuggestion(suggestion: String) {
        _searchSuggestions.value = emptyList()

        // 검색어를 먼저 업데이트하여 UI에 즉시 반영
        _searchQuery.value = TextFieldValue(
            text = suggestion,
            selection = TextRange(suggestion.length)
        )

        // 그 후에 검색 모드와 플래그 설정
        _isSearchMode.value = true
        isExecutingSearch = true

        viewModelScope.launch {
            kotlinx.coroutines.yield()

            searchHistoryDataSource.addQuery(suggestion)

            _uiState.value = ServiceSearchUiState.Loading
            searchServiceSheets(
                q = suggestion, genreId = null, instrumentId = null, tierCode = null,
                page = 1, size = 1000
            ).onSuccess { pageResult ->
                _searchResults.value = pageResult.sheets
                _uiState.value = ServiceSearchUiState.Success(
                    totalCount = pageResult.totalCount,
                    loadedCount = pageResult.sheets.size
                )
            }.onFailure { e ->
                _searchResults.value = emptyList()
                _uiState.value = ServiceSearchUiState.Error(e.message)
            }

            isExecutingSearch = false
        }
    }

    /**
     * 검색 실행
     */
    fun executeSearch() {
        val query = _searchQuery.value.text
        if (query.isBlank()) {
            isExecutingSearch = false
            return
        }

        // 결과 모드 + 자동완성 닫기
        _isSearchMode.value = true
        _searchSuggestions.value = emptyList()
        _uiState.value = ServiceSearchUiState.Loading
        isExecutingSearch = true

        viewModelScope.launch {
            searchHistoryDataSource.addQuery(query)

            searchServiceSheets(
                q = query,
                genreId = null,
                instrumentId = null,
                tierCode = null,
                page = 1,
                size = 1000
            ).onSuccess { pageResult ->
                _searchResults.value = pageResult.sheets
                _uiState.value = ServiceSearchUiState.Success(
                    totalCount = pageResult.totalCount,
                    loadedCount = pageResult.sheets.size
                )
            }.onFailure { e ->
                _searchResults.value = emptyList()
                _uiState.value = ServiceSearchUiState.Error(e.message)
            }

            isExecutingSearch = false
        }
    }

    /**
     * 검색 모드 종료 (검색 결과 초기화)
     */
    fun clearSearch() {
        _isSearchMode.value = false
        _searchResults.value = emptyList()
        _searchQuery.value = TextFieldValue("")
        isExecutingSearch = false
        _uiState.value = ServiceSearchUiState.Success(
            totalCount = totalCount,
            loadedCount = _allItems.value.size
        )
    }

    /**
     * 실시간 인기 검색어 Top 10 조회
     */
    fun loadTrendingSearches() {
        viewModelScope.launch {
            getTrendingSearchesUseCase()
                .onSuccess { trending ->
                    _trendingSearches.value = trending.keywords!!
                    _trendingGeneratedAt.value = trending.generatedAt

                    Log.d(
                        TAG,
                        "실시간 인기 검색어 로드 성공: generatedAt=${trending.generatedAt}, count=${trending.keywords.size}"
                    )
                }
                .onFailure { e ->
                    Log.e(TAG, "실시간 인기 검색어 로드 실패: ${e.message}")
                    _trendingSearches.value = emptyList()
                    _trendingGeneratedAt.value = null
                }
        }
    }


    /**
     * 자동완성 검색어 가져오기
     */
    private fun fetchSearchSuggestions(query: String) {
        viewModelScope.launch {
            getSearchSuggestionsUseCase(query)
                .onSuccess { suggestions ->
                    _searchSuggestions.value = suggestions
                    Log.d(TAG, "자동완성 검색어 조회 성공: ${suggestions.size}개")
                }
                .onFailure { e ->
                    _searchSuggestions.value = emptyList()
                    Log.e(TAG, "자동완성 검색어 조회 실패: ${e.message}")
                }
        }
    }

    /**
     * 자동완성 검색어 숨기기
     */
    fun clearSuggestions() {
        _searchSuggestions.value = emptyList()
    }

    /**
     * 필요한 경우에만 로드
     * - 인기 악보: 최초 1회만
     * - 전체 목록: 10분마다
     */
    fun loadIfNeeded() {
        val now = System.currentTimeMillis()
        val cacheAge = now - lastLoadedTime

        // 1) 캐시가 살아있으면 UIState만 Success로 세팅 (API 호출 X)
        if (_allItems.value.isNotEmpty() && cacheAge <= CACHE_DURATION) {
            _uiState.value = ServiceSearchUiState.Success(
                totalCount = totalCount,
                loadedCount = _allItems.value.size
            )
        }

        viewModelScope.launch {
            // 인기 악보: 최초 1회만 로드
            if (!isPopularSheetsLoaded) {
                Log.d(TAG, "인기 악보 조회")
                getPopularSheetsUseCase().onSuccess { sheets ->
                    _popularSheets.value = sheets
                    isPopularSheetsLoaded = true
                }.onFailure { e ->
                    _popularSheets.value = emptyList()
                }
            }

            // 전체 목록: 데이터가 없거나 10분 경과 시 로드
            if (_allItems.value.isEmpty() || cacheAge > CACHE_DURATION) {
                Log.d(TAG, "전체 목록 조회 시작")
                initialLoad()
                lastLoadedTime = now
            }
        }
    }

    /** 전체 목록 초기 로드 (q=null → 전체) */
    fun initialLoad() {
        page = 1
        totalCount = 0
        _allItems.value = emptyList()
        _uiState.value = ServiceSearchUiState.Loading

        viewModelScope.launch {
            loadNext()
        }
    }

    /** 다음 페이지를 불러와 전체 리스트에 누적 */
    fun loadNext() {
        if (isLoading) return
        if (_allItems.value.size >= totalCount && totalCount != 0) return

        isLoading = true
        viewModelScope.launch {
            searchServiceSheets(
                q = "",
                genreId = null,
                instrumentId = null,
                tierCode = null,
                page = page,
                size = size
            ).onSuccess { pageResult ->
                totalCount = pageResult.totalCount
                val merged = _allItems.value + pageResult.sheets
                _allItems.value = merged.distinctBy { it.sheetId }
                _uiState.value = ServiceSearchUiState.Success(
                    totalCount = totalCount,
                    loadedCount = _allItems.value.size
                )
                page += 1
            }.onFailure { e ->
                _uiState.value = ServiceSearchUiState.Error(e.message)
            }
            isLoading = false
        }
    }

    /**
     * 클래식 악보 조회 (genreId = 1)
     */
    fun loadClassicSheets() {
        viewModelScope.launch {
            _classicUiState.value = ClassicUiState.Loading
            Log.d(TAG, "클래식 악보 로드 시작 (allItems 기반)")

            val ok = ensureAllItemsLoaded()
            if (!ok) {
                val msg = (uiState.value as? ServiceSearchUiState.Error)?.message
                _classicUiState.value = ClassicUiState.Error(msg)
                return@launch
            }

            val classics = _allItems.value.filter { it.genreId == 1 }
            _classicSheets.value = classics
            _classicUiState.value = ClassicUiState.Success(classics.size)

            Log.d(TAG, "클래식 악보 로드 성공 (필터링): ${classics.size}개")
        }
    }

    /**
     * 추천 검색어 로드 (검색 화면 전용)
     * - 이미 불러왔다면 다시 호출하지 않음
     */
//    fun loadRecommendedKeywordsIfNeeded() {
//        // 이미 있으면 호출 안 함
//        if (_recommendedKeywords.value.isNotEmpty()) return
//
//        viewModelScope.launch {
//            getRecommendedSearchKeywordsUseCase()
//                .onSuccess { result ->
//                    _recommendedKeywords.value = result.keywords
//                    android.util.Log.d(TAG, "추천 검색어 조회 성공: ${result.keywords}")
//                }
//                .onFailure { e ->
//                    android.util.Log.e(TAG, "추천 검색어 조회 실패: ${e.message}")
//                }
//        }
//    }

    /** 다시 시도 */
    fun retry() {
        lastLoadedTime = 0L
        initialLoad()
    }
}

enum class TierFilter {
    ALL, BRONZE, SILVER, GOLD
}

enum class InstrumentFilter {
    ALL, PIANO, GUITAR
}
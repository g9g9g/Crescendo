package com.d102.crescendo.presentation.ui.screen.ranking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.domain.usecase.common.GetInstrumentsUseCase
import com.d102.crescendo.domain.usecase.rank.GetRankingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TAG = "Crescendo_RankingViewModel"

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val getRankingsUseCase: GetRankingsUseCase,
    private val getInstrumentsUseCase: GetInstrumentsUseCase,
    private val savedStateHandle: SavedStateHandle // NavHost가 전달한 인자 받기
) : ViewModel() {

    // 상위 20명 랭킹 리스트 상태
    private val _uiState = MutableStateFlow<RankingUiState>(RankingUiState.Loading)
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    // 현재 선택된 악기 탭 상태
    private val _selectedInstrumentId = MutableStateFlow<Int?>(null)
    val selectedInstrumentId: StateFlow<Int?> = _selectedInstrumentId.asStateFlow()

    init {
        val initialInstrumentId: Int? = savedStateHandle.get<Int>("instrumentId")
        if (initialInstrumentId == null) {
            _uiState.value = RankingUiState.Error("악기 정보를 불러오지 못했습니다.")
        } else {
            _selectedInstrumentId.value = initialInstrumentId
            observeInstrumentChanges()
        }
    }

    private fun observeInstrumentChanges() {
        viewModelScope.launch {
            _selectedInstrumentId.collect { id ->
                if (id != null) {
                    loadRankingData(id) // 탭이 바뀌면 API 호출
                }
            }
        }
    }

    /**
     * UseCase를 호출하여 랭킹 목록을 가져옴
     */
    private fun loadRankingData(instrumentId: Int) {
        viewModelScope.launch {
            _uiState.value = RankingUiState.Loading

            // 2개의 API 동시 호출
            val rankingsResult = getRankingsUseCase(instrumentId)
            val instrumentsResult = getInstrumentsUseCase()

            // 결과 조합
            if (rankingsResult.isSuccess && instrumentsResult.isSuccess) {
                _uiState.value = RankingUiState.Success(
                    topRankers = rankingsResult.getOrThrow(),
                    instruments = instrumentsResult.getOrThrow()
                )
            } else {
                val errorMsg = (rankingsResult.exceptionOrNull() ?: instrumentsResult.exceptionOrNull())
                    ?.message ?: "데이터 로드 실패"
                _uiState.value = RankingUiState.Error(errorMsg)
            }
        }
    }

    /**
     * [이벤트] UI에서 악기 탭 클릭 시 호출
     */
    fun onInstrumentSelected(instrumentId: Int) {
        _selectedInstrumentId.value = instrumentId
    }
}
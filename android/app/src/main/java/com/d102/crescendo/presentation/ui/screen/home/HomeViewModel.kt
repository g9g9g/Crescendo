package com.d102.crescendo.presentation.ui.screen.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.domain.usecase.common.GetGenresUseCase
import com.d102.crescendo.domain.usecase.common.GetInstrumentsUseCase
import com.d102.crescendo.domain.usecase.performance.GetRecentPracticeUseCase
import com.d102.crescendo.domain.usecase.sheet.GetPopularSheetsUseCase
import com.d102.crescendo.domain.usecase.sheet.GetTodayRecommendSheetsUseCase
import com.d102.crescendo.domain.usecase.user.GetProfileUseCase
import com.d102.crescendo.util.PracticeUpdateNotifier
import com.d102.crescendo.util.ProfileUpdateNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TAG = "Crescendo_HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getRecentPracticeUseCase: GetRecentPracticeUseCase,
    private val getTodayRecommendSheetsUseCase: GetTodayRecommendSheetsUseCase,
    private val getGenresUseCase: GetGenresUseCase,
    private val getInstrumentsUseCase: GetInstrumentsUseCase,
    private val profileUpdateNotifier: ProfileUpdateNotifier,
    private val practiceUpdateNotifier: PracticeUpdateNotifier,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeScreenData()
        observeUpdates()
    }

    /**
     * Notifier를 모두 구독
     */
    private fun observeUpdates() {
        // 1. "프로필 수정" 신호 구독
        viewModelScope.launch {
            profileUpdateNotifier.events.collect {
                Log.d(TAG, "Profile update notification received. Refreshing...")
                loadHomeScreenData() // 새로고침
            }
        }

        // 2. "연주 완료" 신호 구독
        viewModelScope.launch {
            practiceUpdateNotifier.events.collect {
                Log.d(TAG, "Practice save notification received. Refreshing...")
                loadHomeScreenData() // 새로고침
            }
        }
    }

    fun loadHomeScreenData() {
        viewModelScope.launch {
            if (_uiState.value is HomeUiState.Loading) return@launch
            _uiState.value = HomeUiState.Loading

            val profileResult = getProfileUseCase()
            val recentPracticeResult = getRecentPracticeUseCase()
            val todaySheetsResult = getTodayRecommendSheetsUseCase()
            val genresResult = getGenresUseCase()
            val instrumentsResult = getInstrumentsUseCase()

            Log.d("Crescendo_HomeViewModel", "오늘의 악보 결과: $todaySheetsResult")

            Log.d(TAG, "프로필: $profileResult")
            Log.d(TAG, "최근 연주: $recentPracticeResult")
            Log.d(TAG, "오늘의 추천 악보: $todaySheetsResult")
            Log.d(TAG, "장르: $genresResult")
            Log.d(TAG, "악기: $instrumentsResult")

            if (
                profileResult.isSuccess &&
                recentPracticeResult.isSuccess &&
                todaySheetsResult.isSuccess &&
                genresResult.isSuccess &&
                instrumentsResult.isSuccess
            ) {
                Log.d(TAG, "All UseCases Success")
                _uiState.value = HomeUiState.Success(
                    userProfile = profileResult.getOrThrow(),
                    recentPracticeList = recentPracticeResult.getOrThrow(),
                    todayRecommendSheets = todaySheetsResult.getOrThrow(),
                    genres = genresResult.getOrThrow(),
                    instruments = instrumentsResult.getOrThrow()
                )
            } else {
                Log.e(TAG, "UseCase Failed")
                val errorMessage = (
                        profileResult.exceptionOrNull()
                            ?: recentPracticeResult.exceptionOrNull()
                            ?: todaySheetsResult.exceptionOrNull()
                            ?: genresResult.exceptionOrNull()
                            ?: instrumentsResult.exceptionOrNull()
                        )?.message ?: "홈 화면 로드 실패"

                _uiState.value = HomeUiState.Error(errorMessage)
            }
        }
    }
}
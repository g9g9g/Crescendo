package com.d102.crescendo.presentation.ui.screen.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.domain.repository.TokenRepository
import com.d102.crescendo.domain.usecase.common.GetGenresUseCase
import com.d102.crescendo.domain.usecase.common.GetInstrumentsUseCase
import com.d102.crescendo.domain.usecase.user.GetOnboardingRecommendSheetsUseCase
import com.d102.crescendo.domain.usecase.user.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


private val TAG = "Crescendo_OnboardingViewModel"
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val getGenresUseCase: GetGenresUseCase,
    private val getInstrumentsUseCase: GetInstrumentsUseCase,
    private val getOnboardingRecommendSheetsUseCase: GetOnboardingRecommendSheetsUseCase,
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun onBackClicked() {
        val current = uiState.value
        if (current !is OnboardingUiState.Content) return

        val previousStep = when (current.currentStep) {
            OnboardingStep.SELECT_GENRE -> OnboardingStep.SELECT_GENRE // 첫 페이지면 그대로
            OnboardingStep.SELECT_INSTRUMENT -> OnboardingStep.SELECT_GENRE
            OnboardingStep.SELECT_SHEET_1 -> OnboardingStep.SELECT_INSTRUMENT
            OnboardingStep.SELECT_SHEET_2 -> OnboardingStep.SELECT_SHEET_1
            OnboardingStep.SELECT_SHEET_3 -> OnboardingStep.SELECT_SHEET_2
        }

        // 기존에 쓰는 방식에 맞게 copy 해서 넣어주면 됨
        _uiState.value = current.copy(
            currentStep = previousStep,
            // 필요하면 isNextButtonEnabled 같은 것도 같이 갱신
        )
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            val genresResult = getGenresUseCase()
            val instrumentsResult = getInstrumentsUseCase()

            if (genresResult.isSuccess && instrumentsResult.isSuccess) {
                _uiState.value = OnboardingUiState.Content(
                    genres = genresResult.getOrThrow(),
                    instruments = instrumentsResult.getOrThrow()
                )
            } else {
                val error = genresResult.exceptionOrNull() ?: instrumentsResult.exceptionOrNull()
                Log.e(TAG, "Failed to load initial data", error)
                _uiState.value = OnboardingUiState.Error(error?.message ?: "데이터 로드 실패")
            }
        }



    }

    /** 장르 선택/해제 이벤트 */
    fun onGenreSelected(genreId: Int) {
        if (_uiState.value !is OnboardingUiState.Content) return

        _uiState.update { currentState ->
            val contentState = currentState as OnboardingUiState.Content
            val currentSelectedIds = contentState.selectedGenreIds.toMutableSet()

            if (currentSelectedIds.contains(genreId)) {
                currentSelectedIds.remove(genreId)
            } else {
                currentSelectedIds.add(genreId)
            }

            contentState.copy(
                selectedGenreIds = currentSelectedIds,
                isNextButtonEnabled = currentSelectedIds.isNotEmpty()
            )
        }
    }

    /** 악기 선택 이벤트 */
    fun onInstrumentSelected(instrumentId: Int) {
        if (_uiState.value !is OnboardingUiState.Content) return

        _uiState.update { currentState ->
            val contentState = currentState as OnboardingUiState.Content
            // 이미 선택된 악기를 다시 클릭하면 아무 반응 없음
            if (contentState.selectedInstrumentId == instrumentId) {
                return@update contentState // 상태 변경 없이 현재 상태를 그대로 반환
            }

            // 새로운 악기를 선택하면 '완료' 버튼 활성화
            contentState.copy(
                selectedInstrumentId = instrumentId,
                isNextButtonEnabled = true
            )
        }
    }

    /** 추천 악보 선택/해제 이벤트 */
    fun onSheetSelected(sheetId: Long) {
        if (_uiState.value !is OnboardingUiState.Content) return

        _uiState.update { currentState ->
            val contentState = currentState as OnboardingUiState.Content
            val currentSelectedIds = contentState.selectedOnboardingSheetIds.toMutableSet()

            if (currentSelectedIds.contains(sheetId)) {
                currentSelectedIds.remove(sheetId)
            } else {
                currentSelectedIds.add(sheetId)
            }

            // (악보 선택은 '다음' 버튼 활성화에 영향을 주지 않음)
            contentState.copy(selectedOnboardingSheetIds = currentSelectedIds)
        }
    }

    /** "건너뛰기" 버튼 클릭 이벤트 */
    fun onSkipClicked() {
        if (_uiState.value !is OnboardingUiState.Content) return
        val contentState = _uiState.value as OnboardingUiState.Content

        // 악보 추천 단계를 건너뛰고 즉시 '최종 저장' 로직 실행
        Log.d(TAG, "악보 추천 건너뛰기 -> 최종 저장")
        completeOnboarding()
    }

    /** '다음' 또는 '완료' 버튼 클릭 이벤트 */
    fun onNextClicked() {
        if (_uiState.value !is OnboardingUiState.Content) return
        val contentState = _uiState.value as OnboardingUiState.Content

        when (contentState.currentStep) {
            OnboardingStep.SELECT_GENRE -> {
                // 다음 단계(악기 선택)로 전환.
                // 만약 사용자가 이전에 악기를 선택한 기록이 있다면 '완료' 버튼을 바로 활성화하고, 아니면 비활성화.
                _uiState.update {
                    (it as OnboardingUiState.Content).copy(
                        currentStep = OnboardingStep.SELECT_INSTRUMENT,
                        isNextButtonEnabled = it.selectedInstrumentId != null
                    )
                }
            }

            OnboardingStep.SELECT_INSTRUMENT -> {
                loadOnboardingRecommendSheets(contentState.selectedInstrumentId)
            }

            // 악보 추천 1/3 -> 2/3
            OnboardingStep.SELECT_SHEET_1 -> {
                _uiState.update {
                    (it as OnboardingUiState.Content).copy(
                        currentStep = OnboardingStep.SELECT_SHEET_2,
                        isNextButtonEnabled = true
                    )
                }
            }

            // 악보 추천 2/3 -> 3/3
            OnboardingStep.SELECT_SHEET_2 -> {
                _uiState.update {
                    (it as OnboardingUiState.Content).copy(
                        currentStep = OnboardingStep.SELECT_SHEET_3,
                        isNextButtonEnabled = true
                    )
                }
            }

            // 악보 추천 3/3 -> 최종 저장
            OnboardingStep.SELECT_SHEET_3 -> {
                completeOnboarding()
            }
        }
    }



    private fun loadOnboardingRecommendSheets(instrumentId: Int?) {
        if (instrumentId == null) {
            _uiState.value = OnboardingUiState.Error("악기를 선택해야 추천을 받을 수 있습니다.")
            return
        }

        viewModelScope.launch {
            // 로딩 시작
            _uiState.update { (it as OnboardingUiState.Content).copy(isLoading = true) }
            getOnboardingRecommendSheetsUseCase(instrumentId)
                .onSuccess { sheets ->
                    Log.d(TAG, "추천 악보 ${sheets.size}개 로드 성공")
                    // 성공: 악보 저장 및 다음 단계로 이동
                    _uiState.update {
                        (it as OnboardingUiState.Content).copy(
                            isLoading = false,
                            onboardingRecommendSheets = sheets, // 18개 악보 저장
                            currentStep = OnboardingStep.SELECT_SHEET_1, // 악보 추천 1단계로 이동
                            isNextButtonEnabled = true // (다음/건너뛰기 버튼 활성화)
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "추천 악보 로드 실패", exception)
                    // 실패: 로딩 끄고 에러 표시
                    _uiState.update {
                        (it as OnboardingUiState.Content).copy(isLoading = false)
                    }
                    _uiState.value = OnboardingUiState.Error(exception.message)
                }
        }
    }

    // 서버 연동 시 API를 여기서 호출해서 결과에 따라 상태 변경
    private fun completeOnboarding() {
        if (_uiState.value !is OnboardingUiState.Content) return
        val contentState = _uiState.value as OnboardingUiState.Content

        // 이미 로딩 중이면 중복 호출 방지
        if (contentState.isLoading) return

        viewModelScope.launch {
            // 전송할 데이터 추출
            val genreIds = contentState.selectedGenreIds.toList()
            val instrumentId = contentState.selectedInstrumentId
            val sheetIds = contentState.selectedOnboardingSheetIds.map { it.toInt() }

            Log.d(TAG, "완료 버튼 클릭: 장르 $genreIds, 악기 $instrumentId, 악보 $sheetIds")

            // 악기 ID가 null이 아닌지 확인 (안전장치)
            if (instrumentId == null) {
                Log.e(TAG, "악기가 선택되지 않았습니다. 전송 불가.")
                _uiState.value = OnboardingUiState.Error("악기를 선택해주세요.")
                return@launch
            }
            // API 호출 시작: 로딩 상태로 변경
            _uiState.update {
                (it as OnboardingUiState.Content).copy(isLoading = true)
            }

            signUpUseCase(genreIds, instrumentId, sheetIds)
                .onSuccess {
                    // Complete 상태로 변경 (화면 이동 트리거)
                    Log.d(TAG, "signUpUseCase 성공")
                    tokenRepository.updateFirstLoginStatus(false)
                    _uiState.value = OnboardingUiState.Complete
                }
                .onFailure { exception ->
                    // Error 상태로 변경 (토스트 등)
                    Log.e(TAG, "signUpUseCase 실패", exception)
                    _uiState.update {
                        (it as OnboardingUiState.Content).copy(isLoading = false)
                    }
                    _uiState.value = OnboardingUiState.Error(exception.message)
                }
        }
    }
}

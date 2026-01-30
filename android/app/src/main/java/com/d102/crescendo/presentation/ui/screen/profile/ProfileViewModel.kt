package com.d102.crescendo.presentation.ui.screen.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.domain.usecase.auth.LogoutUseCase
import com.d102.crescendo.domain.usecase.common.GetGenresUseCase
import com.d102.crescendo.domain.usecase.common.GetInstrumentsUseCase
import com.d102.crescendo.domain.usecase.user.DeleteAccountUseCase
import com.d102.crescendo.domain.usecase.user.GetProfileUseCase
import com.d102.crescendo.util.PracticeUpdateNotifier
import com.d102.crescendo.util.ProfileUpdateNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TAG = "Crescendo_ProfileViewModel"

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getGenresUseCase: GetGenresUseCase,
    private val getInstrumentsUseCase: GetInstrumentsUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val profileUpdatenotifier: ProfileUpdateNotifier,
    private val practiceUpdateNotifier: PracticeUpdateNotifier
) : ViewModel() {

    // 회원탈퇴 모달 '보임/숨김' 상태
    private val _showWithdrawDialog = MutableStateFlow(false)
    val showWithdrawDialog: StateFlow<Boolean> = _showWithdrawDialog.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // 선택된 탭 인덱스 저장
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    init {
        // ViewModel이 생성되면 즉시 프로필 정보를 불러오도록
        loadUserProfile()
        // 프로필 업데이트 이벤트 구독
        observeUpdates()
    }

    // 탭 변경 함수
    fun onTabSelected(index: Int) {
        _selectedTabIndex.value = index
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            if (_uiState.value is ProfileUiState.Loading)
                return@launch

            _uiState.value = ProfileUiState.Loading // 로딩 상태 시작

            val profileResult = getProfileUseCase()
            val genresResult = getGenresUseCase()
            val instrumentsResult = getInstrumentsUseCase()

            if (profileResult.isSuccess && genresResult.isSuccess && instrumentsResult.isSuccess) {
                // 성공 시, 3개의 데이터를 모두 UiState에 담아 전송
                Log.d(TAG, "All UseCases Success")

                _uiState.value = ProfileUiState.Success(
                    userProfile = profileResult.getOrThrow(),
                    genres = genresResult.getOrThrow(),
                    instruments = instrumentsResult.getOrThrow(),

                )
            } else {
                // 실패 시, 첫 번째로 실패한 에러 메시지를 전송
                Log.e(TAG, "UseCase Failed")
                val errorMessage = (profileResult.exceptionOrNull()
                    ?: genresResult.exceptionOrNull()
                    ?: instrumentsResult.exceptionOrNull())
                    ?.message ?: "프로필 로드 실패"

                _uiState.value = ProfileUiState.Error(errorMessage)
            }
        }
    }

    private fun observeUpdates() {
        viewModelScope.launch {
            profileUpdatenotifier.events.collect {
                Log.d(TAG, "Profile update notification received. Refreshing...")
                loadUserProfile()
            }
        }

        viewModelScope.launch {
            practiceUpdateNotifier.events.collect {
                Log.d(TAG, "Practice save notification received. Refreshing profile...")
                loadUserProfile() // 👈 새로고침 (완곡한 곡 갱신)
            }
        }
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            logoutUseCase()
                .onSuccess {
                    Log.d(TAG, "Logout Success. Tokens cleared.")
                    _uiState.value = ProfileUiState.NavigateToLogin
                }
                .onFailure { exception ->
                    Log.e(TAG, "Logout Failed", exception)
                    _uiState.value = ProfileUiState.Error(exception.message ?: "로그아웃 실패")
                }
        }
    }

    fun onWithdrawClicked() {
        _showWithdrawDialog.value = true
    }

    fun onDismissWithdrawDialog() {
        _showWithdrawDialog.value = false
    }

    fun confirmWithdraw() {
        _showWithdrawDialog.value = false // 모달 닫기
        viewModelScope.launch {
            // 👈 (기존 onWithdrawClicked의 로직과 동일)
            deleteAccountUseCase()
                .onSuccess {
                    Log.d(TAG, "Withdraw Success. Tokens cleared.")
                    _uiState.value = ProfileUiState.NavigateToLogin
                }
                .onFailure { exception ->
                    Log.e(TAG, "Withdraw Failed", exception)
                    _uiState.value = ProfileUiState.Error(exception.message ?: "회원 탈퇴 실패")
                }
        }
    }

    fun consumeUiState() {
        _uiState.value = ProfileUiState.Idle
    }



}
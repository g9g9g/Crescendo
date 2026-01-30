package com.d102.crescendo.presentation.ui.screen.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.domain.repository.TokenRepository
import com.d102.crescendo.domain.usecase.auth.GetAuthStatusFlowUseCase
import com.d102.crescendo.domain.usecase.user.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TAG = "Crescendo_SplashViewModel"
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase, // 서버 검증용 -> 간단한 API 호출
    private val tokenRepository: TokenRepository    // 로컬 플래그 확인용
) : ViewModel() {
    private val _splashUiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val splashUiState: StateFlow<SplashUiState> = _splashUiState.asStateFlow()

    init {
        // '선-인증' 로직 실행
        validateSession()
    }

    private fun validateSession() {
        viewModelScope.launch {
            val accessToken = tokenRepository.getAccessToken().first()
            val isFirstLogin = tokenRepository.isFirstLogin().first()

            Log.d(TAG, "validateSession() -> accessToken='$accessToken', isFirstLogin=$isFirstLogin")

            if (accessToken.isNullOrEmpty()) {
                Log.d(TAG, "validateSession: No AccessToken. -> NavigateToLogin")
                _splashUiState.value = SplashUiState.NavigateToLogin
                return@launch
            }

            Log.d(TAG, "validateSession: Token exists. Calling getProfile...")
            val profileResult = getProfileUseCase()

            if (profileResult.isSuccess) {
                Log.d(TAG, "validateSession: getProfile SUCCESS.")
                val isFirstLoginNow = tokenRepository.isFirstLogin().first()
                Log.d(TAG, "validateSession: isFirstLoginNow=$isFirstLoginNow")
                if (isFirstLoginNow) {
                    _splashUiState.value = SplashUiState.NavigateToOnboarding
                } else {
                    _splashUiState.value = SplashUiState.NavigateToMain
                }
            } else {
                Log.e(TAG, "validateSession: getProfile FAILED.", profileResult.exceptionOrNull())
                tokenRepository.clearTokens()
                _splashUiState.value = SplashUiState.NavigateToLogin
            }
        }
    }

}
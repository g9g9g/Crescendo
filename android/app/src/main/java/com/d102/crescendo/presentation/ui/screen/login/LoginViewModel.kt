package com.d102.crescendo.presentation.ui.screen.login

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.BuildConfig
import com.d102.crescendo.domain.repository.TokenRepository
import com.d102.crescendo.domain.usecase.auth.GoogleLoginUseCase
import com.d102.crescendo.domain.usecase.fcm.UpdateFcmTokenUseCase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

private val TAG = "Crescendo_LoginViewModel"

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val updateFcmTokenUseCase: UpdateFcmTokenUseCase,
    private val tokenRepository: TokenRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * LoginScreen에서 'Google 로그인' 버튼 클릭 시 호출.
     */
    fun initiateGoogleLogin(context: Context) {
        if (uiState.value is LoginUiState.Loading) return // 이미 로딩 중이면 무시

        // Gradle에서 주입된 BuildConfig.GOOGLE_CLIENT_ID
        val webClientId = BuildConfig.GOOGLE_CLIENT_ID
        _uiState.value = LoginUiState.Loading // UI 상태 '로딩'으로 변경

        // Google 로그인 옵션 빌드
        val nonce = UUID.randomUUID().toString() // 재생 공격 방지를 위한 Nonce
        val googleIdOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(nonce)
            .build()

        // Credential Manager 요청 빌드
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Coroutine으로 SDK 호출
        viewModelScope.launch {
            try {
                // Google 로그인 하단 시트(Bottom Sheet) 표시 및 결과 대기
                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )

                // 결과 처리
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    try {
                        // idToken 파싱
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        val idToken = googleIdTokenCredential.idToken

                        // 파싱 성공 -> idToken으로 실제 로그인 처리
                        handleBackendLoginAndFcm(idToken)

                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "idToken 파싱 실패", e)
                        _uiState.value = LoginUiState.Error("로그인 응답을 처리하지 못했습니다.")
                    }
                } else {
                    _uiState.value = LoginUiState.Error("알 수 없는 로그인 유형입니다.")
                }

            } catch (e: GetCredentialException) {
                // 사용자가 하단 시트를 닫거나(Cancel) 실패한 경우
                Log.e(TAG, "로그인 실패 또는 취소", e)
                _uiState.value = LoginUiState.Idle // UI 상태 원복
            }
        }
    }

    /**
     * Google idToken을 백엔드 서버로 보내고, 우리 앱의 JWT를 받아 저장
     */
    private fun handleBackendLoginAndFcm(idToken: String,) {
        viewModelScope.launch {
            // 백엔드 로그인
            googleLoginUseCase(idToken)
                .onSuccess { authToken ->
                    Log.d(TAG, "백엔드 로그인 및 토큰 저장 성공")

                    // FCM 토큰 가져오기
                    try {
                        val fcmToken = Firebase.messaging.token.await()
                        Log.d(TAG, "FCM 토큰 가져오기 성공: $fcmToken")

                        tokenRepository.saveFcmToken(fcmToken)

                        // 3. [추가] FCM 토큰 서버에 등록
                        updateFcmTokenUseCase(fcmToken)
                            .onSuccess {
                                Log.d(TAG, "FCM 토큰 서버 등록 성공")
                            }
                            .onFailure { fcmError ->
                                Log.e(TAG, "FCM 토큰 서버 등록 실패", fcmError)
                                // (FCM 등록 실패는 로그인은 성공한 것으로 처리)
                            }

                    } catch (e: Exception) {
                        Log.e(TAG, "FCM 토큰 가져오기 실패", e)
                        // (FCM 토큰 가져오기 실패는 로그인은 성공한 것으로 처리)
                    }

                    // 로그인 성공 상태 전파
                    _uiState.value = LoginUiState.Success(authToken.firstLoginYn)
                }
                .onFailure { exception ->
                    Log.e(TAG, "백엔드 로그인 실패", exception)
                    _uiState.value = LoginUiState.Error(exception.message ?: "로그인에 실패했습니다.")
                }
        }
    }

    fun consumeUiState() {
        _uiState.value = LoginUiState.Idle
    }
}
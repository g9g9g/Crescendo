package com.d102.crescendo.presentation.ui.screen.profile.edit

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.data.remote.exception.ApiException
import com.d102.crescendo.domain.model.onboarding.Genre
import com.d102.crescendo.domain.usecase.common.GetGenresUseCase
import com.d102.crescendo.domain.usecase.s3.UploadFileUseCase
import com.d102.crescendo.domain.usecase.user.GetProfileUseCase
import com.d102.crescendo.domain.usecase.user.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TAG = "Crescendo_ProfileEditViewModel"
@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getGenresUseCase: GetGenresUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
) : ViewModel() {

    // API 통신 상태
    private val _uiState = MutableStateFlow<ProfileEditUiState>(ProfileEditUiState.Idle)
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    // 닉네임
    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    // 수정 불가능한 이메일
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    // 선택 가능한 전체 태그 목록
    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val availableTags: StateFlow<List<String>> = _availableTags.asStateFlow()

    // 사용자가 선택한 태그
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    // 이미지 관련 상태
    private val _displayImage = MutableStateFlow<Any?>(null)
    val displayImage: StateFlow<Any?> = _displayImage.asStateFlow()

    // 원본 데이터 캐시
    private var allGenres: List<Genre> = emptyList()
    private var initialNickname: String = ""
    private var initialProfileUrl: String? = null

    init {
        loadInitialProfile()
    }

    private fun loadInitialProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileEditUiState.Loading

            val profileResult = getProfileUseCase()
            val genresResult = getGenresUseCase()

            if (profileResult.isSuccess && genresResult.isSuccess) {
                val profile = profileResult.getOrThrow()
                val genres = genresResult.getOrThrow() // 장르 목록(ID, 이름)을 ViewModel에 캐시

                this@ProfileEditViewModel.allGenres = genres

                val genreMap = genres.associate { it.id to it.korName }
                val currentSelectedTags = profile.favoriteGenreIds
                    .mapNotNull { id -> genreMap[id] }
                    .toSet()

                initialProfileUrl = profile.profileUrl // 원본 (비교용)
                _displayImage.value = profile.profileUrl // UI 표시용 (S3 URL)

                _nickname.value = profile.nickname
                _email.value = profile.email
                _availableTags.value = genres.map { it.korName }
                _selectedTags.value = currentSelectedTags

                initialNickname = profile.nickname // 원본 닉네임 저장
                _uiState.value = ProfileEditUiState.Idle // 로딩 종료
            } else {
                val error = profileResult.exceptionOrNull() ?: genresResult.exceptionOrNull()
                Log.e(TAG, "Failed to load initial data", error)
                _uiState.value = ProfileEditUiState.Error(error?.message ?: "데이터 로드 실패")
            }
        }
    }

    /**
     * [이벤트] 닉네임 텍스트필드 값이 변경될 때
     */
    fun onNicknameChanged(newNickname: String) {
        _nickname.value = newNickname
    }

    /**
     * [이벤트] 태그 칩(Chip) 클릭 시
     */
    fun onTagClicked(tag: String) {
        val newSelectedTags = _selectedTags.value.toMutableSet()
        if (newSelectedTags.contains(tag)) {
            newSelectedTags.remove(tag)
        } else {
            newSelectedTags.add(tag)
        }
        _selectedTags.value = newSelectedTags
    }

    /**
     * [이벤트] 갤러리에서 이미지 선택 시 (UI가 호출)
     */
    fun onImageSelected(uri: Uri?) {
        if (uri != null) {
            _displayImage.value = uri
        }
    }


    /**
     * [이벤트] "프로필 수정" 완료 버튼 클릭 시
     * [수정] 409(중복) 에러 처리 로직 추가
     */
    fun onSubmitClicked() {
        // 버튼 활성화 로직은 UI(Screen)에서 처리
        // (예: nickname.isNotEmpty() && selectedTags.isNotEmpty())

        val newNickname = _nickname.value
        val newSelectedTags = _selectedTags.value
        val newDisplayImage = _displayImage.value

        viewModelScope.launch {
            _uiState.value = ProfileEditUiState.Loading
            var finalProfileUrl: String? = initialProfileUrl

            try {
                // (S3 업로드) 새로 선택한 이미지가 있다면 S3에 업로드
                if (newDisplayImage is Uri) {
                    Log.d(TAG, "onSubmit: Uploading new image...")
                    finalProfileUrl = uploadFileUseCase(newDisplayImage, "profile")
                        .getOrThrow()
                }

                // (ID 변환) 장르 이름(Set<String>)을 ID(List<Int>)로 변환
                val genreMap = allGenres.associate { it.korName to it.id }
                val finalGenreIds = newSelectedTags.mapNotNull { tagName -> genreMap[tagName] }

                // (프로필 수정) 변경된 내용만 서버로 전송
                Log.d(TAG, "onSubmit: Calling updateProfile...")
                updateProfileUseCase(
                    nickname = if (newNickname != initialNickname) newNickname else null,
                    genreIds = finalGenreIds,
                    profileUrl = finalProfileUrl
                ).getOrThrow()

                Log.d(TAG, "onSubmit: Success!")
                _uiState.value = ProfileEditUiState.Complete

            } catch (e: Exception) {
                Log.e(TAG, "onSubmit: Failed", e)
                val errorMessage = if (e is ApiException) {
                    // 서버 명세에 따른 에러 코드 분기
                    when (e.httpCode) {
                        400 -> "닉네임 형식이 올바르지 않습니다."
                        409 -> "이미 사용 중인 닉네임입니다."
                        404 -> "선택한 장르 ID를 찾을 수 없습니다."
                        else -> e.message ?: "프로필 수정 실패"
                    }
                } else {
                    e.message ?: "알 수 없는 오류 발생"
                }
                _uiState.value = ProfileEditUiState.Error(errorMessage)
            }
        }
    }

    /**
     * [이벤트] UI가 Success/Error 상태를 소비(consume)한 후 호출
     */
    fun consumeUiState() {
        _uiState.value = ProfileEditUiState.Idle
    }
}
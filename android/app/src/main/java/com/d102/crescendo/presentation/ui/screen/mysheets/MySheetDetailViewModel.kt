package com.d102.crescendo.presentation.ui.screen.mysheets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.domain.model.performance.PlayResult
import com.d102.crescendo.domain.usecase.performance.GetPlayResultUseCase
import com.d102.crescendo.domain.usecase.sheet.ArrangeUserSheetUseCase
import com.d102.crescendo.domain.usecase.sheet.GetMySheetDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MySheetDetailViewModel"

@HiltViewModel
class MySheetDetailViewModel @Inject constructor(
    private val getMySheetDetailUseCase: GetMySheetDetailUseCase,
    private val getPlayResultUseCase: GetPlayResultUseCase,
    private val arrangeUserSheetUseCase: ArrangeUserSheetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MySheetDetailUiState>(MySheetDetailUiState.Loading)
    val uiState: StateFlow<MySheetDetailUiState> = _uiState.asStateFlow()

    private val _playResultState = MutableStateFlow<PlayResultUiState>(PlayResultUiState.Idle)
    val playResultState: StateFlow<PlayResultUiState> = _playResultState.asStateFlow()

    // 편곡 결과 UIState
    private val _arrangeState = MutableStateFlow<ArrangeUiState>(ArrangeUiState.Idle)
    val arrangeState: StateFlow<ArrangeUiState> = _arrangeState.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    /**
     * 내 악보 상세 정보 로드
     */
    fun loadSheetDetail(userSheetId: Int) {
        viewModelScope.launch {
            _uiState.value = MySheetDetailUiState.Loading
            Log.d(TAG, "내 악보 상세 조회 userSheetId=$userSheetId")

            val result = getMySheetDetailUseCase(userSheetId = userSheetId)

            result.fold(
                onSuccess = { sheetDetail ->
                    Log.d(TAG, "내 악보 상세 조회 성공: ${sheetDetail.title}")
                    Log.d(TAG, "진도율: ${sheetDetail.progress}%")

                    // 진도율 100% 이상이면 완료 상태로 업데이트
                    _isCompleted.value = sheetDetail.progress >= 100
                    Log.d(TAG, "완료 상태: ${_isCompleted.value}")

                    _uiState.value = MySheetDetailUiState.Success(sheetDetail)
                },
                onFailure = { exception ->
                    Log.e(TAG, "내 악보 상세 조회 실패: ${exception.message}")
                    _uiState.value = MySheetDetailUiState.Error(
                        exception.message ?: "악보 상세 조회 실패"
                    )
                }
            )
        }
    }

    /**
     * 악보 편곡 요청
     */
    fun arrangeSheet(
        userSheetId: Int,
        style: String,
        xmlUrl: String
    ) {
        viewModelScope.launch {
            Log.d(TAG, "==================== 악보 편곡 요청 시작 ====================")
            Log.d(TAG, "userSheetId: $userSheetId")
            Log.d(TAG, "style: $style")
            Log.d(TAG, "xmlUrl: $xmlUrl")

            _arrangeState.value = ArrangeUiState.Loading

            val result = arrangeUserSheetUseCase(
                userSheetId = userSheetId,
                style = style,
                xmlUrl = xmlUrl
            )

            result.fold(
                onSuccess = {
                    Log.d(TAG, "악보 편곡 성공")
                    Log.d(TAG, "새로운 편곡 악보가 생성되었습니다.")
                    Log.d(TAG, "============================================================")
                    _arrangeState.value = ArrangeUiState.Success
                },
                onFailure = { exception ->
                    Log.e(TAG, "악보 편곡 실패: ${exception.message}")
                    Log.e(TAG, "에러 타입: ${exception::class.simpleName}")
                    Log.e(TAG, "============================================================")
                    _arrangeState.value = ArrangeUiState.Error(
                        exception.message ?: "악보 편곡 중 오류 발생"
                    )
                }
            )
        }
    }

    /**
     * 연주 기록 결과 조회
     */
    fun loadPlayResult(playId: Int) {
        viewModelScope.launch {
            _playResultState.value = PlayResultUiState.Loading
            Log.d(TAG, "==================== 연주 평가 결과 조회 시작 ====================")
            Log.d(TAG, "playId: $playId")

            val result = getPlayResultUseCase(playId = playId)

            result.fold(
                onSuccess = { playResult ->
                    Log.d(TAG, "연주 평가 결과 조회 성공")
                    Log.d(TAG, "  ├─ status: ${playResult.status}")
                    Log.d(TAG, "  ├─ totalScore: ${playResult.totalScore}")
                    Log.d(TAG, "  ├─ grade: ${playResult.grade}")
                    Log.d(TAG, "  ├─ comment: ${playResult.comment}")

                    if (!playResult.metrics.isNullOrEmpty()) {
                        Log.d(TAG, "  └─ metrics (${playResult.metrics.size}개):")
                        playResult.metrics.forEachIndexed { index, metric ->
                            Log.d(TAG, "      ${index + 1}. ${metric.name} (${metric.code}): ${metric.score}점")
                        }
                    } else {
                        Log.d(TAG, "  └─ metrics: 없음")
                    }
                    Log.d(TAG, "================================================================")

                    _playResultState.value = PlayResultUiState.Success(playResult)
                },
                onFailure = { exception ->
                    Log.e(TAG, "연주 평가 결과 조회 실패")
                    Log.e(TAG, "  └─ 에러 메시지: ${exception.message}")
                    Log.e(TAG, "  └─ 에러 타입: ${exception::class.simpleName}")
                    exception.printStackTrace()
                    Log.e(TAG, "================================================================")

                    _playResultState.value = PlayResultUiState.Error(
                        exception.message ?: "연주 기록 조회 실패"
                    )
                }
            )
        }
    }

    /**
     * 재시도
     */
    fun retry(userSheetId: Int) {
        loadSheetDetail(userSheetId)
    }

    /**
     * 연주 결과 상태 초기화
     */
    fun resetPlayResultState() {
        _playResultState.value = PlayResultUiState.Idle
    }
    /**
     * 편곡 상태 초기화
     */
    fun resetArrangeState() {
        _arrangeState.value = ArrangeUiState.Idle
    }
}
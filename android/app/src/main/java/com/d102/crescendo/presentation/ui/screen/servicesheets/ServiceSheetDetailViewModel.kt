package com.d102.crescendo.presentation.ui.screen.servicesheets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.domain.model.sheet.ServiceSheetDetail
import com.d102.crescendo.domain.usecase.sheet.DownloadServiceSheetUseCase
import com.d102.crescendo.domain.usecase.sheet.GetServiceSheetDetailUseCase
import com.d102.crescendo.domain.usecase.sheet.GetSimilarServiceSheetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opensheetmusicdisplay.osmd.kotlin.OSMD
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "ServiceSheetDetailVM"
@HiltViewModel
class ServiceSheetDetailViewModel @Inject constructor(
    private val getServiceSheetDetail: GetServiceSheetDetailUseCase,
    private val getSimilarServiceSheets: GetSimilarServiceSheetsUseCase,
    private val downloadServiceSheet: DownloadServiceSheetUseCase
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<ServiceSheetDetailUiState>(ServiceSheetDetailUiState.Idle)
    val uiState: StateFlow<ServiceSheetDetailUiState> = _uiState

    // 다운로드 이벤트(토스트용 one-shot)
    sealed interface DownloadEvent {
        data object Success : DownloadEvent
        data class Error(val message: String) : DownloadEvent
    }

    private val _downloadEvents = MutableSharedFlow<DownloadEvent>()
    val downloadEvents = _downloadEvents


    // 미리듣기 관련 OSMD 객체
    val osmd = OSMD()
    // 미리듣기 재생 상태
    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying = _isPreviewPlaying.asStateFlow()

    // OSMDView가 렌더링되었는지 알리는 플래그
    private val _isOsmdReady = MutableStateFlow(false)
    val isOsmdReady = _isOsmdReady.asStateFlow()

    // 로드할 XML URL을 임시 저장
    private var pendingXmlUrl: String? = null

    init {
        setupOsmdListeners()
    }

    private fun setupOsmdListeners() {
        // [수정] OSMD.kt의 JsBridge가 직접 호출하는 리스너 설정
        osmd.playbackStateChangedListener = { state ->
            viewModelScope.launch {
                if (state == "paused" || state == "stopped") {
                    _isPreviewPlaying.value = false
                }
            }
        }
    }

    fun onOsmdReady() {
        _isOsmdReady.value = true
        // 만약 준비되기 전에 playPreview가 호출되었다면, 지금 실행
        pendingXmlUrl?.let {
            playPreview(it)
            pendingXmlUrl = null
        }
    }

    /**
     * 미리듣기 토글
     */
    fun togglePreview(xmlUrl: String) {
        if (xmlUrl.isBlank()) return

        if (_isPreviewPlaying.value) {
            // 재생 중이면 정지
            stopPreviewInternal()
        } else {
            // 정지 중이면 재생
            playPreview(xmlUrl)
        }
    }

    /**
     * 실제 재생 시작
     */
    private fun playPreview(xmlUrl: String) {
        if (_isOsmdReady.value) {
            _isPreviewPlaying.value = true
            // 렌더링 없이 오디오만 로드 및 재생
            osmd.loadMusicXMLWithoutRendering(xmlUrl)
        } else {
            // 엔진이 아직 준비 안 됨 → 준비되면 onOsmdReady에서 실행
            Log.w(TAG, "Preview requested, but OSMD Engine not ready. Pending...")
            pendingXmlUrl = xmlUrl
        }
    }

    /**
     * 내부에서 공통으로 사용하는 정지 로직
     */
    private fun stopPreviewInternal() {
        // 대기 중인 XML도 취소
        pendingXmlUrl = null

        try {
            osmd.stop() // 실제 OSMD 정지 메소드
        } catch (e: Exception) {
            Log.e(TAG, "미리듣기 정지 중 오류", e)
        } finally {
            _isPreviewPlaying.value = false
        }
    }

    /**
     * 화면/네비게이션 등에서 직접 호출할 수 있는 정지 함수
     * - 유사한 악보 눌러서 다른 상세로 갈 때
     * - 화면에서 나갈 때(DisposableEffect 등)
     */
    fun stopPreview() {
        if (_isPreviewPlaying.value || pendingXmlUrl != null) {
            stopPreviewInternal()
        }
    }

    override fun onCleared() {
        // 뷰모델 종료 시에도 안전하게 정지 후 파기
        stopPreviewInternal()
        osmd.destroy()
        super.onCleared()
    }


    fun load(sheetId: Int) {
        Log.d(TAG, "========== 서비스 악보 상세 로드 시작 ==========")
        _uiState.value = ServiceSheetDetailUiState.Loading
        viewModelScope.launch {
            try {
                // 상세/유사 악보 병렬 로드
                val detailDeferred = async { getServiceSheetDetail(sheetId) }
                val similarDeferred = async { getSimilarServiceSheets(sheetId) }

                // 상세 실패면 전체 실패 처리
                val detail = detailDeferred.await().getOrElse { throwable ->
                    _uiState.value =
                        ServiceSheetDetailUiState.Error(throwable.message ?: "알 수 없는 오류가 발생했어요.")
                    return@launch
                }

                Log.d(TAG, "========== 상세 정보 로드 성공 ==========")
                Log.d(TAG, "제목: ${detail.title}")
                Log.d(TAG, "작곡가: ${detail.composer}")
                Log.d(TAG, "장르 ID: ${detail.genreId}")
                Log.d(TAG, "악기 ID: ${detail.instrumentId}")
                Log.d(TAG, "난이도: ${detail.tierCode} - Level ${detail.tierLevel}")
                Log.d(TAG, "다운로드 수: ${detail.downloadNumber}")
                Log.d(TAG, "썸네일 URL: ${detail.thumbnailUrl ?: "없음"}")
                Log.d(TAG, "XML 미리보기 URL: ${detail.xmlUrlPreview ?: "없음"}")
                Log.d(TAG, "XML URL: ${detail.xmlUrl ?: "없음"}")

                detail.metrics?.let { metrics ->
                    Log.d(TAG, "---------- Metrics 정보 ----------")
                    Log.d(TAG, "Tempo: ${metrics.tempo}")
                    Log.d(TAG, "Rhythm: ${metrics.rhythm}")
                    Log.d(TAG, "Intervals: ${metrics.intervals}")
                    Log.d(TAG, "Harmony: ${metrics.harmony}")
                    Log.d(TAG, "Technique: ${metrics.technique}")
                    Log.d(TAG, "Length: ${metrics.length}")
                } ?: Log.d(TAG, "Metrics: 없음")

                Log.d(TAG, "요약 정보")
                if (!detail.summary.isNullOrBlank()) {
                    Log.d(TAG, "  - Summary: ${detail.summary}")
                } else {
                    Log.d(TAG, "  - Summary: null 또는 빈 문자열")
                }

                Log.d(TAG, "----------------------------------------")
                Log.d(TAG, "추천 사항 (Recommendations)")
                if (detail.recommendations.isNotEmpty()) {
                    Log.d(TAG, "  - 개수: ${detail.recommendations.size}")
                    detail.recommendations.forEachIndexed { index, recommendation ->
                        Log.d(TAG, "    [${index + 1}] $recommendation")
                    }
                } else {
                    Log.d(TAG, "  - 추천 사항: 빈 리스트")
                }


                // 유사 악보는 실패해도 화면은 유지 (빈 리스트 대체)
                val similar = similarDeferred.await().getOrElse { emptyList() }
                Log.d(TAG, "유사 악보 개수: ${similar.size}")

                _uiState.value = ServiceSheetDetailUiState.Success(
                    detail.copy(similarSheets = similar)
                )
            } catch (ce: CancellationException) {
                // 코루틴 취소는 상위로 전달
                throw ce
            } catch (e: Exception) {
                _uiState.value =
                    ServiceSheetDetailUiState.Error(e.message ?: "알 수 없는 오류가 발생했어요.")
            }
        }
    }

    fun download(sheetId: Int) {
        viewModelScope.launch {
            downloadServiceSheet(sheetId).fold(
                onSuccess = {
                    // 화면 전체 reload 대신, 현재 uiState 안의 downloadNumber만 증가
                    val currentState = _uiState.value
                    if (currentState is ServiceSheetDetailUiState.Success) {
                        val updatedData = currentState.data.copy(
                            downloadNumber = currentState.data.downloadNumber + 1
                        )
                        _uiState.value = currentState.copy(data = updatedData)
                    }

                    _downloadEvents.emit(DownloadEvent.Success)
                },
                onFailure = { e ->
                    _downloadEvents.emit(
                        ServiceSheetDetailViewModel.DownloadEvent.Error(
                            e.message ?: "다운로드에 실패했어요."
                        )
                    )
                }
            )
        }
    }

}
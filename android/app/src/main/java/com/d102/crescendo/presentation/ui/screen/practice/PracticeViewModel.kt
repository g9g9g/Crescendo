package com.d102.crescendo.presentation.ui.screen.practice

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d102.crescendo.data.local.SettingsRepository
import com.d102.crescendo.di.ApplicationScope
import com.d102.crescendo.domain.model.performance.PlayRecord
import com.d102.crescendo.domain.model.performance.PlayResult
import com.d102.crescendo.domain.usecase.performance.SavePlayRecordUseCase
import com.d102.crescendo.domain.usecase.s3.UploadFileUseCase
import com.d102.crescendo.util.NotificationHelper
import com.google.android.datatransport.runtime.firebase.transport.LogEventDropped
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.opensheetmusicdisplay.osmd.kotlin.OSMD
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val TAG = "Crescendo_PracticeViewModel"

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val savePlayRecordUseCase: SavePlayRecordUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<PracticeUiState>(PracticeUiState.Loading)
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private val _hasLoadStarted = MutableStateFlow(false)
    val hasLoadStarted = _hasLoadStarted.asStateFlow()

    private val _exitDialogType = MutableStateFlow(ExitDialogType.NONE)
    val exitDialogType = _exitDialogType.asStateFlow()

    // OSMD 객체를 뷰모델이 소유
    val osmd = OSMD()
    val osmdOptions: JSONObject = JSONObject().apply {
        put("drawTitle", true)
        put("followCursor", true)
    }

    // UI 컨트롤 상태
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isMicOn = MutableStateFlow(false)
    val isMicOn = _isMicOn.asStateFlow()

    private val _isRepeatOn = MutableStateFlow(false)
    val isRepeatOn = _isRepeatOn.asStateFlow()

    private val _isMetronomeOn = MutableStateFlow(false)
    val isMetronomeOn = _isMetronomeOn.asStateFlow()

    private val _isHighlightOn = MutableStateFlow(false)
    val isHighlightOn = _isHighlightOn.asStateFlow()

    private val _practiceStart = MutableStateFlow<Int?>(null)
    val practiceStart = _practiceStart.asStateFlow()

    private val _practiceEnd = MutableStateFlow<Int?>(null)
    val practiceEnd = _practiceEnd.asStateFlow()

    private val _showRangeInput = MutableStateFlow(false)
    val showRangeInput = _showRangeInput.asStateFlow()

    private val _startRangeInput = MutableStateFlow("")
    val startRangeInput = _startRangeInput.asStateFlow()

    private val _endRangeInput = MutableStateFlow("")
    val endRangeInput = _endRangeInput.asStateFlow()

    private val _showHandMenu = MutableStateFlow(false)
    val showHandMenu = _showHandMenu.asStateFlow()

    private val _currentHand = MutableStateFlow(Hand.BOTH)
    val currentHand = _currentHand.asStateFlow()

    private val _showMetronomeInput = MutableStateFlow(false)
    val showMetronomeInput = _showMetronomeInput.asStateFlow()

    private val _currentBpmString = MutableStateFlow("...")
    val currentBpmString = _currentBpmString.asStateFlow()

    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale = _zoomScale.asStateFlow()

    private val _visitedStartMeasure = MutableStateFlow<Int?>(null)
    val visitedStartMeasure = _visitedStartMeasure.asStateFlow()

    private val _visitedEndMeasure = MutableStateFlow<Int?>(null)
    val visitedEndMeasure = _visitedEndMeasure.asStateFlow() // 실제 연주한 마지막 마디

    private val _totalMeasures = MutableStateFlow<Int?>(null)
    val totalMeasures = _totalMeasures.asStateFlow()

    private val _startedAt = MutableStateFlow("")
    val startedAt = _startedAt.asStateFlow() // 연주 시작 시간

    private val _isRendering = MutableStateFlow(true)
    val isRendering = _isRendering.asStateFlow()

    private val _isBottomToolbarVisible = MutableStateFlow(true)
    val isBottomToolbarVisible = _isBottomToolbarVisible.asStateFlow()

    // [!!] (추가) 설정 모달 UI 상태
    private val _showSettingsModal = MutableStateFlow(false)
    val showSettingsModal = _showSettingsModal.asStateFlow()

    // [!!] (수정) 기본값을 SettingsRepository의 기본값과 일치시킴
    private val _instrumentVolume = MutableStateFlow(100)
    val instrumentVolume = _instrumentVolume.asStateFlow()

    private val _metronomeVolume = MutableStateFlow(100)
    val metronomeVolume = _metronomeVolume.asStateFlow()

    private val _micSensitivity = MutableStateFlow(1f)
    val micSensitivity = _micSensitivity.asStateFlow()

    private val _pitchTolerance = MutableStateFlow(0.5f)
    val pitchTolerance = _pitchTolerance.asStateFlow()


    val userSheetId: Int = savedStateHandle["userSheetId"] ?: 0
    private val xmlUrl: String = savedStateHandle["xmlUrl"] ?: ""
    val isPracticeMode: Boolean = savedStateHandle["isPracticeMode"] ?: true
    private val startMeasure: Int =  // 연습모드면 세이브스테이트 핸들 활용, 아니면 1번.
        if (isPracticeMode) (savedStateHandle["startMeasure"] ?: 1) else 1

    // OSMD.kt의 내부 로직을 복제하기 위한 상태 변수
    private var micWasOnBeforePlayback = false

    // API 2.2 호출 완료 후 화면을 닫으라는 단방향 이벤트
    private val _navigateBackEvent = MutableStateFlow(false)
    val navigateBackEvent: StateFlow<Boolean> = _navigateBackEvent.asStateFlow()


    init {
        Log.d(TAG, "전달받은 값: $startMeasure")
        Log.d(TAG, "현재 연습모드인가?: $isPracticeMode")
        // ViewModel이 생성되면, 전달받은 URL로 즉시 렌더링 준비
        // (만약 userSheetId를 받아와서 UseCase를 호출해야 한다면
        //  Practice(userSheetId: Int)로 변경하고 UseCase를 호출해야 함)

        // *현재 시나리오: Login에서 더미 URL을 직접 전달받음*
        // startMeasure는 3으로 고정해 시작 시 제대로 3번으로 이동하는지 테스트
        _uiState.value = PracticeUiState.Success(xmlUrl = xmlUrl, startMeasure = startMeasure)
        osmd.setZoom(zoomScale.value)
        _practiceStart.value = startMeasure  // 우선 연습 시작과 끝을 받아온 값으로 정함
        _startRangeInput.value = startMeasure.toString()

        _startedAt.value = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        setupOsmdListeners() // 뷰모델 생성 시 리스너 설정
        loadInitialSettings()
    }

    private fun loadInitialSettings() {
        viewModelScope.launch {
            // DataStore에서 저장된 값을 딱 한 번(first) 가져옴
            val settings = settingsRepository.settingsFlow.first()

            // ViewModel의 StateFlow에 값을 반영
            _instrumentVolume.value = settings.instrumentVolume
            _metronomeVolume.value = settings.metronomeVolume

            Log.d(TAG, "loadInitialSettings: ${_micSensitivity.value}")
            // [!!] 중요: osmd.set... 함수들은 여기(init)서 호출하면 안 됩니다.
            // WebView가 아직 준비되지 않았기 때문입니다.
            // 적용은 PracticeScreen.kt의 onRender에서 합니다.
        }
    }

    fun onShowSettingsModal() { _showSettingsModal.value = true }
    fun onDismissSettingsModal() { _showSettingsModal.value = false }

    // [!!] (수정) B. 기존 핸들러들에 "저장" 로직 추가
    fun onInstrumentVolumeChange(percent: Int) {
        val p = percent.coerceIn(0, 200)
        _instrumentVolume.value = p
        osmd.setMasterVolumePercent(p)
        viewModelScope.launch { settingsRepository.updateInstrumentVolume(p) } // [!!] (추가) 저장
    }

    fun onMetronomeVolumeChange(percent: Int) {
        val p = percent.coerceIn(0, 100)
        _metronomeVolume.value = p
        osmd.setMetronomeVolumePercent(p)
        viewModelScope.launch { settingsRepository.updateMetronomeVolume(p) } // [!!] (추가) 저장
    }

    private fun setupOsmdListeners() {
        // osmd.playbackStateChangedListener = { state -> ... }
        // osmd.setVisitedMeasuresListener { min, max -> ... }
        // 등등... OSMD.kt에 콜백을 추가하고 여기서 ViewModel 상태를 업데이트

        osmd.setPracticeRangeListener { start, end ->
            // JS에서 받은 마디 번호를 ViewModel의 StateFlow에 즉시 반영
            // (viewModelScope.launch는 메인 스레드에서 값을 안전하게 변경하기 위함)
            viewModelScope.launch {
                _practiceStart.value = start
                _practiceEnd.value = end
                _startRangeInput.value = start?.toString() ?: ""
                _endRangeInput.value = end?.toString() ?: ""
            }
        }

        osmd.setVisitedMeasuresListener { min, max ->
            _visitedStartMeasure.value = min
            _visitedEndMeasure.value = max
        }

        osmd.setHighlightStateListener { isOn ->
            viewModelScope.launch {
                _isHighlightOn.value = isOn
            }
        }
    }

    fun onToggleBottomToolbar() {
        _isBottomToolbarVisible.value = !_isBottomToolbarVisible.value
    }

    fun onHighlightToggle() {
        // ViewModel의 상태를 직접 바꾸지 않습니다.
        // JS에 토글 명령을 보내고, JS가 `updateHighlightState` 인터페이스를 통해
        // 새 상태를 다시 보내주면 `setHighlightStateListener`가 상태를 업데이트합니다. (단방향 데이터 흐름)
        osmd.toggleHighlight()
    }

    // 렌더가 끝나면 악보의 끝을 선택
    fun onRenderFinished(totalMeasures: Int) {
        _practiceEnd.value = totalMeasures
        _endRangeInput.value = totalMeasures.toString()
        _totalMeasures.value = totalMeasures

        _isRendering.value = false
    }

    // UI 이벤트 핸들러
    // [수정] UI 이벤트 핸들러
    fun onPlayPauseToggle() {
        val newState = !_isPlaying.value
        _isPlaying.value = newState

        if (newState) {
            // === 재생 시작 ===
            osmd.play()

            // ViewModel이 마이크 상태를 직접 관리
            if (_isMicOn.value) {
                micWasOnBeforePlayback = true // 켜져 있었다고 기억
                _isMicOn.value = false      // UI를 'Off'로 변경
                // osmd.stopNativeMic()은 OSMD.kt가 내부적으로 이미 호출함
            }
        } else {
            // === 재생 중지 ===
            osmd.pause()

            // ViewModel이 마이크 상태를 직접 복원
            if (micWasOnBeforePlayback) {
                micWasOnBeforePlayback = false
                _isMicOn.value = true       // UI를 'On'으로 변경
                // osmd.startNativeMic()은 OSMD.kt가 내부적으로 이미 호출함
            }
        }
    }

    fun onGoToFirstMeasureClick() {
        _isPlaying.value = false

        // osmd.stop()은 OSMD.kt의 stopPlaybackScript를 호출하며,
        // 이 스크립트는 pause()와 setPlaybackStart() (인수 없음)를 실행합니다.
        // setPlaybackStart()는 기본적으로 1번 마디로 이동시킵니다.
        osmd.stop()

        // ViewModel이 마이크 상태를 직접 복원 (UI 동기화)
        // onStopClick과 동일한 로직입니다.
        if (micWasOnBeforePlayback) {
            micWasOnBeforePlayback = false
            _isMicOn.value = true
            // 실제 마이크 재시작은 OSMD.kt의 playbackStateChanged 콜백이 처리합니다.
        }
    }

    fun onStopClick() {
        _isPlaying.value = false
        if (practiceStart.value != null) {
            // 2. 설정 O: 연습 구간의 시작점으로 이동합니다.
            osmd.backToPracticeStart()
            osmd.pause()  // 곡 재생 멈춤
        } else {
            // 3. 설정 X: 곡의 가장 처음(1마디)으로 이동합니다.
            osmd.stop()
        }

        // ViewModel이 마이크 상태를 직접 복원
        if (micWasOnBeforePlayback) {
            micWasOnBeforePlayback = false
            _isMicOn.value = true
            // osmd.startNativeMic()은 OSMD.kt가 내부적으로 이미 호출함
        }
    }

    fun onMicToggle() {
        if (isPlaying.value) return

        if (!isPracticeMode && _isMicOn.value) {
            Log.d(TAG, "onMicToggle: 평가 모드에서는 마이크를 끌 수 없습니다.")
            return
        }

        val newState = !_isMicOn.value
        _isMicOn.value = newState

        if (isPlaying.value) {
            // 재생 중일 때:
            // 1. UI만 업데이트
            // 2. "나중에 켜야 함"을 기억
            if (newState) {
                micWasOnBeforePlayback = true
            } else {
                micWasOnBeforePlayback = false
            }
            // osmd.start/stopNativeMic()은 호출하지 않음.
            // (OSMD.kt의 startNativeMic()가 어차피 "Deferred"로 처리)
        } else {
            // 재생 중이 아닐 때:
            // 1. UI 업데이트
            // 2. 실제 마이크 제어
            if (newState) {
                osmd.startNativeMic(this.isPracticeMode)
            } else {
                osmd.stopNativeMic()
            }
        }
    }

    fun onRepeatToggle() {
        // [!!] 수정: val newState를 먼저 계산합니다.
        val newState = !_isRepeatOn.value
        _isRepeatOn.value = newState
        osmd.setRepeatEnabled(newState)

        // [!!] 추가: newState(반복 켜짐/꺼짐)에 따라 프리카운트를 설정/해제합니다.
        if (newState) {
            // 반복 켜기: 1마디 프리카운트 활성화
            osmd.setPrecount(true, 1)
        } else {
            // 반복 끄기: 프리카운트 비활성화
            osmd.setPrecount(false)
        }
    }

    fun onMetronomeToggle() {
        _isMetronomeOn.value = !_isMetronomeOn.value
        osmd.setMetronomeEnabled(_isMetronomeOn.value)
    }

    fun onZoomIn() {
        val newZoom = (zoomScale.value + 0.1f).coerceAtMost(2.0f) // 최대 2배
        _zoomScale.value = newZoom
        osmd.setZoom(newZoom)
    }

    fun onZoomOut() {
        val newZoom = (zoomScale.value - 0.1f).coerceAtLeast(0.5f) // 최소 0.5배
        _zoomScale.value = newZoom
        osmd.setZoom(newZoom)
    }

    // --- 구간 제어 ---
    fun onShowRangeInput() {
        if (_showRangeInput.value) {
            // 1. 이미 열려있으면 닫기 (onDismissRangeInput 로직 실행)
            onDismissRangeInput()
        } else {
            // 2. 닫혀있으면 열기
            _showRangeInput.value = true
            osmd.startPracticePick()
        }
    }
    fun onDismissRangeInput() { _showRangeInput.value = false
        osmd.stopPracticePick()
    }
    fun onStartRangeChange(text: String) { _startRangeInput.value = text }
    fun onEndRangeChange(text: String) { _endRangeInput.value = text }

    fun onConfirmRangeInput() {
        val start = _startRangeInput.value.toIntOrNull()
        val end = _endRangeInput.value.toIntOrNull()
        _practiceStart.value = start
        _practiceEnd.value = end
        _showRangeInput.value = false // 팝업을 닫는다
        osmd.stopPracticePick() // [추가] 팝업이 닫히면 JS 클릭 모드도 끈다.
        if (start != null && end != null) {
            osmd.setPracticeRange(start, end)
        }
    }

    fun onAllRange() {
        // 1. 렌더링 완료 시 저장해둔 총 마디 수를 가져옵니다.
        val total = _totalMeasures.value
        if (total == null || total <= 0) {
            // (안전장치) 아직 총 마디 수를 모르면 아무것도 하지 않습니다.
            Log.w(TAG, "onClearRange called but totalMeasures is not set.")
            return
        }

        // 2. 상태를 'null'이 아닌 '전체 범위' (1부터 total까지)로 설정합니다.
        _practiceStart.value = 1
        _practiceEnd.value = total
        _startRangeInput.value = "1"
        _endRangeInput.value = total.toString()

        // 3. 팝업을 닫고 클릭 모드를 중지합니다.
        _showRangeInput.value = false
        osmd.stopPracticePick()

        // 4. [중요] JS의 clearPracticeRange() 대신 setPracticeRange()를 호출합니다.
        // 이렇게 해야 JS의 state.active가 true가 됩니다.
        osmd.setPracticeRange(1, total)
    }

    // --- 손 선택 ---
    fun onShowHandMenu() {
        if (_showHandMenu.value) {
            // 1. 이미 열려있으면 닫기
            onDismissHandMenu()
        } else {
            // 2. 닫혀있으면 열기 (및 다른 팝업 닫기)
            _showHandMenu.value = true
        }
    }
    fun onDismissHandMenu() { _showHandMenu.value = false }
    fun onSelectHand(hand: Hand) {
        _currentHand.value = hand
        _showHandMenu.value = false
        osmd.setPianoHandMode(
            when (hand) {
                Hand.LEFT -> OSMD.PianoHandMode.LEFT
                Hand.RIGHT -> OSMD.PianoHandMode.RIGHT
                Hand.BOTH -> OSMD.PianoHandMode.BOTH
            }
        )
    }

    // --- BPM 제어 ---
    fun onShowMetronomeInput() {
        if (_showMetronomeInput.value) {
            // 1. 이미 열려있으면 "저장 없이" 닫기
            onCancelMetronomeInput()
        } else {
            // 2. 닫혀있으면 열기
            _showMetronomeInput.value = true
        }
    }
    fun onDismissMetronomeInput() { // 저장
        val bpmInt = _currentBpmString.value.toIntOrNull()
        Log.d(TAG, "onDismissMetronomeInput: bpmInt: $bpmInt")
        
        if (bpmInt == null || bpmInt !in 1..200) {
            //  실패: 토스트 띄우기
            Toast.makeText(context, "BPM은 1에서 200 사이로 입력해주세요.", Toast.LENGTH_SHORT).show()

            if (bpmInt == null || bpmInt <= 0)
                _currentBpmString.value = "1"
            else
                _currentBpmString.value = "200"
            // 모달을 닫지 않고 상태 유지 (return)
            return
        }

        _currentBpmString.value = bpmInt.toString() // (이미 입력된 값이므로)
        _showMetronomeInput.value = false
        osmd.setBpm(bpmInt)
    }

    private fun onCancelMetronomeInput() {
        _showMetronomeInput.value = false
    }

    fun onBpmChange(bpmString: String) {
        _currentBpmString.value = bpmString
        // 텍스트 입력 중에도 즉시 OSMD에 반영 (선택 사항)
        // bpmString.toIntOrNull()?.let { osmd.setBpm(it) }
    }

    // --- 화면 종료 ---
    fun onExitScreen() {
        // 완주 여부 판단: 방문한 마지막 마디 >= 총 마디 수 (단, 0은 제외)
        val hasFinished = (_visitedEndMeasure.value ?: 0) >= (_totalMeasures.value ?: 0)
                && (_totalMeasures.value ?: 0) > 0

        if (isPracticeMode) {
            // 연습 모드 -> 무조건 '저장' 모달
            _exitDialogType.value = ExitDialogType.SAVE
        } else {
            // 2. 평가 모드
            if (hasFinished) {
                // 2a. 완주함 -> '저장' 모달
                _exitDialogType.value = ExitDialogType.SAVE
            } else {
                // 2b. 완주 못함 -> '경고' 모달
                _exitDialogType.value = ExitDialogType.WARNING
            }
        }
    }

    // 모달 닫기
    fun onDismissExitDialog() {
        _exitDialogType.value = ExitDialogType.NONE
    }

    // [신규] 모달 "OK" 버튼 (API 2.2 호출)
    fun onConfirmExit() {
        _exitDialogType.value = ExitDialogType.NONE // 모달 닫기

        if (_isMicOn.value) {
            osmd.stopNativeMic() // 마이크 중지 (파일 저장 트리거)
        }

        if (isPracticeMode) {
            // --- 1. 연습 모드 (빠른 저장) ---
            val record = createPlayRecord(wavUrl = null)
            // viewModelScope로 빠른 저장 실행
            viewModelScope.launch {
                saveRecordAndNavigate(record, showNotification = false)
            }
        } else {
            // --- 2. 평가 모드 (백그라운드 작업) ---

            // 백그라운드 작업 시작 알림 (Toast)
            Toast.makeText(context, "평가 저장을 시작합니다.\n완료되면 알림으로 알려드립니다.", Toast.LENGTH_LONG).show()

            // applicationScope로 백그라운드 작업 시작
            @SuppressLint("MissingPermission")
            applicationScope.launch {
                val wavUri: Uri? = osmd.getRecordedFileUri()
                val downloadUrl: String? = if (wavUri != null) {
                    Log.d(TAG, "S3 업로드 시작... Uri: $wavUri")
                    uploadFileUseCase(uri = wavUri, uploadPath = "wav")
                        .onFailure { Log.e(TAG, "S3 업로드 실패", it) }
                        .getOrNull()
                } else {
                    Log.w(TAG, "녹음된 WAV Uri가 없음")
                    null
                }

                if (downloadUrl == null && wavUri != null) {
                    // S3 업로드가 실패한 경우
                    NotificationHelper.showEvaluationErrorNotification(
                        context,
                        "파일 업로드에 실패하여 평가를 진행할 수 없습니다."
                    )
                    return@launch // 작업 중단
                }

                // [!!] 18. API 호출 (평가 요청)
                val record = createPlayRecord(wavUrl = downloadUrl)
                saveRecordAndNavigate(record, showNotification = true)
            }

            // [!!] 19. Toast를 띄우고 '즉시' 화면 닫기
            _navigateBackEvent.value = true
        }
    }

    // 헬퍼: PlayRecord 객체 생성
    private fun createPlayRecord(wavUrl: String?): PlayRecord {
        val finalStartMeasure = _visitedStartMeasure.value ?: 1
        val finalEndMeasure = _visitedEndMeasure.value ?: _practiceEnd.value ?: finalStartMeasure

        return PlayRecord(
            userSheetId = this.userSheetId,
            practiceMode = this.isPracticeMode,
            startMeasure = finalStartMeasure,
            endMeasure = finalEndMeasure,
            startedAt = _startedAt.value,
            wavXmlUrl = wavUrl // wavXmlUrl 필드 전달
        )
    }

    // 헬퍼 함수 수정 (알림 분기)
    @SuppressLint("MissingPermission")
    private suspend fun saveRecordAndNavigate(record: PlayRecord, showNotification: Boolean) {
        // UseCase가 Result<PlayResult> (playId만 있음)를 반환
        savePlayRecordUseCase(record)
            .onSuccess { endPlayResult ->
                Log.d(TAG, "saveRecordAndNavigate: ${endPlayResult.playId}에 저장 성공")
            }
            .onFailure { exception ->
                Log.e(TAG, "savePlayRecord: Failure", exception)
                if (showNotification) {
                    //  '평가 요청' 실패 시에만 알림
                    NotificationHelper.showEvaluationErrorNotification(
                        context = context,
                        errorMessage = exception.message ?: "평가 요청에 실패했습니다."
                    )
                }
            }

        // 연습 모드(빠른 저장)일 때만 여기서 네비게이션
        if (!showNotification) {
            _navigateBackEvent.value = true
        }
    }

    override fun onCleared() {
        releaseResources()
        super.onCleared()
    }

    // OSMD 자원 해제
    private fun releaseResources() {
        // 1. 마이크(AudioRecord) 스레드 중지 및 해제
        osmd.stopNativeMic()

        // 2. 재생 중지
        osmd.stop()

        // 3. (가장 중요) WebView의 JS 컨텍스트와 콜백을 정리
        //    (OSMD.kt에 이 함수를 추가해야 함)
        osmd.destroy()
    }

    fun onNavigationComplete() {
        _navigateBackEvent.value = false
    }

    fun setLoadStarted() {
        _hasLoadStarted.value = true
    }

    // '경고' 모달 확인 로직
    fun onConfirmWarningExit() {
        _exitDialogType.value = ExitDialogType.NONE // 모달 닫기
        _navigateBackEvent.value = true // 네비게이션 이벤트 발생
    }
}
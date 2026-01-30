package com.d102.crescendo.presentation.ui.screen.practice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d102.crescendo.R
import com.d102.crescendo.presentation.theme.ActiveToggle
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.DarkActive
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.Normal
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.presentation.ui.component.loading.CustomLoadingIndicator
import kotlin.math.roundToInt


@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 사용자가 팝업에서 '허용'을 누름 -> ViewModel 로직 실행
            viewModel.onMicToggle()
        } else {
            // 사용자가 팝업에서 '거부'를 누름 -> 토스트 메시지
            Toast.makeText(context, "음 인식을 위해서는 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPracticeMode = viewModel.isPracticeMode

    // ViewModel의 OSMD 인스턴스와 옵션을 가져옴
    val osmd = viewModel.osmd
    val options = viewModel.osmdOptions

    // ViewModel의 모든 UI 상태를 구독
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isMicOn by viewModel.isMicOn.collectAsStateWithLifecycle()
    val isRepeatOn by viewModel.isRepeatOn.collectAsStateWithLifecycle()
    val isMetronomeOn by viewModel.isMetronomeOn.collectAsStateWithLifecycle()
    val practiceStart by viewModel.practiceStart.collectAsStateWithLifecycle()
    val practiceEnd by viewModel.practiceEnd.collectAsStateWithLifecycle()
    val showRangeInput by viewModel.showRangeInput.collectAsStateWithLifecycle()
    val startRangeInput by viewModel.startRangeInput.collectAsStateWithLifecycle()
    val endRangeInput by viewModel.endRangeInput.collectAsStateWithLifecycle()
    val showHandMenu by viewModel.showHandMenu.collectAsStateWithLifecycle()
    val currentHand by viewModel.currentHand.collectAsStateWithLifecycle()
    val showMetronomeInput by viewModel.showMetronomeInput.collectAsStateWithLifecycle()
    val currentBpmString by viewModel.currentBpmString.collectAsStateWithLifecycle()
    val exitDialogType by viewModel.exitDialogType.collectAsStateWithLifecycle()
    val visitedEndMeasure by viewModel.visitedEndMeasure.collectAsStateWithLifecycle()
    val startedAt by viewModel.startedAt.collectAsStateWithLifecycle()
    val hasLoadStarted by viewModel.hasLoadStarted.collectAsStateWithLifecycle()
    val isHighlightOn by viewModel.isHighlightOn.collectAsStateWithLifecycle()
    val navigateBack by viewModel.navigateBackEvent.collectAsStateWithLifecycle()

    val isBottomToolbarVisible by viewModel.isBottomToolbarVisible.collectAsStateWithLifecycle()
    val showSettingsModal by viewModel.showSettingsModal.collectAsStateWithLifecycle()
    val instrumentVolume by viewModel.instrumentVolume.collectAsStateWithLifecycle()
    val metronomeVolume by viewModel.metronomeVolume.collectAsStateWithLifecycle()
    val micSensitivity by viewModel.micSensitivity.collectAsStateWithLifecycle()
    val pitchTolerance by viewModel.pitchTolerance.collectAsStateWithLifecycle()

    val isRendering by viewModel.isRendering.collectAsStateWithLifecycle()

    BackHandler(enabled = true) {
        viewModel.onExitScreen()
    }

    // navigateBackEvent가 true가 되면 뒤로가기 실행
    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            onBackClick()
            viewModel.onNavigationComplete() // 이벤트 소비
        }
    }
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        when (val state = uiState) {
            is PracticeUiState.Loading -> {
                CustomLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    title = "악보를 그리고 있어요", // 문구 변경
                    textColor = Black,
                    iconColor = Black
                )
            }

            is PracticeUiState.Error -> {
                Text(
                    text = state.message,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is PracticeUiState.Success -> {
                // --- OSMD 뷰 렌더링 ---
                osmd.OSMDView(
                    musicXML = state.xmlUrl, // ViewModel에서 받은 S3 URL 전달
                    options = options,  // 옵션 전달
                    hasLoadStarted = hasLoadStarted,
                    setLoadStarted = viewModel::setLoadStarted,

                    // [!!] 이 onRender 람다 블록을 통째로 교체하세요.
                    onRender = {
                        // [검증 로그 4] Kotlin 콜백 "시작"
                        Log.d("Verify", "4. Kotlin onRender() CALLED")

                        osmd.setZoom(viewModel.zoomScale.value)

                        osmd.setMasterVolumePercent(viewModel.instrumentVolume.value)
                        osmd.setMetronomeVolumePercent(viewModel.metronomeVolume.value)

                        // [!!] 1. goToMeasure 호출을 여기에서 제거합니다.
                        osmd.getOriginalBpm { originalBpm ->
                            // ViewModel의 BPM 상태를 악보의 원본 값으로 업데이트
                            viewModel.onBpmChange(originalBpm?.toString() ?: "120")
                            // JS 라이브러리에도 이 BPM을 즉시 설정 (상태 동기화)
                            osmd.setBpm(originalBpm ?: 120)
                        }

                        // 위임된 프로퍼티(practiceStart)를 로컬 변수(start, end)에 복사.
                        osmd.getTotalMeasures { totalMeasures ->
                            // 5. 콜백 안에서 ViewModel의 '끝' 상태를 업데이트
                            viewModel.onEndRangeChange(totalMeasures.toString())
                            viewModel.onRenderFinished(totalMeasures)

                            // 6a. state.startMeasure는 SavedStateHandle에서 온 값
                            val savedStartMeasure = state.startMeasure

                            // 6b. 완주 여부 판단
                            val isFinished = isPracticeMode &&
                                    (savedStartMeasure >= totalMeasures) &&
                                    (totalMeasures > 0)

                            // 6c. 최종 시작 마디 결정
                            val finalStartMeasure = if (isFinished) {
                                1 // 완주했으면 1번 마디
                            } else {
                                savedStartMeasure // 중간이었으면 저장된 마디
                            }

                            // [!!] 2. goToMeasure를 이 위치로 이동시킵니다.
                            // finalStartMeasure 값으로 스크롤과 커서를 설정합니다.
                            Log.d("Verify", "5. osmd.goToMeasure(${finalStartMeasure}) CALLED")
                            osmd.goToMeasure(finalStartMeasure)
                            osmd.resetVisitedToMeasure(finalStartMeasure)

                            // 6d. JS 라이브러리에 최종 시작/끝 구간 설정
                            osmd.setPracticeRange(finalStartMeasure, totalMeasures)
                        }
                    }
                )

                // --- 2. 컨트롤 UI (Z-Index 1: 오버레이) ---
                PracticeControlsOverlay(
                    // 연습모드에 따라 달라지는 컨트롤 패널
                    isPracticeMode = isPracticeMode,
                    isPlaying = isPlaying,
                    isMicOn = isMicOn,
                    isRepeatOn = isRepeatOn,
                    isMetronomeOn = isMetronomeOn,
                    isHighlightOn = isHighlightOn,
                    isBottomToolbarVisible = isBottomToolbarVisible,
                    onToggleBottomToolbar = viewModel::onToggleBottomToolbar,
                    onSettingsClick = viewModel::onShowSettingsModal,

                    // 구간 상태 (주석 2, 3)
                    practiceStart = practiceStart,
                    practiceEnd = practiceEnd,
                    showRangeInput = showRangeInput,
                    startRangeInput = startRangeInput,
                    endRangeInput = endRangeInput,
                    // 손 선택 상태 (주석 6)
                    showHandMenu = showHandMenu,
                    currentHand = currentHand, // (표시용)

                    // [신규] 메트로놈 상태
                    showMetronomeInput = showMetronomeInput,
                    currentBpmString = currentBpmString,

                    onPlayPauseToggle = viewModel::onPlayPauseToggle,
                    onMicToggle = {
                        when (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )) {
                            // 권한이 있는 경우
                            PackageManager.PERMISSION_GRANTED -> {
                                // 즉시 ViewModel 로직 실행
                                viewModel.onMicToggle()
                            }
                            // 권한이 없는 경우
                            else -> {
                                // 권한 요청 팝업 띄우기
                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    onRepeatToggle = viewModel::onRepeatToggle,
                    onMetronomeToggle = viewModel::onMetronomeToggle,
                    onGoToFirstMeasureClick = viewModel::onGoToFirstMeasureClick,
                    onStopClick = viewModel::onStopClick,
                    onBackClick = viewModel::onExitScreen,
                    onAllRangeClick = viewModel::onAllRange,
                    onZoomInClick = viewModel::onZoomIn,
                    onZoomOutClick = viewModel::onZoomOut,
                    onShowRangeInput = viewModel::onShowRangeInput,
                    onDismissRangeInput = viewModel::onDismissRangeInput,
                    onConfirmRangeInput = { start, end ->
                        // ViewModel의 함수는 파라미터를 받지 않으므로,
                        // 여기서는 텍스트 변경(onStartRangeChange)이 이미 ViewModel에서
                        // 처리되었다고 가정하고 ViewModel의 함수를 그냥 호출합니다.
                        viewModel.onConfirmRangeInput()
                    },
                    onStartRangeChange = viewModel::onStartRangeChange,
                    onEndRangeChange = viewModel::onEndRangeChange,
                    onShowHandMenu = viewModel::onShowHandMenu,
                    onDismissHandMenu = viewModel::onDismissHandMenu,
                    onSelectHand = viewModel::onSelectHand,
                    onShowMetronomeInput = viewModel::onShowMetronomeInput,
                    onDismissMetronomeInput = viewModel::onDismissMetronomeInput,
                    onBpmChange = viewModel::onBpmChange,
                    onHighlightToggle = viewModel::onHighlightToggle,
                )

                // 렌더링 중일 때 어두운 오버레이와 로딩 UI 표시
                if (isRendering) {
                    // 어두운 배경 (Scrim)
                    Surface(
                        color = Gray3,
                        modifier = Modifier.fillMaxSize()
                    ) {}

                    // 커스텀 로딩 인디케이터 (가운데 정렬)
                    CustomLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        title = "악보를 그리고 있어요", // 로딩 문구
                        textColor = Black,
                        iconColor = Black,
                    )
                }

                // [신규] 테스트용 API 2.2 확인 모달
                when (exitDialogType) {
                    ExitDialogType.SAVE -> {
                        // 1. 기존처럼 데이터 계산
                        val finalStartMeasure = viewModel.visitedStartMeasure.value ?: 1
                        val finalEndMeasure = viewModel.visitedEndMeasure.value ?: viewModel.practiceEnd.value ?: finalStartMeasure
                        val finalPracticeMode = viewModel.isPracticeMode

                        // 2. [신규] 이 데이터를 Map으로 만듭니다.
                        val practiceInfoMap = mapOf(
                            "연주 모드" to if (finalPracticeMode) "연습" else "평가",
                            "시작한 마디" to finalStartMeasure.toString(),
                            "도달한 마디" to finalEndMeasure.toString(),
                            "연주 시작" to startedAt
                        )

                        // 3. 커스텀 모달에 Map을 전달
                        CustomPracticeExitDialog(
                            onDismiss = viewModel::onDismissExitDialog,
                            onConfirm = viewModel::onConfirmExit,
                            title = "연습 저장",
                            text = "연주 기록을 저장하고 나가시겠습니까?",
                            confirmButtonText = "저장 후 종료",
                            dismissButtonText = "취소",
                            practiceInfo = practiceInfoMap // [!!] (추가)
                        )
                    }

                    ExitDialogType.WARNING -> {
                        CustomPracticeExitDialog(
                            onDismiss = viewModel::onDismissExitDialog,
                            onConfirm = viewModel::onConfirmWarningExit,
                            title = "종료 확인",
                            text = "아직 연주를 마치지 않았습니다.\n이대로 종료하면 기록이 저장되지 않습니다.\n정말 종료하시겠어요?",
                            confirmButtonText = "종료",
                            dismissButtonText = "취소",
                            practiceInfo = null // [!!] (추가) 경고창엔 정보 없음
                        )
                    }

                    ExitDialogType.NONE -> {
                        // 아무것도 안 함
                    }
                }

                if (showSettingsModal) {
                    SettingsModal(
                        instrumentVolume = instrumentVolume,
                        metronomeVolume = metronomeVolume,
                        micSensitivity = micSensitivity,
                        pitchTolerance = pitchTolerance,
                        onInstrumentVolumeChange = { viewModel.onInstrumentVolumeChange(it) },
                        onMetronomeVolumeChange = { viewModel.onMetronomeVolumeChange(it) },
                        onDismiss = viewModel::onDismissSettingsModal
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeControlsOverlay(
    isPracticeMode: Boolean,
    isPlaying: Boolean,
    isMicOn: Boolean,
    isRepeatOn: Boolean,
    isMetronomeOn: Boolean,
    isHighlightOn: Boolean,
    practiceStart: Int?,
    practiceEnd: Int?,
    showRangeInput: Boolean,
    startRangeInput: String,
    endRangeInput: String,
    showHandMenu: Boolean,
    currentHand: Hand,
    showMetronomeInput: Boolean,
    currentBpmString: String,
    isBottomToolbarVisible: Boolean,

    onPlayPauseToggle: () -> Unit,
    onMicToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onMetronomeToggle: () -> Unit,
    onStopClick: () -> Unit,
    onGoToFirstMeasureClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    onAllRangeClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,

    onShowRangeInput: () -> Unit,
    onDismissRangeInput: () -> Unit,
    onConfirmRangeInput: (String, String) -> Unit,
    onStartRangeChange: (String) -> Unit,
    onEndRangeChange: (String) -> Unit,
    onShowHandMenu: () -> Unit,
    onDismissHandMenu: () -> Unit,
    onSelectHand: (Hand) -> Unit,
    onShowMetronomeInput: () -> Unit,
    onDismissMetronomeInput: () -> Unit,
    onBpmChange: (String) -> Unit,
    onHighlightToggle: () -> Unit,
    onToggleBottomToolbar: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // --- 상단 컨트롤 ---
        // (반투명 배경 위에 배치하여 가독성 확보)
        Surface(
            color = Black.copy(alpha = 0.3f), // Surface 내부에서 Black 사용
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 0.dp, bottom = 0.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- 1. 왼쪽 그룹 (뒤로가기 버튼) ---
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "뒤로가기",
                        tint = White
                    )
                }

                // --- 2. 오른쪽 그룹 ---
                //    이 버튼들을 별도의 Row로 묶어서 오른쪽에 함께 있도록
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // 버튼 사이 간격
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleBottomToolbar) {
                        Icon(
                            painter = painterResource(R.drawable.ic_hide_toolbar), // TODO: 이 아이콘을 drawable에 추가하세요
                            contentDescription = "툴바 숨기기",
                            tint = White
                        )
                    }
                    IconButton(onClick = onZoomOutClick) {
                        Icon(
                            painter = painterResource(R.drawable.outline_zoom_out_24),
                            contentDescription = "줌 아웃",
                            tint = White
                        )
                    }

                    IconButton(onClick = onZoomInClick) {
                        Icon(
                            painter = painterResource(R.drawable.outline_zoom_in_24),
                            contentDescription = "줌 인", // (오타 수정)
                            tint = White
                        )
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "상세 설정",
                            tint = White
                        )
                    }
                }
            }
        }

        // --- 하단 컨트롤 ---
        AnimatedVisibility(
            visible = isBottomToolbarVisible,
            modifier = Modifier.align(Alignment.BottomCenter), // align을 Column에서 여기로 이동
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isPracticeMode) {  // 연습모드 일 때만
                    // [신규] 메트로놈 UI (가장 위에 쌓임)
                    AnimatedVisibility(
                        visible = showMetronomeInput,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        MetronomeInputSurface(
                            bpmString = currentBpmString,
                            onBpmChange = onBpmChange,
                            onDismiss = onDismissMetronomeInput
                        )
                    }
                    // 손 선택
                    AnimatedVisibility(
                        visible = showHandMenu,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        HandSelectionSurface(
                            currentHand = currentHand,
                            onSelectHand = onSelectHand,
                            onDismiss = onDismissHandMenu,

                            )
                    }
                    // [추가] 주석 3, 4: 구간 입력 UI
                    AnimatedVisibility(
                        visible = showRangeInput,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        RangeInputSurface(
                            start = startRangeInput,
                            end = endRangeInput,
                            onStartChange = onStartRangeChange,
                            onEndChange = onEndRangeChange,
                            onConfirm = { onConfirmRangeInput(startRangeInput, endRangeInput) },
                            onDismiss = onDismissRangeInput,
                            onAll = { // [신규] "All" 버튼 클릭 시
                                onAllRangeClick() // 상태 초기화
                                onDismissRangeInput() // 팝업 닫기
                            }
                        )
                    }
                }


                // --- 하단 메인 컨트롤 ---
                Surface(
                    color = Black.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp), // 높이 줄임
                        horizontalArrangement = if (isPracticeMode) Arrangement.SpaceBetween else Arrangement.Center, // 평가모드면 중앙정렬
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 왼쪽 클러스터 (연습 모드)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if(isPracticeMode){
                            // 마이크 토글
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = White,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable(
                                        enabled = isPracticeMode || !isMicOn // 연습모드이면 언제나 가능, 평가모드면 마이크 꺼진 때만 능
                                    ) { onMicToggle() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isMicOn) R.drawable.baseline_mic_on_24
                                        else R.drawable.baseline_mic_off_24
                                    ),
                                    contentDescription = "마이크",
                                    tint = if (isMicOn) ActiveToggle else DarkActive,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            // [추가] 주석 1: "연주 인식 중" 텍스트
                            val textAlpha by animateFloatAsState(
                                targetValue = if (isMicOn) 1f else 0f,
                                label = "MicTextAlpha"
                            )
                            val micText = if (isPracticeMode) "연주 인식 중..." else "평가를 위한 녹음 중..."
                            Text(
                                text = micText,
                                color = ActiveToggle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .alpha(textAlpha) // [!!] 2. Modifier.alpha() 적용
                            )
                        }
                            else{
                                FloatingActionButton(
                                    onClick = onMicToggle,
                                    containerColor = if (isMicOn) ActiveToggle else White,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isMicOn) R.drawable.baseline_mic_on_24
                                                else R.drawable.baseline_mic_off_24
                                            ),
                                            contentDescription = "연주 평가",
                                            tint = if (isMicOn) White else DarkActive,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = if (isMicOn) "평가 중..." else "평가 시작하기",
                                            color = if (isMicOn) White else DarkActive,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (isPracticeMode) {
                                // 반복재생 토글
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = White,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onRepeatToggle() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isRepeatOn) R.drawable.outline_repeat_24
                                            else R.drawable.ic_one // TODO: 1회 재생 아이콘
                                        ),
                                        contentDescription = "반복재생",
                                        tint = if (isRepeatOn) ActiveToggle else DarkActive,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 구간 선택
                                if (practiceStart == null || practiceEnd == null) {
                                    // 1. 구간 미설정 시: 아이콘 표시
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = White,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onShowRangeInput() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.outline_arrow_range_24),
                                            contentDescription = "구간 선택",
                                            tint = DarkActive,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    // 2. 구간 설정 시: 텍스트 표시
                                    Surface(
                                        onClick = onShowRangeInput,
                                        shape = RoundedCornerShape(8.dp),
                                        color = White
                                    ) {
                                        Text(
                                            text = "$practiceStart - $practiceEnd",
                                            color = ActiveToggle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                    // [삭제] X 버튼이 팝업 내부의 "All"로 이동했으므로 여기서 삭제
                                }
                            }

                        }

                        if (isPracticeMode) {
                            // 가운데 클러스터 (재생 제어)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SmallFloatingActionButton(
                                    onClick = onStopClick,
                                    containerColor = White,
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_stop_24),
                                        contentDescription = "정지",
                                        tint = DarkActive,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                FloatingActionButton(
                                    onClick = onPlayPauseToggle,
                                    containerColor = White,
                                    modifier = Modifier.size(52.dp),
                                    shape = RoundedCornerShape(24.dp)                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isPlaying) R.drawable.baseline_pause_24
                                            else R.drawable.baseline_play_arrow_24
                                        ),
                                        contentDescription = "재생 및 일시정지",
                                        tint = DarkHover,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = onGoToFirstMeasureClick,
                                    containerColor = White,
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_replay_one),
                                        contentDescription = "1번 마디로",
                                        tint = DarkActive,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }


                        if (isPracticeMode) {
                            // [추가] 오른쪽 클러스터 (주석 5, 6)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = White,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onHighlightToggle() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_highlight_filled),
                                        contentDescription = "하이라이트",
                                        tint = if (isHighlightOn) ActiveToggle else DarkActive,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = White,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onMetronomeToggle() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_metronome),
                                        contentDescription = "메트로놈",
                                        tint = if (isMetronomeOn) ActiveToggle else DarkActive,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                // [신규] BPM 표시 텍스트 (클릭 시 팝업)
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .background(
                                            color = White,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onShowMetronomeInput() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp), // 텍스트 내부 패딩
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "BPM: $currentBpmString",
                                        color = DarkActive,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // [추가] 주석 6:
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = White,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onShowHandMenu() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = currentHand.iconRes),
                                        contentDescription = "손 선택",
                                        tint = DarkActive,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}

/**
 * [신규] 메트로놈 BPM 입력 UI
 */
@Composable
private fun MetronomeInputSurface(
    bpmString: String,
    onBpmChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 슬라이더와 텍스트필드의 값을 동기화
    val bpmInt = bpmString.toIntOrNull() ?: 1
    val sliderValue = bpmInt.toFloat()

    Surface(
        color = Black.copy(alpha = 0.8f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 왼쪽: 선택 버튼들
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("BPM:", color = White, fontSize = 14.sp)

                // 슬라이더
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        // 슬라이더 값을 문자열로 변환하여 onBpmChange 호출
                        onBpmChange(it.roundToInt().toString())
                    },
                    valueRange = 1f..200f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF3D5F8F), // 슬라이더 thumb (동그란 부분)
                        activeTrackColor = Color(0xFF3D5F8F), // 선택된 부분
                        inactiveTrackColor = Gray3 // 선택 안된 부분
                    )
                )

                // 텍스트 입력
                NumberTextField(
                    value = bpmString,
                    onValueChange = { textValue ->
                        onBpmChange(textValue.take(3))
                    },
                    modifier = Modifier.width(56.dp) // 3자리를 위해 너비 약간 늘림
                )
            }

            // 오른쪽: 닫기 버튼
            IconButton(onClick = {
                val sanitizedBpm = (bpmString.toIntOrNull() ?: 0)
                onBpmChange(sanitizedBpm.toString())
                onDismiss()
            }) {
                Icon(Icons.Default.Check, contentDescription = "확인", tint = ActiveToggle)
            }
        }
    }
}

/**
 * [추가] 주석 3, 4: 하단 바 위에 표시될 구간 입력 UI
 */
@Composable
private fun RangeInputSurface(
    start: String,
    end: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onAll: () -> Unit,
) {
    Surface(
        color = Black.copy(alpha = 0.8f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "All",
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onAll() } // [신규] 클릭 시 onClear 호출
                    .padding(horizontal = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("연습 구간:", color = White, fontSize = 14.sp)
                Spacer(Modifier.width(16.dp))
                NumberTextField(value = start, onValueChange = onStartChange)
                Text(" - ", color = White, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                NumberTextField(value = end, onValueChange = onEndChange)
            }
            Row {
                IconButton(onClick = onConfirm) {
                    Icon(Icons.Default.Check, contentDescription = "확인", tint = ActiveToggle)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = White)
                }
            }
        }
    }
}

/**
 * [추가] 주석 6: 손 선택
 */
@Composable
private fun HandSelectionSurface(
    currentHand: Hand,
    onSelectHand: (Hand) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = Black.copy(alpha = 0.8f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 왼쪽: 선택 버튼들
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("연주 파트:", color = White, fontSize = 14.sp)

                Hand.values().forEach { hand ->
                    HandButton(
                        hand = hand,
                        isSelected = currentHand == hand,
                        onClick = { onSelectHand(hand) }
                    )
                }
            }
            // 오른쪽: 닫기 버튼
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Check, contentDescription = "확인", tint = ActiveToggle)
            }
        }
    }
}

/**
 * [신규] HandSelectionSurface 내부에서 사용할 버튼
 */
@Composable
private fun HandButton(
    hand: Hand, // [수정] Hand 객체를 직접 받음
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 선택됐을 때의 색상
    val bgColor = if (isSelected) White else Color.Transparent
    val contentColor = if (isSelected) DarkActive else White

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        contentColor = contentColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = hand.iconRes), // hand 객체에서 아이콘 사용
                contentDescription = hand.displayName,
                tint = contentColor
            )
            Text(text = hand.displayName, style = TextStyle(fontWeight = FontWeight.Bold))
        }
    }
}
/**
 * [추가] 구간 입력을 위한 작은 텍스트 필드
 */
@Composable
private fun NumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
        modifier = modifier
            .width(48.dp)
            .background(White, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        textStyle = TextStyle(
            color = Black,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(DarkHover)
    )
}

// [신규] 테스트용 확인 모달 Composable
@Composable
private fun CustomPracticeExitDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String,
    dismissButtonText: String,
    practiceInfo: Map<String, String>?
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = White,
            modifier = Modifier.fillMaxWidth(0.85f).padding(top = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 아이콘
                Surface(
                    shape = CircleShape,
                    color = White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_music),
                            contentDescription = null,
                            tint = DarkHover,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. 타이틀
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkHover,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 3. 본문
                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = DarkHover.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )

                // 4. 연주 정보
                if (practiceInfo != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkHover.copy(alpha = 0.05f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            practiceInfo.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkHover.copy(alpha = 0.7f)
                                    )

                                    Text(
                                        text = value,
                                        fontSize = 13.sp,
                                        color = DarkHover,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 취소 버튼
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Gray3,
                        onClick = onDismiss
                    ) {
                        Text(
                            text = dismissButtonText,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkHover.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }

                    // 확인 버튼
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = DarkHover,
                        onClick = onConfirm
                    ) {
                        Text(
                            text = confirmButtonText,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
//@Composable
//private fun CustomPracticeExitDialog(
//    onDismiss: () -> Unit,
//    onConfirm: () -> Unit,
//    title: String,
//    text: String,
//    confirmButtonText: String,
//    dismissButtonText: String,
//    practiceInfo: Map<String, String>? // [!!] (추가) Map 파라미터
//) {
//    // Dialog를 사용해 전체 화면 커스텀
//    Dialog(onDismissRequest = onDismiss) {
//        Surface(
//            shape = RoundedCornerShape(16.dp),
//            color = White,
//            modifier = Modifier.fillMaxWidth(0.85f) // 가로 모드이므로 75%
//        ) {
//            Column(
//                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
//            ) {
//                // 1. 타이틀
//                Text(
//                    text = title,
//                    fontSize = 22.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = DarkHover,
//                    modifier = Modifier.fillMaxWidth(),
//                    textAlign = TextAlign.Center
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // 2. 본문
//                Text(
//                    text = text,
//                    fontSize = 16.sp,
//                    color = DarkHover,
//                    modifier = Modifier.fillMaxWidth(),
//                    textAlign = TextAlign.Center,
//                    fontWeight = FontWeight.Medium,
//                    lineHeight = 20.sp // 줄 간격
//                )
//
//                // [!!] (추가) 3. 연주 정보 (practiceInfo가 null이 아닐 때만 표시)
//                if (practiceInfo != null) {
//                    Spacer(modifier = Modifier.height(12.dp))
//                    // 구분선
//                    androidx.compose.material3.HorizontalDivider(color = GrayLine)
//                    Spacer(modifier = Modifier.height(12.dp))
//
//                    // 정보 리스트
//                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                        practiceInfo.forEach { (key, value) ->
//                            Row {
//                                // Key (예: "연주 모드")
//                                Text(
//                                    text = key,
//                                    fontSize = 14.sp,
//                                    fontWeight = FontWeight.Bold,
//                                    color = DarkActive, // 키는 좀 더 진하게
//                                    modifier = Modifier.width(80.dp) // Key 정렬을 위해 너비 고정
//                                )
//                                // Value (예: "연습")
//                                Text(
//                                    text = value,
//                                    fontSize = 12.sp,
//                                    color = DarkHover, // 값은 좀 더 연하게
//                                    fontWeight = FontWeight.SemiBold
//                                )
//                            }
//                        }
//                    }
//                    Spacer(modifier = Modifier.height(16.dp))
//                } else {
//                    // practiceInfo가 없으면 원래 간격 유지
//                    Spacer(modifier = Modifier.height(24.dp))
//                }
//
//
//                // 4. 버튼 (오른쪽 정렬)
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    // 취소 버튼
//                    TextButton(onClick = onDismiss) {
//                        Text(dismissButtonText, fontWeight = FontWeight.Medium, color = DarkHover)
//                    }
//                    Spacer(modifier = Modifier.width(8.dp))
//
//                    // 확인 버튼 (테마 색상)
//                    TextButton(onClick = onConfirm) {
//                        Text(confirmButtonText, fontWeight = FontWeight.Bold, color = ActiveToggle)
//                    }
//                }
//            }
//        }
//    }
//}

// Composable이 활성화/비활성화될 때 화면 방향을 잠그고 해제
@Composable
private fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(Unit) {  // 블록 영역은컴포저블이 처음 화면에 그려질 때 단 한 번 실행됨

        // 현재 액티비티를 찾은 다음,
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}

        val window = activity.window

        // 현재 화면 방향을 백업해두고
        val originalOrientation = activity.requestedOrientation

        // 기존에는 입력 시 스크롤 어떻게 되는지 저장
        val originalSoftInputMode = activity.window.attributes.softInputMode

        // 기존 시스템 상태바 상태 저장
        val originalFlags = window.attributes.flags

        // 파라미터로 받은 방향으로 화면을 고정함. 현재 위에서 LANDSCAPE로 넘겨주고 있음.
        activity.requestedOrientation = orientation

        // SoftInputMode를 'adjustPan'으로 강제
        // 키보드가 UI를 밀어 올리지 않고, 윈도우만 패닝함)
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)  // 상태바 숨기도록 함

        onDispose {  // 컴포저블이 화면에서 사라질 때 실행됨
            activity.requestedOrientation = originalOrientation
            activity.window.setSoftInputMode(originalSoftInputMode)
            window.attributes = window.attributes.apply {
                flags = originalFlags
            }
        }
    }
}

/**
 * [신규] 설정 모달 (전체 화면 오버레이)
 */
@Composable
private fun SettingsModal(
    instrumentVolume: Int,
    metronomeVolume: Int,
    micSensitivity: Float,
    pitchTolerance: Float,
    onInstrumentVolumeChange: (Int) -> Unit,
    onMetronomeVolumeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 모달 배경 (어둡게)
    Surface(
        color = Black.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() } // 배경 클릭 시 닫기
    ) {
        // 모달 컨텐츠 (중앙 정렬)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // 모달 흰색 배경
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = White,
                modifier = Modifier
                    .fillMaxWidth(0.9f) // 90% 사용
                    .clickable(enabled = false, onClick = {}) // 모달 내부 클릭은 막기
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp) // 간격 증가
                ) {
                    // 1. 타이틀
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "설정",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = DarkHover
                            )
                        }
                    }

                    // 2. 첫 번째 줄 (볼륨 2개)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(40.dp)
                    ) {
                        SettingsSlider(
                            modifier = Modifier.weight(1f),
                            title = "악기 볼륨",
                            value = instrumentVolume.toFloat(),
                            onValueChange = { onInstrumentVolumeChange(it.roundToInt()) },
                            valueRange = 0f..200f,
                            labelMin = "작게",
                            labelMax = "크게"
                        )
                        SettingsSlider(
                            modifier = Modifier.weight(1f),
                            title = "메트로놈 볼륨",
                            value = metronomeVolume.toFloat(),
                            onValueChange = { onMetronomeVolumeChange(it.roundToInt()) },
                            valueRange = 0f..100f,
                            labelMin = "작게",
                            labelMax = "크게"
                        )
                    }
                }
            }
        }
    }
}

/**
 * [신규] 설정 모달용 커스텀 슬라이더
 */
@Composable
private fun SettingsSlider(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    labelMin: String,
    labelMax: String
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4F.dp)
    ) {
        // 1. 타이틀
        Text(
            text = title,
            fontSize = 14.sp,
            color = DarkActive,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )

        // 2. 슬라이더
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = DarkHover,
                activeTrackColor = DarkActive,
                inactiveTrackColor = Gray3
            )
        )

        // 3. 라벨
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = labelMin,
                fontSize = 14.sp,
                color = DarkHover,
                maxLines = 1
            )
            Text(
                text = labelMax,
                fontSize = 14.sp,
                color = DarkHover,
                maxLines = 1
            )
        }
    }
}

// Compose의 LocalContext.current가 반환하는 Context는 Activity가 아닐 수 있음.
// 대부분 Activity를 감싸고 있는 ContextWrapper임
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this  // 현재 Context가 Activity인가? -> 맞으면 반환.
    is ContextWrapper -> baseContext.findActivity()  // 아니면, ContextWrapper인가? -> 맞으면, 그 안의 baseContext를 꺼내서 1번부터 다시 시도.
    else -> null
}
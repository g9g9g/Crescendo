package com.d102.crescendo.presentation.ui.screen.mysheets

import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.performance.Metric
import com.d102.crescendo.domain.model.performance.PlayResult
import com.d102.crescendo.domain.model.sheet.MySheetDetail
import com.d102.crescendo.domain.model.sheet.Performance
import com.d102.crescendo.presentation.theme.*
import com.d102.crescendo.presentation.ui.component.loading.CustomLoadingIndicator
import com.d102.crescendo.presentation.ui.component.mysheet.ArrangementGenreDialog
import com.d102.crescendo.presentation.ui.screen.main.GlobalLoadingViewModel
import com.d102.crescendo.util.TierBadge
import com.d102.crescendo.util.getGenreName
import com.d102.crescendo.util.matchThumbnail
import com.d102.crescendo.util.rememberDownloadHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MySheetsDetailScreen(
    userSheetId: Int,
    isCompleted: Boolean,
    onNavigateToPractice: (Int, String, Int, Boolean) -> Unit,
    globalLoadingViewModel : GlobalLoadingViewModel,
    viewModel: MySheetDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val currentIsCompleted by viewModel.isCompleted.collectAsState()

    DisposableEffect(lifecycleOwner, userSheetId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    Log.d("MySheetsDetail", "화면 재진입 - 데이터 새로고침")
                    viewModel.loadSheetDetail(userSheetId)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(userSheetId) {
        viewModel.loadSheetDetail(userSheetId)
    }

    when (val state = uiState) {
        is MySheetDetailUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CustomLoadingIndicator()
            }
        }

        is MySheetDetailUiState.Success -> {
            MySheetDetailContent(
                sheetDetail = state.sheetDetail,
                isCompleted = currentIsCompleted,
                onNavigateToPractice = onNavigateToPractice,
                globalLoadingViewModel = globalLoadingViewModel,
                viewModel = viewModel
            )
        }

        is MySheetDetailUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = state.message ?: "상세 조회 실패", color = Gray)
                    Button(
                        onClick = { viewModel.retry(userSheetId) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkHover)
                    ) { Text("다시 시도") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySheetDetailContent(
    sheetDetail: MySheetDetail,
    isCompleted: Boolean,
    onNavigateToPractice: (Int, String, Int, Boolean) -> Unit,
    globalLoadingViewModel: GlobalLoadingViewModel,
    viewModel: MySheetDetailViewModel
) {
    val context = LocalContext.current
    val (onDownload, receiverCleaner) = rememberDownloadHandler(context)
    val scope = rememberCoroutineScope()

    // 연주 결과 다이얼로그 상태
    var showPlayResultDialog by remember { mutableStateOf(false) }
    val playResultState by viewModel.playResultState.collectAsState()

    // 바텀시트 상태 관리
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // AI 편곡 다이얼로그 상태 관리
    var showArrangementDialog by remember { mutableStateOf(false) }

    // AI 편곡 상태
    val arrangeState by viewModel.arrangeState.collectAsState()

    // 어떤 장르로 편곡 중인지 (로딩 문구용)
    var arrangingGenreName by remember { mutableStateOf<String?>(null) }

    // 편곡 상태에 따라 로딩/토스트 처리
    LaunchedEffect(arrangeState) {
        when (val state = arrangeState) {
            ArrangeUiState.Idle -> Unit

            ArrangeUiState.Loading -> {
                val genre = arrangingGenreName ?: "선택한 장르"
                globalLoadingViewModel.showLoading("${genre} 장르로\n악보를 편곡하는 중입니다...")
            }

            ArrangeUiState.Success -> {
                globalLoadingViewModel.hideLoading()
                Toast.makeText(
                    context,
                    "${arrangingGenreName ?: "선택한 장르"} 장르로 편곡이 완료되었습니다!",
                    Toast.LENGTH_SHORT
                ).show()
                // 편곡 후 상태 초기화 및 상세 재조회 (원하면)
                viewModel.resetArrangeState()
                viewModel.loadSheetDetail(sheetDetail.userSheetId)
            }

            is ArrangeUiState.Error -> {
                globalLoadingViewModel.hideLoading()
                Toast.makeText(
                    context,
                    state.message,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetArrangeState()
            }
        }
    }

    // 더미 연주 기록 데이터 (UI 테스트용)
//    val dummyPerformances = remember {
//        listOf(
//            Performance(
//                performanceId = 1,
//                totalScore = 95,
//                comment = "완벽한 연주였습니다!",
//                endedAt = "2024-03-15T14:30:00"
//            ),
//            Performance(
//                performanceId = 2,
//                totalScore = 87,
//                comment = "잘했어요! 조금만 더 연습하면 완벽할 거예요.",
//                endedAt = "2024-03-14T10:20:00"
//            ),
//            Performance(
//                performanceId = 3,
//                totalScore = 72,
//                comment = "박자가 조금 불안정했습니다.",
//                endedAt = "2024-03-13T16:45:00"
//            ),
//            Performance(
//                performanceId = 4,
//                totalScore = 68,
//                comment = "더 연습이 필요합니다.",
//                endedAt = "2024-03-12T09:15:00"
//            ),
//            Performance(
//                performanceId = 5,
//                totalScore = 91,
//                comment = "훌륭한 연주! 감정 표현이 좋았어요.",
//                endedAt = "2024-03-11T18:00:00"
//            )
//        )
//    }

    // 더미 연주 결과 데이터 (UI 테스트용)
//    val dummyPlayResult = PlayResult(
//        status = "COMPLETED",
//        totalScore = 95,
//        grade = "A+",
//        comment = "완벽한 연주였습니다! 박자와 음정이 정확했어요.",
//        metrics = listOf(
//            Metric(code = "tempo_stability", name = "템포 안정성", score = 92),
//            Metric(code = "pitch_accuracy", name = "음정 정확도", score = 98),
//            Metric(code = "rhythm_precision", name = "리듬 정확도", score = 95),
//            Metric(code = "dynamics", name = "강약 표현", score = 94)
//        )
//    )

    // Todo: 실제 데이터 대신 더미 데이터 사용 (테스트용) testSheetDetail -> sheetDetail
//    val testSheetDetail = sheetDetail.copy(
//        performances = sheetDetail.performances
//            ?.takeIf { it.isNotEmpty() }
//            ?: dummyPerformances
//    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(White),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // 악보 이미지 카드
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(252.dp)
                        .background(Gray3)
                ) {
                    if (sheetDetail.thumbnailUrl.isNullOrEmpty()) {
                        Icon(
                            painter = painterResource(
                                matchThumbnail(title = sheetDetail.title)
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.Unspecified
                        )
                    } else {
                        // TODO: AsyncImage로 썸네일 표시
                        Icon(
                            painter = painterResource(
                                matchThumbnail(title = sheetDetail.title)
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.Unspecified
                        )
                    }

                    // 티어 아이콘 (왼쪽 상단)
                    if (sheetDetail.tierCode != "" && sheetDetail.tierLevel != 0) {
                        TierBadge(
                            tierCode = sheetDetail.tierCode,
                            tierLevel = sheetDetail.tierLevel,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        )
                    }

                    // 악기 아이콘
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .size(36.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = if (sheetDetail.instrumentId == 1) {
                                painterResource(R.drawable.ic_piano)
                            } else {
                                painterResource(R.drawable.ic_guitar)
                            },
                            contentDescription = null,
                            tint = DarkHover,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // 악보 정보
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${sheetDetail.title}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )

                        Button(
                            modifier = Modifier.height(32.dp),
                            onClick = { showArrangementDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkHover
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 2.dp,
                                focusedElevation = 8.dp
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_ai),
                                    contentDescription = "AI",
                                    tint = White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI 편곡",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = White
                                )
                            }
                        }
                    }
                    Text(
                        text = "${sheetDetail.composer ?: "Unknown"} · ${getGenreName(sheetDetail.genreId)}",
                        fontSize = 14.sp,
                        color = Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 연주 기록 제목
            item {
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 56.dp, bottom = 8.dp),
                    text = "연주 기록",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkHover
                )
            }

            // 연주 기록 없으면 Empty UI
            if (sheetDetail.performances.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "연주 기록이 없습니다.",
                                fontSize = 16.sp,
                                color = Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "연주를 시작해보세요!",
                                fontSize = 16.sp,
                                color = Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // 연주기록 리스트
                itemsIndexed(sheetDetail.performances) { index, performance ->
                    PerformanceRecordCard(
                        performance = performance,
                        onClick = {
                            // 더미 데이터 사용 (테스트용)
//                            viewModel.setDummyPlayResult(dummyPlayResult)
                            // 연주 기록 클릭 시 결과 조회
                            viewModel.loadPlayResult(performance.performanceId)
                            showPlayResultDialog = true
                        }
                    )

                    if (index < sheetDetail.performances.size - 1) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            thickness = 1.dp,
                            color = GrayLine
                        )
                    }
                }
            }
        }

        // 연주 결과 다이얼로그
        if (showPlayResultDialog) {
            PlayResultDialog(
                playResultState = playResultState,
                onDismiss = {
                    showPlayResultDialog = false
                    viewModel.resetPlayResultState()
                }
            )
        }

        // 하단 고정 버튼
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .shadow(8.dp)
                .background(White)
        ) {
            // 구분선
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = GrayLine
            )

            BottomActionBar(
                onDownloadClick = {
                    onDownload(
                        sheetDetail.xmlUrl ?: return@BottomActionBar,
                        sheetDetail.title
                    )
                },
                onPlayClick = {
                    val url = sheetDetail.xmlUrl
                    if (url.isNullOrBlank()) {
                        Toast
                            .makeText(context, "악보 파일이 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                        return@BottomActionBar
                    }
                    // 바텀시트 표시
                    showBottomSheet = true
                }
            )
        }
    }

    // 바텀시트
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            PlayModeBottomSheet(
                isPracticeEnabled = isCompleted,
                onModeSelected = { mode ->
                    showBottomSheet = false
                    val url = sheetDetail.xmlUrl ?: return@PlayModeBottomSheet
                    when (mode) {
                        // Todo: 모드별 isPractice 플래그 전달 로직 추가 필요
                        PlayMode.PRACTICE -> {
                            // 평가모드 로직
                            onNavigateToPractice(
                                sheetDetail.userSheetId,
                                url,
                                sheetDetail.startMeasure,
                                false,
                            )
                        }

                        PlayMode.NORMAL -> {
                            // 연습모드 로직
                            onNavigateToPractice(
                                sheetDetail.userSheetId,
                                url,
                                sheetDetail.startMeasure,
                                true
                            )
                        }
                    }
                },
                onDismiss = { showBottomSheet = false }
            )
        }
    }

    // AI 편곡 다이얼로그
    if (showArrangementDialog) {
        ArrangementGenreDialog(
            onDismiss = { showArrangementDialog = false },
            onGenreSelected = { genreCode, genreName ->
                // 다이얼로그 닫기
                showArrangementDialog = false

                // 어떤 장르로 요청했는지 저장 (로딩 문구용)
                arrangingGenreName = genreName

                // 편곡할 XML URL 체크
                val xmlUrl = sheetDetail.xmlUrl
                if (xmlUrl.isNullOrBlank()) {
                    Toast.makeText(
                        context,
                        "편곡할 악보 파일이 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@ArrangementGenreDialog
                }

                // 실제 편곡 API 호출
                viewModel.arrangeSheet(
                    userSheetId = sheetDetail.userSheetId,
                    style = genreCode,
                    xmlUrl = xmlUrl
                )
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { receiverCleaner() }
    }
}

enum class PlayMode {
    NORMAL,    // 연습모드
    PRACTICE   // 평가모드
}

@Composable
fun PlayModeBottomSheet(
    isPracticeEnabled: Boolean,
    onModeSelected: (PlayMode) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // 연습모드
        PlayModeOption(
            icon = R.drawable.ic_test_mode,
            title = "연습모드",
            description = "정확한 음을 맞추면 악보가 자동으로 넘어갑니다.",
            onClick = { onModeSelected(PlayMode.NORMAL) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 평가모드
        PlayModeOption(
            icon = R.drawable.ic_practice_mode,
            title = "평가모드",
            description = "정확도와 리듬을 분석해 연주 실력을 확인하세요.",
            enabled = isPracticeEnabled,
            overlayText = if (!isPracticeEnabled) "진도율 100% 달성해야 평가모드를 사용할 수 있어요" else null,
            onClick = { onModeSelected(PlayMode.PRACTICE) }
        )
    }
}

@Composable
fun PlayModeOption(
    icon: Int,
    title: String,
    description: String,
    enabled: Boolean = true,
    overlayText: String? = null,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "playModeScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // 높이 자동 조정
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (enabled) {
                        Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        val released = tryAwaitRelease()
                                        isPressed = false
                                        if (released) {
                                            onClick()
                                        }
                                    }
                                )
                            }
                    } else {
                        Modifier // 비활성화일 땐 그냥 박스만
                    }
                )
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(16.dp),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Gray3)
                .border(1.dp, Gray3, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아이콘
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Gray3),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = title,
                    tint = Normal,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 텍스트
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkHover
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray
                )
            }
        }
        // 오버레이 안내 문구 (가운데)
        if (!enabled && overlayText != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 6.dp)
                    )

                    Text(
                        text = overlayText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    onDownloadClick: () -> Unit,
    onPlayClick: () -> Unit = {}
) {
    var isDownloadPressed by remember { mutableStateOf(false) }
    val downloadScale by animateFloatAsState(
        targetValue = if (isDownloadPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "downloadScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 12.dp, bottom = 12.dp, end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = White,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Gray3),
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = downloadScale
                    scaleY = downloadScale
                }

        ) {
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isDownloadPressed = true
                                val released = tryAwaitRelease()
                                isDownloadPressed = false
                            },
                            onTap = {
                                onDownloadClick()
                            }
                        )
                    }) {
                Icon(
                    painter = painterResource(R.drawable.ic_download),
                    contentDescription = "다운로드",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        var isPlayPressed by remember { mutableStateOf(false) }
        val playScale by animateFloatAsState(
            targetValue = if (isPlayPressed) 0.90f else 1f,
            animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
            label = "playScale"
        )

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkHover,
            shadowElevation = 4.dp,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .graphicsLayer {
                scaleX = playScale
                scaleY = playScale
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPlayPressed = true
                                val released = tryAwaitRelease()
                                isPlayPressed = false
                            },
                            onTap = {
                                onPlayClick()
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "연주하기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_music),
                    contentDescription = "연주하기",
                    tint = White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun PerformanceRecordCard(performance: Performance, onClick: () -> Unit = {}) {
    var showAudioDialog by remember { mutableStateOf(false) }
    var isPlayPressed by remember { mutableStateOf(false) }
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "playScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(White)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 재생 버튼
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp).graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                },
                shape = CircleShape,
                color = Normal,
                shadowElevation = 8.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPlayPressed = true
                                val released = tryAwaitRelease()
                                isPlayPressed = false
                            },
                            onTap = {
                                showAudioDialog = true
                            }
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_btn),
                        contentDescription = "재생",
                        tint = White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // 코멘트 / 날짜 / 아이콘
        Row(
            modifier = Modifier
                .weight(1f)
                .height(66.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = performance.comment,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 더보기 아이콘 (점수 위치)
                Icon(
                    painter = painterResource(R.drawable.ic_more),
                    contentDescription = "더보기",
                    tint = Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onClick()
                        }
                )

                // 날짜 (하단)
                Text(
                    text = performance.endedAt.take(10).replace("-", "."),
                    fontSize = 12.sp,
                    color = Gray,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    // 오디오 플레이어 다이얼로그
    if (showAudioDialog) {
        AudioPlayerDialog(
            audioUrl = performance.wavXmlUrl,
            onDismiss = { showAudioDialog = false }
        )
    }
}

@Composable
fun AudioPlayerDialog(
    audioUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // 슬라이더용 상태 (0f ~ 1f)
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    // 사용자가 드래그 중인지 여부
    var isUserSeeking by remember { mutableStateOf(false) }

    // 실시간 재생 위치 업데이트
    LaunchedEffect(isPlaying, isUserSeeking) {
        while (isPlaying && !isUserSeeking) {
            currentPosition = mediaPlayer.currentPosition
            sliderPosition =
                if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                else 0f
            delay(100)
        }
    }

    // MediaPlayer 초기화
    LaunchedEffect(Unit) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(audioUrl)
            mediaPlayer.setOnPreparedListener { mp ->
                duration = mp.duration
                isLoading = false
                isPlaying = true
                mp.start()
                Log.d("AudioPlayerDialog", "음원 준비 완료 - 길이: ${duration}ms")
            }
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                currentPosition = 0
                sliderPosition = 0f
                mediaPlayer.seekTo(0)
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            Log.e("AudioPlayerDialog", "음원 로드 실패: ${e.message}")
            Toast.makeText(context, "음원을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .align(Alignment.Center)
                    .border(1.dp, Gray3, RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 다이얼로그 내부 클릭은 무시 */ },
                shape = RoundedCornerShape(24.dp),
                color = White,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        if (isLoading) {
                            // 로딩 상태
                            CustomLoadingIndicator(title = "음원을 불러오는 중입니다")
                        } else {
                            // 음악 아이콘
                            Icon(
                                painter = painterResource(R.drawable.ic_mp3),
                                contentDescription = "음악",
                                tint = Normal,
                                modifier = Modifier.size(84.dp)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // 드래그 가능한 슬라이더
                            CustomAudioSeekBar(
                                progress = sliderPosition,
                                onChange = { newValue ->
                                    isUserSeeking = true
                                    sliderPosition = newValue
                                },
                                onChangeFinished = {
                                    val newPos = (sliderPosition * duration).toInt()
                                    mediaPlayer.seekTo(newPos)
                                    currentPosition = newPos
                                    isUserSeeking = false
                                }
                            )


                            Spacer(modifier = Modifier.height(8.dp))

                            // 시간 표시
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(currentPosition),
                                    fontSize = 12.sp,
                                    color = Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatTime(duration),
                                    fontSize = 12.sp,
                                    color = Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // 컨트롤 버튼들
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 10초 뒤로
                                IconButton(
                                    onClick = {
                                        val newPosition =
                                            (currentPosition - 10000).coerceAtLeast(0)
                                        mediaPlayer.seekTo(newPosition)
                                        currentPosition = newPosition
                                        sliderPosition =
                                            if (duration > 0)
                                                (newPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                            else 0f
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_10_left),
                                        contentDescription = "10초 뒤로",
                                        tint = Normal,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // 재생/일시정지 버튼
                                Surface(
                                    onClick = {
                                        if (isPlaying) {
                                            mediaPlayer.pause()
                                            isPlaying = false
                                        } else {
                                            mediaPlayer.start()
                                            isPlaying = true
                                        }
                                    },
                                    shape = CircleShape,
                                    color = Normal,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isPlaying) R.drawable.baseline_pause_24
                                                else R.drawable.baseline_play_arrow_24
                                            ),
                                            contentDescription = if (isPlaying) "일시정지" else "재생",
                                            tint = White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                // 10초 앞으로
                                IconButton(
                                    onClick = {
                                        val newPosition =
                                            (currentPosition + 10000).coerceAtMost(duration)
                                        mediaPlayer.seekTo(newPosition)
                                        currentPosition = newPosition
                                        sliderPosition =
                                            if (duration > 0)
                                                (newPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                            else 0f
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_10_right),
                                        contentDescription = "10초 앞으로",
                                        tint = Normal,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // X 버튼 (오른쪽 상단)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "닫기",
                            tint = Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomAudioSeekBar(
    progress: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit
) {
    val barHeight = 4.dp          // 전체 바를 조금 더 얇게
    val thumbWidth = 4.dp         // 세로로 긴 슬라이더 핸들 느낌

    var barWidthPx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .onSizeChanged { size ->
                barWidthPx = size.width.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onChangeFinished() }
                ) { change, _ ->
                    val x = change.position.x
                    if (barWidthPx > 0f) {
                        val newValue = (x / barWidthPx).coerceIn(0f, 1f)
                        onChange(newValue)
                    }
                }
            }
    ) {
        // 회색 트랙
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(barHeight / 2))
                .background(GrayLine)
        )

        // 파란 진행 영역
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(RoundedCornerShape(barHeight / 2))
                .background(Normal)
        )

        // 핸들 (바 높이에 맞는 둥근 막대)
        Box(
            modifier = Modifier
                .offset {
                    // 진행 비율에 따라 X 위치 계산 (양 끝으로 안 튀어나가게 clamp)
                    val thumbPx = thumbWidth.toPx()
                    val x = (progress * barWidthPx - thumbPx / 2)
                        .coerceIn(0f, barWidthPx - thumbPx)
                    IntOffset(x.toInt(), 0)
                }
                .size(width = thumbWidth, height = barHeight)
                .clip(RoundedCornerShape(50))   // 완전 둥근 막대
                .background(Normal)
        )
    }
}


// 시간 포맷 함수 (밀리초 -> MM:SS)
private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    thickness: Dp = 6.dp,
    trackColor: Color = GrayLine,
    progressColor: Color = DarkHover,
    startAngle: Float = -90f,
    rounded: Boolean = false
) {
    val strokeWidthPx = with(LocalDensity.current) { thickness.toPx() }

    Canvas(modifier = modifier) {
        val cap = if (rounded) StrokeCap.Round else StrokeCap.Butt
        val style = Stroke(width = strokeWidthPx, cap = cap)

        val inset = strokeWidthPx / 2f
        val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
        val topLeft = Offset(inset, inset)

        // 트랙
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = style
        )

        // 진행부 (시계방향으로 채워짐)
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped > 0f) {
            drawArc(
                color = progressColor,
                startAngle = startAngle, // -90f (12시 방향)
                sweepAngle = 360f * clamped, // 양수 = 시계방향
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style
            )
        }
    }
}
@Composable
fun PlayResultDialog(
    playResultState: PlayResultUiState,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.85f) // 최대 높이 제한
            .border(1.dp, Gray3, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = White
        ) {
            when (playResultState) {
                is PlayResultUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomLoadingIndicator(title = "연주 평가 결과를 불러오는 중입니다")
                    }
                }
                is PlayResultUiState.Success -> {
                    PlayResultContent(
                        playResult = playResultState.playResult,
                        onDismiss = onDismiss
                    )
                }
                is PlayResultUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "결과를 불러올 수 없습니다",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = playResultState.message,
                                fontSize = 14.sp,
                                color = Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkHover
                                )
                            ) {
                                Text("확인")
                            }
                        }
                    }
                }
                PlayResultUiState.Idle -> {}
            }
        }
    }
}

@Composable
fun PlayResultContent(
    playResult: PlayResult,
    onDismiss: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // 세부 지표 버튼의 Y 위치를 저장
    var detailsButtonY by remember { mutableStateOf(0f) }

    // 등급별 색상
    val gradeColor = when (playResult.grade?.uppercase()) {
        "S", "S+" -> Color(0xFFB71C1C)
        "A", "A+" -> Color(0xFF1565C0)
        "B", "B+" -> Color(0xFF2E7D32)
        "C", "C+" -> Color(0xFFFF9800)
        else -> Gray
    }

    // showDetails 변경 시 스크롤 처리
    LaunchedEffect(showDetails) {
        if (showDetails) {
            // 세부 지표를 열 때: 부드럽게 아래로 스크롤
            scope.launch {
                scrollState.animateScrollTo(
                    value = detailsButtonY.toInt(),
                    animationSpec = tween(durationMillis = 500)
                )
            }
        } else {
            // 세부 지표를 닫을 때: 원래 위치로 스크롤
            scope.launch {
                scrollState.animateScrollTo(
                    value = 0,
                    animationSpec = tween(durationMillis = 500)
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 헤더
            Text(
                text = "연주 결과",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkHover
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 등급 표시 (크게)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                gradeColor.copy(alpha = 0.3f),
                                gradeColor.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(4.dp, gradeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = playResult.grade ?: "?",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = gradeColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 점수 표시
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${playResult.totalScore}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Normal
                )
                Text(
                    text = "점",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 코멘트
            Text(
                text = playResult.comment ?: "연주가 완료되었습니다!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 세부 지표 토글 버튼
            if (!playResult.metrics.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            // 버튼의 Y 위치 저장
                            detailsButtonY = coordinates.positionInParent().y
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(Gray3)
                        .clickable { showDetails = !showDetails }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chart),
                            contentDescription = null,
                            tint = DarkHover,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "세부 지표",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkHover
                        )
                    }
                    Icon(
                        painter = painterResource(
                            if (showDetails) R.drawable.ic_up_arrow
                            else R.drawable.ic_down_arrow
                        ),
                        contentDescription = null,
                        tint = DarkHover,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 세부 지표 상세
                AnimatedVisibility(visible = showDetails) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        playResult.metrics.forEach { metric ->
                            MetricItem(metric = metric)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        // X 버튼 (오른쪽 상단)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "닫기",
                tint = Gray,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun MetricItem(metric: Metric) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = metric.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Black
            )
            Text(
                text = "${metric.score}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Normal
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 프로그레스 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(GrayLine)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(metric.score / 100f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Normal,
                                Normal.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }
    }
}
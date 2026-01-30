package com.d102.crescendo.presentation.ui.screen.servicesheets

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.sheet.Metrics
import com.d102.crescendo.domain.model.sheet.ServiceSheetDetail
import com.d102.crescendo.domain.model.sheet.SimilarSheet
import com.d102.crescendo.presentation.theme.*
import com.d102.crescendo.presentation.ui.component.loading.CustomLoadingIndicator
import com.d102.crescendo.util.TierBadge
import com.d102.crescendo.util.getGenreName
import com.d102.crescendo.util.matchThumbnail
import com.d102.crescendo.util.pressClickEffect
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ServiceSheetsDetailScreen(
    sheetId: Int,
    onSheetClick: (Int) -> Unit = {},
    viewModel: ServiceSheetDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ViewModel의 OSMD 인스턴스와 옵션을 가져옴
    val osmd = viewModel.osmd
    val isOsmdReady by viewModel.isOsmdReady.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPreviewPlaying.collectAsStateWithLifecycle()

    LaunchedEffect(sheetId) {
        viewModel.stopPreview()
        viewModel.load(sheetId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPreview()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.downloadEvents.collect { ev ->
            when (ev) {
                is ServiceSheetDetailViewModel.DownloadEvent.Success -> {
                    Toast.makeText(context, "악보 담기 완료", Toast.LENGTH_SHORT).show()
                }

                is ServiceSheetDetailViewModel.DownloadEvent.Error -> {
                    val message = extractDetailFromError(ev.message)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(1.dp, 1.dp) // 0dp가 아닌 최소 크기 보장
                .graphicsLayer { alpha = 0f } // 렌더링은 하되 GPU에서 숨김
        ) {
            osmd.OSMDView(
                musicXML = "", // [!!] 빈 문자열로 초기화
                options = null,
                hasLoadStarted = isOsmdReady, // ViewModel의 플래그 사용
                setLoadStarted = viewModel::onOsmdReady, // 페이지 로드 완료 시 ViewModel에 알림
                onRender = {
                    // 이 콜백은 빈 문자열 로드 시 호출됨.
                    // 실제 준비 완료 신호는 setLoadStarted(onPageFinished)가 담당
                    Log.d("ServiceSheetDetail", "Hidden OSMDView Initialized (onRender).")
                }
            )
        }
        when (val state = uiState) {
            ServiceSheetDetailUiState.Idle,
            ServiceSheetDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CustomLoadingIndicator()
                }
            }

            is ServiceSheetDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("불러오는 중 오류가 발생했습니다.", color = Gray)
                        state.message?.let { Text(it, color = Gray, fontSize = 12.sp) }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.load(sheetId) },
                            colors = ButtonDefaults.buttonColors(DarkHover)
                        ) { Text("다시 시도", color = White) }
                    }
                }
            }

            is ServiceSheetDetailUiState.Success -> {
                ServiceSheetDetailContent(
                    data = state.data,
                    isPlaying = isPlaying,
                    isReady = isOsmdReady,
                    onSheetClick = { nextId ->
                        viewModel.stopPreview()
                        onSheetClick(nextId)
                    },
                    onDownloadClick = { viewModel.download(sheetId) },
                    onPlayClick = { xmlUrl ->
                        viewModel.togglePreview(xmlUrl)
                    }
                )
            }
        }
    }

}

@Composable
private fun ServiceSheetDetailContent(
    data: ServiceSheetDetail,
    isPlaying: Boolean,
    isReady: Boolean,
    onSheetClick: (Int) -> Unit,
    onDownloadClick: () -> Unit,
    onPlayClick: (String) -> Unit
) {
    val context = LocalContext.current

    // 말풍선 표시 여부 상태
    var showSummaryBubble by remember { mutableStateOf(false) }

    // 차트 애니메이션 트리거
    var triggerChartAnimation by remember { mutableStateOf(false) }

    val tierFloatTransition = rememberInfiniteTransition(label = "tierFloat")
    val tierOffsetY by tierFloatTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f, // 위로 4dp 정도
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tierOffsetY"
    )

    // Metrics가 null이면 기본값 사용
    val metricsToDisplay = data.metrics ?: Metrics(
        tempo = 0.1,
        rhythm = 0.1,
        intervals = 0.1,
        harmony = 0.1,
        technique = 0.1,
        length = 0.1
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
        ) {
            // 썸네일/난이도 배지 + 설명 아이콘 + 떠다니는 말풍선
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(252.dp)
                ) {
                    // 배경 이미지
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Gray3)
                    ) {
                        Icon(
                            painter = painterResource(
                                matchThumbnail(
                                    title = data.title
                                )
                            ),
                            contentDescription = "악보 이미지",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.Unspecified
                        )
                    }

                    // 왼쪽 상단 설명 아이콘 버튼
                    TierBadge(
                        tierCode = data.tierCode,
                        tierLevel = data.tierLevel,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .offset(y = tierOffsetY.dp)
                    ) {
                        // 클릭 시 기존처럼 요약 말풍선 토글
                        showSummaryBubble = !showSummaryBubble
                    }

                    // 떠다니는 말풍선 (조건부 표시)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSummaryBubble,
                        enter = androidx.compose.animation.fadeIn(
                            animationSpec = tween(300)
                        ) + androidx.compose.animation.slideInVertically(
                            animationSpec = tween(300),
                            initialOffsetY = { -20 }
                        ),
                        exit = androidx.compose.animation.fadeOut(
                            animationSpec = tween(300)
                        ) + androidx.compose.animation.slideOutVertically(
                            animationSpec = tween(300),
                            targetOffsetY = { -20 }
                        )
                    ) {
                        FloatingSummaryBubble(
                            summary = data.summary?.takeIf { it.isNotBlank() }
                            ?: "악보 정보가 제공되지 않습니다.",
                            bgColor = getTierColor(data.tierCode, data.tierLevel)
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
                            painter = if (data.instrumentId == 1) {
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

            // 기본 정보
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
                            text = data.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = "다운로드",
                                tint = DarkHover,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = data.downloadNumber.toString(),
                                fontSize = 16.sp,
                                color = Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${data.composer} · ${getGenreName(data.genreId)}",
                        fontSize = 14.sp,
                        color = Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 연습 추천 (데이터가 있을 때만 표시)
            if (data.recommendations.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    RecommendationsSection(recommendations = data.recommendations)
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    EmptyRecommendationsSection()
                }
            }

            // 육각형 레이더 차트
            item {
                RadarChartSection(
                    metrics = metricsToDisplay,
                    shouldAnimate = triggerChartAnimation,
                    onVisible = { triggerChartAnimation = true })
            }

            // 유사한 악보 제목
            item {
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 36.dp, bottom = 8.dp),
                    text = "유사한 악보",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkHover
                )
            }

            // 유사한 악보
            if (data.similarSheets.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(data.similarSheets.size) { idx ->
                            val sheet = data.similarSheets[idx]
                            SimilarSheetCard(
                                sheet = sheet,
                                onClick = { onSheetClick(sheet.sheetId) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 120.dp)
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "유사한 악보가 없습니다.",
                            fontSize = 14.sp,
                            color = Gray
                        )
                    }
                }
            }
        }

        // 하단 고정 버튼
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .shadow(8.dp)
                .background(White)
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = GrayLine
            )
            ServiceBottomActionBar(
                isPlaying = isPlaying,
                isReady = isReady,
                onDownloadClick = onDownloadClick,
                onPlayClick = {
                    val xmlUrl = data.xmlUrlPreview ?: data.xmlUrl // 미리보기 URL 우선 사용
                    if (xmlUrl.isNullOrBlank()) {
                        Toast.makeText(context, "재생할 악보가 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        // [!!] Toast 대신 ViewModel의 함수를 호출합니다.
                        onPlayClick(xmlUrl)
                    }
                }
            )
        }
    }
}

@Composable
private fun FloatingSummaryBubble(summary: String, bgColor: Color = Color(0xFF9BB0D4)) {
    // 위아래로 부드럽게 움직이는 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingY"
    )

    // 투명도 애니메이션
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 60.dp)
            .offset(y = offsetY.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 말풍선
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .background(
                    bgColor.copy(alpha = 0.85f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = summary,
                fontSize = 13.sp,
                color = White,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RecommendationsSection(recommendations: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tip),
            contentDescription = "팁 아이콘",
            tint = Color.Unspecified,
            modifier = Modifier.size(48.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFE8F4F8),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recommendations.forEachIndexed { index, recommendation ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${index + 1}.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray
                        )
                        Text(
                            text = recommendation,
                            fontSize = 14.sp,
                            color = Gray,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun EmptyRecommendationsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tip),
            contentDescription = "팁 아이콘",
            tint = Color.Unspecified,
            modifier = Modifier.size(48.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFE8F4F8),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "악보 팁이 없습니다.",
                fontSize = 14.sp,
                color = Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RadarChartSection(
    metrics: Metrics,
    shouldAnimate: Boolean,
    onVisible: () -> Unit
) {
    val animationProgress = remember { Animatable(0f) }
    var hasAnimated by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // 매트릭스 데이터가 모두 0.1인지 확인 (기본값인지 체크)
    val hasNoData = metrics.tempo == 0.1 &&
            metrics.rhythm == 0.1 &&
            metrics.intervals == 0.1 &&
            metrics.harmony == 0.1 &&
            metrics.technique == 0.1 &&
            metrics.length == 0.1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = "악보 분석",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkHover,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 회색 배경 박스
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Gray3, RoundedCornerShape(16.dp))
                .onGloballyPositioned { coordinates ->
                    // 차트의 위치와 크기 계산
                    val chartTop = coordinates.positionInRoot().y
                    val chartHeight = coordinates.size.height
                    val chartCenter = chartTop + (chartHeight / 2)

                    // 화면 중앙보다 위에 차트의 중심이 있을 때 애니메이션 트리거
                    if (chartCenter < screenHeight && !hasAnimated) {
                        onVisible()
                    }
                }
        ) {
            Box {
                HexagonRadarChart(
                    metrics = metrics,
                    animationProgress = animationProgress.value
                )

                // 데이터가 없을 때 어두운 오버레이와 메시지 표시
                if (hasNoData) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "악보 분석 데이터가 없습니다.",
                            fontSize = 16.sp,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // 애니메이션 트리거
    LaunchedEffect(shouldAnimate) {
        if (shouldAnimate && !hasAnimated) {
            hasAnimated = true
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
            )
        }
    }
}

@Composable
private fun HexagonRadarChart(
    metrics: Metrics,
    animationProgress: Float,
    modifier: Modifier = Modifier
) {
    val labels = listOf("Tempo", "Rhythm", "Intervals", "Harmony", "Technique", "Length")
    val values = listOf(
        metrics.tempo / 10.0,
        metrics.rhythm / 10.0,
        metrics.intervals / 10.0,
        metrics.harmony / 10.0,
        metrics.technique / 10.0,
        metrics.length / 10.0
    )

    val chartSize = 300.dp
    val radiusDp = chartSize / 2.4f           // 그래프 꼭짓점 반지름(dpi 기반)
    val labelRadius = radiusDp + 25.dp        // 라벨은 꼭짓점보다 살짝 바깥쪽

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(chartSize)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2.4f
            val angleStep = 360f / 6

            // 육각형 배경 그리드 (5개 레벨로 더 세밀하게)
            for (level in 1..5) {
                val levelRadius = radius * (level / 5f)
                val hexPath = Path()
                for (i in 0..5) {
                    val angle = Math.toRadians((angleStep * i - 90).toDouble())
                    val x = center.x + levelRadius * cos(angle).toFloat()
                    val y = center.y + levelRadius * sin(angle).toFloat()

                    if (i == 0) hexPath.moveTo(x, y)
                    else hexPath.lineTo(x, y)
                }
                hexPath.close()

                // 그리드 선 두께와 색상 조절
                val strokeWidth = if (level == 5) 4.5f else 1.5f
                val gridColor = DarkHover.copy(alpha = if (level == 5) 0.5f else 0.25f)

                drawPath(
                    path = hexPath,
                    color = gridColor,
                    style = Stroke(width = strokeWidth)
                )
            }

            // 중심에서 각 꼭짓점으로 선
            for (i in 0..5) {
                val angle = Math.toRadians((angleStep * i - 90).toDouble())
                val endX = center.x + radius * cos(angle).toFloat()
                val endY = center.y + radius * sin(angle).toFloat()

                drawLine(
                    color = DarkHover.copy(alpha = 0.3f),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 1.5f
                )
            }

            // 데이터 영역 (애니메이션 적용 - 중심에서 천천히 퍼짐)
            val dataPath = Path()
            for (i in 0..5) {
                val angle = Math.toRadians((angleStep * i - 90).toDouble())
                // 애니메이션 progress에 따라 중심(0)에서 목표값까지
                val animatedValue = values[i] * animationProgress
                val distance = radius * animatedValue.toFloat()
                val x = center.x + distance * cos(angle).toFloat()
                val y = center.y + distance * sin(angle).toFloat()

                if (i == 0) dataPath.moveTo(x, y)
                else dataPath.lineTo(x, y)
            }
            dataPath.close()

            // 그라데이션 느낌의 채우기
            drawPath(
                path = dataPath,
                color = Normal.copy(alpha = 0.35f * animationProgress)
            )

            // 테두리 강조
            drawPath(
                path = dataPath,
                color = Normal.copy(alpha = 0.8f * animationProgress),
                style = Stroke(width = 3f)
            )

            // 각 꼭짓점에 점 표시 (애니메이션 적용)
            for (i in 0..5) {
                val angle = Math.toRadians((angleStep * i - 90).toDouble())
                val animatedValue = values[i] * animationProgress
                val distance = radius * animatedValue.toFloat()
                val x = center.x + distance * cos(angle).toFloat()
                val y = center.y + distance * sin(angle).toFloat()

                // 외곽 원
                drawCircle(
                    color = Normal.copy(alpha = animationProgress),
                    radius = 6f,
                    center = Offset(x, y)
                )
                // 내부 하이라이트
                drawCircle(
                    color = White.copy(alpha = animationProgress),
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }

        // 라벨 배치
        labels.forEachIndexed { index, label ->
            val angle = Math.toRadians((360.0 / 6 * index - 90).toDouble())
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()

            val offsetXDp = labelRadius * cosA
            val offsetYDp = labelRadius * sinA

            Box(
                modifier = Modifier.offset(
                    x = offsetXDp,
                    y = offsetYDp
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkHover
                )
            }
        }
    }
}

@Composable
private fun SimilarSheetCard(
    sheet: SimilarSheet,
    onClick: () -> Unit
) {
    // 눌림 애니메이션을 위한 상태
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "scale"
    )

    Column(
        modifier = Modifier.width(120.dp)
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(120.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(12.dp))
                .background(Gray3)
                .border(1.dp, Gray3, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            val released = tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = {
                            onClick()
                        }
                    )
                }
        ) {
            Icon(
                painter = painterResource(matchThumbnail(title = sheet.title)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = Color.Unspecified
            )

            // 오른쪽 하단 악기 아이콘
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .size(32.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Gray3),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = if (sheet.instrumentId == 1) {
                        painterResource(R.drawable.ic_piano)
                    } else {
                        painterResource(R.drawable.ic_guitar)
                    },
                    contentDescription = "악기 아이콘",
                    tint = DarkHover
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = sheet.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${sheet.composer ?: "Unknown"} • ${getGenreName(sheet.genreId)}",
            fontSize = 12.sp,
            color = Gray,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ServiceBottomActionBar(
    modifier: Modifier = Modifier,
    onDownloadClick: () -> Unit,
    onPlayClick: () -> Unit,
    isPlaying: Boolean,
    isReady: Boolean
) {

    //
    val buttonColor = if (isReady) DarkHover else Gray
    val contentAlpha = if (isReady) 1f else 0.5f

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
            border = BorderStroke(0.5.dp, Gray3),
            modifier = Modifier.size(56.dp).pressClickEffect { onDownloadClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_sheet_download),
                    contentDescription = "다운로드",
                    tint = DarkHover,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = buttonColor,
            shadowElevation = 4.dp,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .pressClickEffect {
                    if (isReady) {
                        onPlayClick()
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "미리듣기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_music_pause
                        else R.drawable.ic_music_play
                    ),
                    contentDescription = if (isPlaying) "정지" else "재생",
                    tint = White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private fun extractDetailFromError(raw: String?): String {
    if (raw.isNullOrBlank()) return "알 수 없는 오류가 발생했습니다."

    return try {
        // 예외 메시지 앞에 okhttp... 이런 prefix가 있을 수 있으니
        val jsonStart = raw.indexOf('{')
        if (jsonStart == -1) return raw

        val json = raw.substring(jsonStart)
        val obj = JSONObject(json)

        // JSON 안에 detail이 있으면 그걸 쓰고, 없으면 raw 전체 사용
        obj.optString("detail", raw)
    } catch (e: Exception) {
        raw
    }
}

@Composable
private fun getTierColor(tierCode: String?, tierLevel: Int?): Color {
    val BronzeColor = Color(0xFFB8860B)
    val SilverColor = Color(0xFFC8C8C8)
    val GoldColor   = Color(0xFFFFC107)

    if (tierCode.isNullOrBlank() || tierLevel == null) return Color(0xFF9BB0D4) // 기본 파란색

    val normalized = tierCode.uppercase()

    return when {
        normalized.startsWith("BRONZE") -> BronzeColor
        normalized.startsWith("SILVER") -> SilverColor
        normalized.startsWith("GOLD")   -> GoldColor
        else -> Color(0xFF9BB0D4)
    }
}
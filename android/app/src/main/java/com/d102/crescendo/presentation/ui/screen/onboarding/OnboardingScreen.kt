package com.d102.crescendo.presentation.ui.screen.onboarding

import GenreSelectionStep
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.onboarding.OnboardingRecommendSheet
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray2
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.Light_Gray
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.presentation.ui.component.loading.CustomLoadingIndicator
import com.d102.crescendo.presentation.ui.component.onboarding.SelectableSheetCard
import kotlin.math.min

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val contentState = uiState as? OnboardingUiState.Content
    BackHandler(
        enabled = contentState != null && contentState.currentStep != OnboardingStep.SELECT_GENRE
    ) {
        // 두 번째 페이지 이상에서만 뒤로가기 가로채서 스텝만 이동
        viewModel.onBackClicked()
    }

    LaunchedEffect(uiState) {
        if (uiState is OnboardingUiState.Complete) {
            onOnboardingComplete()
        }
    }

    Scaffold(
        containerColor = White,
        bottomBar = {
            val contentState = uiState as? OnboardingUiState.Content ?: return@Scaffold

            OnboardingBottomBar(
                step = contentState.currentStep,
                isNextEnabled = contentState.isNextButtonEnabled, // 뷰모델에서 관리하는 enable 상태
                isLoading = contentState.isLoading,               // 로딩 상태
                onNextClick = { viewModel.onNextClicked() },
                onSkipClick = { viewModel.onSkipClicked() }
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is OnboardingUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CustomLoadingIndicator(title = "온보딩 화면을 불러오는 중입니다")
                }
            }

            is OnboardingUiState.Content -> {
                val genreMap: Map<Int, String> =
                    remember(state.genres) { state.genres.associate { it.id to it.korName } }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp),
                ) {
                    Spacer(modifier = Modifier.height(80.dp))
                    OnboardingHeader(step = state.currentStep)
                    Spacer(modifier = Modifier.height(64.dp))

                    when (state.currentStep) {
                        OnboardingStep.SELECT_GENRE -> {
                            GenreSelectionStep(
                                genres = state.genres,
                                selectedGenreIds = state.selectedGenreIds,
                                onGenreClick = viewModel::onGenreSelected
                            )
                        }

                        OnboardingStep.SELECT_INSTRUMENT -> {
                            InstrumentSelectionStep(
                                instruments = state.instruments,
                                selectedInstrumentId = state.selectedInstrumentId,
                                onInstrumentClick = viewModel::onInstrumentSelected
                            )
                        }

                        // 관심 악보 1페이지
                        OnboardingStep.SELECT_SHEET_1 -> {
                            val pageSheets = getPageSheets(
                                all = state.onboardingRecommendSheets,
                                page = 1,
                                pageSize = 6
                            )

                            SheetSelectionStep(
                                page = 1,
                                sheets = pageSheets,
                                selectedSheetIds = state.selectedOnboardingSheetIds,
                                instrumentId = state.selectedInstrumentId!!,
                                genreMap = genreMap,
                                onSheetClick = viewModel::onSheetSelected,
                                onSwipeToPrev = { viewModel.onBackClicked() },
                                onSwipeToNext = { viewModel.onNextClicked() }
                            )
                        }

                        // 관심 악보 2페이지
                        OnboardingStep.SELECT_SHEET_2 -> {
                            val pageSheets = getPageSheets(
                                all = state.onboardingRecommendSheets,
                                page = 2,
                                pageSize = 6
                            )

                            SheetSelectionStep(
                                page = 2,
                                sheets = pageSheets,
                                selectedSheetIds = state.selectedOnboardingSheetIds,
                                instrumentId = state.selectedInstrumentId!!,
                                genreMap = genreMap,
                                onSheetClick = viewModel::onSheetSelected,
                                onSwipeToPrev = { viewModel.onBackClicked() },
                                onSwipeToNext = { viewModel.onNextClicked() }
                            )
                        }

                        // 관심 악보 3페이지
                        OnboardingStep.SELECT_SHEET_3 -> {
                            val pageSheets = getPageSheets(
                                all = state.onboardingRecommendSheets,
                                page = 3,
                                pageSize = 6
                            )

                            SheetSelectionStep(
                                page = 3,
                                sheets = pageSheets,
                                selectedSheetIds = state.selectedOnboardingSheetIds,
                                instrumentId = state.selectedInstrumentId!!,
                                genreMap = genreMap,
                                onSheetClick = viewModel::onSheetSelected,
                                onSwipeToPrev = { viewModel.onBackClicked() },
                                onSwipeToNext = { viewModel.onNextClicked() }
                            )
                        }
                    }


                }
            }

            is OnboardingUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message ?: "알 수 없는 오류가 발생했습니다.")
                }
            }

            is OnboardingUiState.Complete -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader(step: OnboardingStep) {
    when (step) {
        OnboardingStep.SELECT_GENRE -> {
            val titleAnnotated = buildAnnotatedString {
                // 앞부분 회색
                pushStyle(SpanStyle(color = Gray))
                append("선호하는 음악 ")

                // '장르'만 포인트 컬러 + SemiBold
                pop()
                pushStyle(
                    SpanStyle(
                        color = DarkHover,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                append("장르")

                // 뒷부분 다시 회색
                pop()
                pushStyle(SpanStyle(color = Gray))
                append("를 선택해주세요")
                pop()
            }

            Text(
                text = titleAnnotated,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        OnboardingStep.SELECT_INSTRUMENT -> {
            // "어떤 악기로 시작해 볼까요?"에서 '악기'만 포인트 컬러 + SemiBold
            val titleAnnotated = buildAnnotatedString {
                // 기본 회색
                pushStyle(SpanStyle(color = Gray))
                append("어떤 ")

                // '악기' 강조
                pop()
                pushStyle(
                    SpanStyle(
                        color = DarkHover,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                append("악기")

                // 나머지 다시 회색
                pop()
                pushStyle(SpanStyle(color = Gray))
                append("로 시작해 볼까요?")
                pop()
            }

            Text(
                text = titleAnnotated,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            // 이 단계는 서브 타이틀 없음
        }

        OnboardingStep.SELECT_SHEET_1,
        OnboardingStep.SELECT_SHEET_2,
        OnboardingStep.SELECT_SHEET_3 -> {

            // 제목 처리: "관심 있는 악보를 선택해 주세요"
            val titleAnnotated = buildAnnotatedString {
                pushStyle(SpanStyle(color = Gray))
                append("관심 있는 ")

                pop()
                pushStyle(
                    SpanStyle(
                        color = DarkHover,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                append("악보")

                pop()
                pushStyle(SpanStyle(color = Gray))
                append("를 선택해 주세요")
                pop()
            }

            // 서브제목 처리: "선택 시 추천 정확도가 올라가요"
            val subAnnotated = buildAnnotatedString {
                pushStyle(SpanStyle(color = Gray))
                append("선택 시 ")

                pop()
                pushStyle(
                    SpanStyle(
                        color = DarkHover,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                append("추천 정확도")

                pop()
                pushStyle(SpanStyle(color = Gray))
                append("가 올라가요")
                pop()
            }

            Text(
                text = titleAnnotated,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subAnnotated,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

    }
}

@Composable
private fun SheetSelectionStep(
    page: Int,
    sheets: List<OnboardingRecommendSheet>,
    selectedSheetIds: Set<Long>,
    instrumentId: Int,
    genreMap: Map<Int, String>,
    onSheetClick: (Long) -> Unit,
    onSwipeToPrev: () -> Unit,
    onSwipeToNext: () -> Unit
) {
    if (sheets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_warning),
                    contentDescription = null,
                    tint = Gray,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "추천할 악보가 아직 없어요.",
                    color = Gray,
                    fontSize = 16.sp
                )

            }
        }
        return
    }

    val density = LocalDensity.current
    val startOffsetY = with(density) { 40.dp.toPx() }
    val enterOffsetY = remember { Animatable(startOffsetY) }

    val infiniteTransition = rememberInfiniteTransition(label = "sheetFloat")
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sheetFloatOffset"
    )

    var hasEntered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        enterOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )
        hasEntered = true
    }

    val swipeThreshold = 80f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = enterOffsetY.value + if (hasEntered) floatOffsetY else 0f
            }
            .pointerInput(page) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        when {
                            dragAmount > swipeThreshold -> onSwipeToPrev()
                            dragAmount < -swipeThreshold -> onSwipeToNext()
                        }
                    }
                )
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sheets, key = { it.sheetId }) { sheet ->
                SelectableSheetCard(
                    sheet = sheet,
                    genreName = genreMap[sheet.genreId.toInt()] ?: "N/A",
                    isSelected = selectedSheetIds.contains(sheet.sheetId),
                    instrumentIdForIcon = instrumentId,
                    onClick = { onSheetClick(sheet.sheetId) }
                )
            }
        }
    }
}




@Composable
private fun OnboardingBottomBar(
    step: OnboardingStep,
    isNextEnabled: Boolean,
    isLoading: Boolean,
    onNextClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    val isSheetStep = step.name.startsWith("SELECT_SHEET")

    // 관심 악보 단계에서만 살짝 위로
    val bottomPadding = if (isSheetStep) 28.dp else 60.dp

    // 온보딩 전체 5단계에 대한 인덱스 (0~4)
    val currentStepIndex = when (step) {
        OnboardingStep.SELECT_GENRE -> 0
        OnboardingStep.SELECT_INSTRUMENT -> 1
        OnboardingStep.SELECT_SHEET_1 -> 2
        OnboardingStep.SELECT_SHEET_2 -> 3
        OnboardingStep.SELECT_SHEET_3 -> 4
    }

    val isClickable = isNextEnabled && !isLoading
    val buttonBgColor = if (isClickable) DarkHover else GrayLine
    val buttonTextColor = if (isClickable) White else Light_Gray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 모든 단계에서 항상 페이지 인디케이터 표시
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val totalSteps = 5
            repeat(totalSteps) { index ->
                val isActive = currentStepIndex == index
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isActive) DarkHover else GrayLine
                        )
                )
                if (index != totalSteps - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        // 버튼은 항상 존재 / 클릭 여부만 조절
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(buttonBgColor)
                .border(1.dp, buttonBgColor, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = isClickable,
                    onClick = onNextClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val buttonText = when (step) {
                OnboardingStep.SELECT_GENRE -> "다음"
                OnboardingStep.SELECT_INSTRUMENT -> "다음"
                OnboardingStep.SELECT_SHEET_1 -> "다음"
                OnboardingStep.SELECT_SHEET_2 -> "다음"
                OnboardingStep.SELECT_SHEET_3 -> "완료"
            }

            if (isLoading) {
                // 로딩 중에는 인디케이터 + 텍스트
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .width(18.dp),
                        strokeWidth = 2.dp,
                        color = buttonTextColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "저장 중...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = buttonTextColor
                    )
                }
            } else {
                Text(
                    text = buttonText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = buttonTextColor
                )
            }
        }

        // 관심 악보 단계에서만 "건너뛰기" 노출
        if (isSheetStep) {
            TextButton(
                enabled = !isLoading, // 로딩 중에는 건너뛰기도 막을지 선택 (원하면 true로 놔도 됨)
                onClick = onSkipClick
            ) {
                Text("건너뛰기", color = Gray)
            }
        }
    }
}

private fun getPageSheets(
    all: List<OnboardingRecommendSheet>,
    page: Int,
    pageSize: Int
): List<OnboardingRecommendSheet> {
    if (all.isEmpty()) return emptyList()

    val fromIndex = (page - 1) * pageSize
    if (fromIndex >= all.size) return emptyList() // 이 페이지에 보여줄 데이터 없음

    val toIndex = min(fromIndex + pageSize, all.size)
    return all.subList(fromIndex, toIndex)
}





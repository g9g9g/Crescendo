package com.d102.crescendo.presentation.ui.screen.servicesheets

import android.R.attr.contentDescription
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.sheet.PopularSheet
import com.d102.crescendo.domain.model.sheet.ServiceSheet
import com.d102.crescendo.presentation.theme.*
import com.d102.crescendo.presentation.ui.component.loading.CustomLoadingIndicator
import com.d102.crescendo.util.getCcmThumbnail
import com.d102.crescendo.util.getGenreName
import com.d102.crescendo.util.getTierDrawable
import com.d102.crescendo.util.matchThumbnail
import kotlinx.coroutines.delay

enum class FeaturedCategory {
    INSTRUMENT, CLASSIC, KPOP, NEWAGE, OST, KIDS, CCM, TIER
}
data class FeaturedItem(
    val title: String,
    val subtitle: String,
    val category: FeaturedCategory
)
private val featuredItems = listOf(
    FeaturedItem("악기별 악보", "피아노·기타 중심의 다양한 연주곡", FeaturedCategory.INSTRUMENT),
    FeaturedItem("클래식 악보", "서양 전통 음악 감성을 담은 정돈된 선율", FeaturedCategory.CLASSIC),
    FeaturedItem("가요 악보", "듣기 편한 멜로디 중심의 보컬 스타일 곡", FeaturedCategory.KPOP),
    FeaturedItem("뉴에이지 악보", "잔잔하고 서정적인 피아노 기반 연주곡", FeaturedCategory.NEWAGE),
    FeaturedItem("OST 악보", "영화·드라마 분위기의 서사적 멜로디", FeaturedCategory.OST),
    FeaturedItem("동요 악보", "밝고 쉬운 선율의 간단한 곡", FeaturedCategory.KIDS),
    FeaturedItem("CCM 악보", "위로와 평온을 전하는 워십 스타일", FeaturedCategory.CCM),
    FeaturedItem("난이도별 악보", "입문부터 고급까지 단계별 선택", FeaturedCategory.TIER)
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceSheetsScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToClassicSheets: () -> Unit,
    onNavigateToPopSheets: () -> Unit,
    onNavigateToOstSheets: () -> Unit,
    onNavigateToNewageSheets: () -> Unit,
    onNavigateToKidsSheets: () -> Unit,
    onNavigateToCcmSheets: () -> Unit,
    onNavigateToLevelSheets: () -> Unit,
    onNavigateToInstrumentSheets: () -> Unit,
    viewModel: ServiceSheetSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val popularSheets by viewModel.popularSheets.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.loadIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        when (val state = uiState) {
            ServiceSearchUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CustomLoadingIndicator()
                }
            }

            is ServiceSearchUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("데이터를 불러올 수 없습니다", color = Gray)
                        state.message?.let { Text(it, color = Gray, fontSize = 12.sp) }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.retry() },
                            colors = ButtonDefaults.buttonColors(DarkHover)
                        ) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            is ServiceSearchUiState.Success -> {
                if (allItems.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("아직 등록된 서비스 악보가 없습니다.", color = Gray)
                    }
                } else {
                    ServiceSheetsContent(
                        popularSheets = popularSheets,
                        allItems = allItems,
                        onNavigateToDetail = onNavigateToDetail,
                        onFeaturedClick = { category ->
                            when (category) {
                                FeaturedCategory.INSTRUMENT -> onNavigateToInstrumentSheets()
                                FeaturedCategory.CLASSIC -> onNavigateToClassicSheets()
                                FeaturedCategory.KPOP -> onNavigateToPopSheets()
                                FeaturedCategory.NEWAGE -> onNavigateToNewageSheets()
                                FeaturedCategory.OST -> onNavigateToOstSheets()
                                FeaturedCategory.KIDS -> onNavigateToKidsSheets()
                                FeaturedCategory.CCM -> onNavigateToCcmSheets()
                                FeaturedCategory.TIER -> onNavigateToLevelSheets()
                            }
                        }
                    )

                    // 추가 로드
                    LaunchedEffect(state.loadedCount, state.totalCount) {
                        if (state.loadedCount < state.totalCount) {
                            viewModel.loadNext()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceSheetsContent(
    popularSheets: List<PopularSheet>,
    allItems: List<ServiceSheet>,
    onNavigateToDetail: (Int) -> Unit,
    onFeaturedClick: (FeaturedCategory) -> Unit
) {
    val scrollState = rememberScrollState()

    // 최신순으로 정렬 (상위 10개)
    val latestSheets = remember(allItems) {
        allItems.sortedByDescending { it.updatedAt }.take(10)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 큰 박스 슬라이더 섹션 (가운데 크게, 양옆 살짝 보이기)
        FeaturedBoxSlider(onItemClick = onFeaturedClick)

        Spacer(modifier = Modifier.height(32.dp))

        // 인기 악보 섹션 (1~20위, 4개씩 페이지)
        PopularSheetsSection(
            sheets = popularSheets,
            onNavigateToDetail = onNavigateToDetail
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 최신 악보 섹션 (가로 스크롤)
        LatestSheetsSection(
            sheets = latestSheets,
            onNavigateToDetail = onNavigateToDetail
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun FeaturedBoxSlider(
    onItemClick: (FeaturedCategory) -> Unit
) {
    val totalBoxes = 8
    val boxWidth = 280.dp
    val boxHeight = 360.dp
    val pageSpacing = 16.dp

    // 각 박스에 대응하는 이미지 리소스
    val backgroundImages = listOf(
        R.drawable.ic_instrument_section,      // 악기별
        R.drawable.ic_classic_section,  // 클래식 악보
        R.drawable.ic_pop_section,      // 가요 악보
        R.drawable.ic_newage_section,   // 뉴에이지 악보
        R.drawable.ic_ost_section,      // OST 악보
        R.drawable.ic_kidssong_section,      // 동요 악보
        R.drawable.ic_ccm_section,      // CCM 악보
        R.drawable.ic_level_section       // 난이도별
    )

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val sidePadding = ((maxWidth - boxWidth) / 2).coerceAtLeast(0.dp)

        val initial = remember {
            val mid = Int.MAX_VALUE / 2
            mid - (mid % totalBoxes)
        }

        val pagerState = rememberPagerState(
            initialPage = initial,
            pageCount = { Int.MAX_VALUE }
        )

        /**
         * 10초마다 다음 박스로 이동
         */
        LaunchedEffect(pagerState) {
            while (true) {
                delay(5000)
                val next = pagerState.currentPage + 1
                pagerState.animateScrollToPage(next)
            }
        }

        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(boxWidth),
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = pageSpacing
        ) { page ->

            val rawOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val pageOffset = rawOffset.absoluteValue.coerceIn(0f, 1f)

            val minScale = 0.92f
            val minAlpha = 0.75f

            val scale = lerp(minScale, 1f, 1f - pageOffset)
            val alpha = lerp(minAlpha, 1f, 1f - pageOffset)
            val z = lerp(0f, 1f, 1f - pageOffset)

            val idx = ((page % totalBoxes) + totalBoxes) % totalBoxes
            val item = featuredItems[idx]

            val interaction = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .width(boxWidth)
                    .height(boxHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        translationY = lerp(12f, 0f, 1f - pageOffset)

                        shape = RoundedCornerShape(16.dp)
                        clip = true
                        shadowElevation = 0f
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { onItemClick(item.category) }
                    .zIndex(z),
                contentAlignment = Alignment.Center
            ) {
                // 이미지 배경 (텍스트 없음)
                AsyncImage(
                    model = backgroundImages[idx],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun PopularSheetsSection(
    sheets: List<PopularSheet>,
    onNavigateToDetail: (Int) -> Unit
) {
    Column {
        Text(
            text = "인기 악보",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkHover,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // 4개씩 묶어서 페이지 생성
        val pages = remember(sheets) { sheets.chunked(4) }
        if (pages.isEmpty()) {
            Text(
                text = "인기 악보가 아직 없어요.",
                color = DarkHover.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            return
        }

        val pagerState = rememberPagerState(pageCount = { pages.size })

        // 페이지 전체 폭에 맞게 스냅되는 페이저
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fill,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { pageIndex ->
            val pageSheets = pages[pageIndex]
            PopularSheetPage(
                sheets = pageSheets,
                startRank = pageIndex * 4 + 1,
                onNavigateToDetail = onNavigateToDetail
            )
        }

    }
}

@Composable
fun PopularSheetPage(
    sheets: List<PopularSheet>,
    startRank: Int,
    onNavigateToDetail: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .width(340.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        sheets.forEachIndexed { index, sheet ->
            PopularSheetRankItem(
                rank = startRank + index,
                sheet = sheet,
                onClick = { onNavigateToDetail(sheet.sheetId.toInt()) }
            )
        }
    }
}

@Composable
fun PopularSheetRankItem(
    rank: Int,
    sheet: PopularSheet,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(White)
            .clickable { onClick() }
            .padding(start = 12.dp, end = 12.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순위 - 고정 너비 추가
        Box(
            modifier = Modifier.width(28.dp),
        ) {
            Text(
                text = "$rank",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkHover
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 썸네일
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Gray3)
        ) {
            if (sheet.thumbnailUrl.isNullOrEmpty()) {
                Icon(
                    painter = painterResource(matchThumbnail(title = sheet.title)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified
                )
            } else {
                Icon(
                    painter = painterResource(getTierDrawable(sheet.tierCode, sheet.tierLevel)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified
                )
                // Todo: 실제 썸네일 이미지 생기면 주석 해제
//                AsyncImage(
//                    model = sheet.thumbnailUrl,
//                    contentDescription = null,
//                    modifier = Modifier.fillMaxSize(),
//                    contentScale = ContentScale.Crop
//                )
            }
            // 악기 아이콘
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .size(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = if (sheet.instrumentId == 1) {
                        painterResource(R.drawable.ic_piano)
                    } else {
                        painterResource(R.drawable.ic_guitar)
                    },
                    contentDescription = null,
                    tint = DarkHover,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))

        // 정보
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = sheet.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${sheet.composer ?: "Unknown"} • ${getGenreName(sheet.genreId)}",
                fontSize = 12.sp,
                color = Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LatestSheetsSection(
    sheets: List<ServiceSheet>,
    onNavigateToDetail: (Int) -> Unit
) {
    Column {
        Text(
            text = "최신 악보",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkHover,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.width(4.dp))

            sheets.take(20).forEach { sheet ->
                LatestSheetCard(
                    sheet = sheet,
                    onClick = { onNavigateToDetail(sheet.sheetId) }
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun LatestSheetCard(
    sheet: ServiceSheet,
    onClick: () -> Unit
) {
    // 눌림 애니메이션을 위한 상태
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "scale"
    )

    // updatedAt -> yyyy-MM-dd 형태로만 보여주기
    val updatedDateText = remember(sheet.updatedAt) {
        val raw = sheet.updatedAt ?: ""
        if (raw.length >= 10) raw.substring(0, 10) else raw
    }

    Column(
        modifier = Modifier.width(140.dp)
    ) {
        // 악보 이미지
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(140.dp)
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
            if (sheet.thumbnailUrl.isNullOrEmpty()) {
                Icon(
                    painter = painterResource(matchThumbnail(title = sheet.title)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified
                )
            } else {
                AsyncImage(
                    model = sheet.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // 악기 아이콘
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .shadow(4.dp, RoundedCornerShape(32.dp))
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
                    contentDescription = null,
                    tint = DarkHover
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = sheet.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${sheet.composer ?: "Unknown"} • ${getGenreName(sheet.genreId)}",
            fontSize = 12.sp,
            color = Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = updatedDateText,
            fontSize = 10.sp,
            lineHeight = 8.sp,
            color = Gray,
            modifier = Modifier
                .fillMaxWidth(),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}
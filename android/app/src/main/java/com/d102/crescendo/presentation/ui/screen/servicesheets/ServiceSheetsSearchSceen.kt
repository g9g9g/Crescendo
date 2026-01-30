package com.d102.crescendo.presentation.ui.screen.servicesheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.sheet.ServiceSheet
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.LLight_Gray
import com.d102.crescendo.presentation.theme.Light_Gray
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.util.getGenreName
import com.d102.crescendo.util.getInstrumentDrawable
import com.d102.crescendo.util.getTierDrawable
import com.d102.crescendo.util.matchThumbnail
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceSheetsSearchScreen(
    viewModel: ServiceSheetSearchViewModel = hiltViewModel(),
    onNavigateToDetail: (Int) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearchMode by viewModel.isSearchMode.collectAsState()
    val focusManager = LocalFocusManager.current

    val imePaddingValues = WindowInsets.ime.asPaddingValues()
    val bottomImePadding = imePaddingValues.calculateBottomPadding()

    val recommendedKeywords by viewModel.recommendedKeywords.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    // 트렌딩 검색어 & 기준 시간
    val trendingSearches by viewModel.trendingSearches.collectAsState()
    val trendingGeneratedAt by viewModel.trendingGeneratedAt.collectAsState()

    LaunchedEffect(Unit) {
//        viewModel.loadRecommendedKeywordsIfNeeded()
        viewModel.loadTrendingSearches()
    }

    // 더미 데이터 (실제로는 ViewModel에서 관리)
//    val recommendedSearches = listOf("물에 빠진 나이프", "데이식스", "귀멸의 칼날", "진격의 거인", "아이유")
//    val popularSearches = listOf(
//        "Summer",
//        "blue0717",
//        "pretender",
//        "클래식",
//        "Summer",
//        "blue0717",
//        "pretender",
//        "클래식",
//        "Summer",
//        "blue0717",
//        "pretender",
//        "클래식"
//    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                onClick = {
                    // 외부 클릭 시 자동완성 숨기기 및 포커스 해제
                    viewModel.clearSuggestions()
                    focusManager.clearFocus()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 검색 입력 필드
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.clickable(
                        onClick = { /* 이벤트 전파 방지 */ },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                ) {
                    // 검색 바
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { newValue ->
                            viewModel.updateSearchQuery(newValue)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .background(
                                color = White,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = Black
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.executeSearch()
                                focusManager.clearFocus()
                            }
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sheet_search),
                                    contentDescription = "검색",
                                    tint = Black,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.text.isEmpty()) {
                                        Text(
                                            text = "제목, 작곡가, 장르로 검색해보세요",
                                            color = Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }

                                // X 버튼 (검색 모드일 때만 표시)
                                if (isSearchMode && searchQuery.text.isNotEmpty()) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_topbar_close),
                                        contentDescription = "검색 취소",
                                        tint = Gray,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable {
                                                viewModel.clearSearch()
                                                focusManager.clearFocus()
                                            }
                                    )
                                }
                            }
                        }
                    )

                    // 자동완성 제안 목록 (검색 모드가 아닐 때 표시)
                    if (!isSearchMode && suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = White
                            )
                        ) {
                            LazyColumn {
                                items(suggestions) { suggestion ->
                                    SearchSuggestionItem(
                                        text = suggestion,
                                        onClick = {
                                            viewModel.selectSuggestion(suggestion)
                                            focusManager.clearFocus()
                                        }
                                    )

                                    if (suggestion != suggestions.last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = Gray.copy(alpha = 0.3f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 검색 전 초기 화면 (검색어 입력 전 & 자동완성 없을 때)
            if (!isSearchMode && suggestions.isEmpty() && searchQuery.text.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 32.dp)
                ) {
                    // 검색 기록
                    if (recentSearches.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "검색 기록",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Black
                            )
                            Text(
                                text = "전체 삭제",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Gray,
                                modifier = Modifier.clickable {
                                    viewModel.clearAllSearchHistory()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentSearches.forEach { keyword ->
                                SearchHistoryChip(
                                    text = keyword,
                                    showCloseButton = true,
                                    onClick = {
                                        viewModel.updateSearchQuery(androidx.compose.ui.text.input.TextFieldValue(keyword))
                                        viewModel.executeSearch()
                                    },
                                    onClose = {
                                        viewModel.removeSearchHistory(keyword)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))
                    } else {
                        Text(
                            text = "검색 기록",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "검색 내역이 없습니다.",
                                fontSize = 14.sp,
                                color = Gray
                            )
                            Spacer(modifier = Modifier.height(36.dp))
                        }
                    }

//                    // 추천 검색어 Todo: 서버에서 가져온 값 사용
//                    if (recommendedKeywords.isNotEmpty()) {
//                        Text(
//                            text = "추천 검색어",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            color = Black
//                        )
//
//                        Spacer(modifier = Modifier.height(12.dp))
//
//                        FlowRow(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.spacedBy(8.dp),
//                            verticalArrangement = Arrangement.spacedBy(12.dp)
//                        ) {
//                            recommendedKeywords.forEach { keyword ->
//                                SearchKeywordChip(
//                                    text = keyword,
//                                    showCloseButton = false,
//                                    onClick = {
//                                        viewModel.updateSearchQuery(
//                                            TextFieldValue(keyword)
//                                        )
//                                        viewModel.executeSearch()
//                                    }
//                                )
//                            }
//                        }
//                    }
//                    else{
//                        Text(
//                            text = "추천 검색어",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            color = Black
//                        )
//
//                        Spacer(modifier = Modifier.height(12.dp))
//
//                        Column(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            Text(
//                                text = "추천 검색이 없습니다.",
//                                fontSize = 14.sp,
//                                color = Gray
//                            )
//                            Spacer(modifier = Modifier.height(36.dp))
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(36.dp))

                    // 인기 검색어
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "인기 검색어",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Black
                        )
                        Text(
                            text = formatTrendingGeneratedAt(trendingGeneratedAt),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 인기 검색어 리스트 + 페이지네이션
                    val popularPages = remember(trendingSearches) {
                        // 4개씩 끊어서 페이지 만들고, 최대 3페이지까지만 사용
                        trendingSearches.chunked(4).take(3)
                    }

                    if (popularPages.isNotEmpty()) {
                        val pagerState = rememberPagerState(
                            pageCount = { popularPages.size }
                        )

                        LaunchedEffect(pagerState, popularPages.size) {
                            if (popularPages.size <= 1) return@LaunchedEffect

                            while (true) {
                                delay(6000L) // 8초
                                val nextPage = (pagerState.currentPage + 1) % popularPages.size
                                pagerState.animateScrollToPage(nextPage)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            pageSize = PageSize.Fill,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val pageItems = popularPages[page]

                                pageItems.forEachIndexed { index, keyword ->
                                    val rankStart = 1 // 5위부터 시작
                                    val rank = rankStart + page * 4 + index

                                    PopularSearchItem(
                                        rank = rank,
                                        keyword = keyword,
                                        onClick = {
                                            viewModel.updateSearchQuery(TextFieldValue(keyword))
                                            viewModel.executeSearch()
                                        }
                                    )

                                    if (index < pageItems.size - 1) {
                                        HorizontalDivider(
                                            color = GrayLine,
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 페이지 인디케이터
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(popularPages.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(
                                            if (index == pagerState.currentPage) Black
                                            else Gray.copy(alpha = 0.3f)
                                        )
                                )
                                if (index < popularPages.size - 1) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                        }
                    }

                }
            }

            // 검색 결과 리스트 (검색 모드일 때만 표시)
            if (isSearchMode && suggestions.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                onClick = { /* 이벤트 전파 방지 */ },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "검색 결과가 없습니다.",
                            color = Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(
                            bottom = 16.dp + bottomImePadding
                        ),
                        modifier = Modifier
                            .clickable(
                                onClick = { /* 이벤트 전파 방지 */ },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                    ) {
                        items(searchResults) { sheet ->
                            SearchResultItem(
                                sheet = sheet,
                                onClick = {
                                    onNavigateToDetail(sheet.sheetId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchKeywordChip(
    text: String,
    showCloseButton: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Gray3)
            .border(0.5.dp, GrayLine, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Black
        )
    }
}

@Composable
fun SearchHistoryChip(
    text: String,
    showCloseButton: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(White)
            .border(0.5.dp, GrayLine, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Black
        )

        if (showCloseButton) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_topbar_close),
                contentDescription = "삭제",
                tint = Gray,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClose() }
            )
        }
    }
}


@Composable
fun PopularSearchItem(
    rank: Int,
    keyword: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 순위
        Text(
            text = "$rank",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Black,
            modifier = Modifier.width(24.dp)
        )

        // 키워드
        Text(
            text = keyword,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Black,
            modifier = Modifier.weight(1f)
        )

        // 검색 아이콘
        Icon(
            painter = painterResource(R.drawable.ic_sheet_search),
            contentDescription = "검색",
            tint = Gray,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SearchSuggestionItem(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sheet_search),
            contentDescription = null,
            tint = DarkHover,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Black
        )
    }
}

@Composable
fun SearchResultItem(
    sheet: ServiceSheet,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 썸네일
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gray3)
            ) {
                // 난이도 아이콘
                Icon(
                    painter = painterResource(
                        matchThumbnail(title = sheet.title)
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified
                )

                // 악기 아이콘
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .size(20.dp)
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(getInstrumentDrawable(sheet.instrumentId)),
                        contentDescription = null,
                        tint = DarkHover,
                        modifier = Modifier.size(14.dp)
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
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

        // 구분선
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Gray3,
            thickness = 1.dp
        )
    }
}

private fun formatTrendingGeneratedAt(raw: String?): String {
    if (raw.isNullOrBlank()) return ""

    return try {
        // 서버에서 오는 ISO 문자열을 Instant로 파싱
        val instant = Instant.parse(raw)

        // 한국 시간(또는 시스템 타임존)으로 변환
        val zoned = instant.atZone(ZoneId.systemDefault())

        // 예: "2025.11.19 09:38 기준"
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm 기준")

        formatter.format(zoned)
    } catch (e: Exception) {
        "" // 파싱 실패하면 그냥 안 보여줌
    }
}
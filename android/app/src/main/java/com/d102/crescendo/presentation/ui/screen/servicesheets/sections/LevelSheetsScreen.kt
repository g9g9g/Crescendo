package com.d102.crescendo.presentation.ui.screen.servicesheets.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.sheet.ServiceSheet
import com.d102.crescendo.presentation.theme.*
import com.d102.crescendo.presentation.ui.component.loading.CustomLoadingIndicator
import com.d102.crescendo.presentation.ui.screen.servicesheets.ServiceSheetSearchViewModel
import com.d102.crescendo.presentation.ui.screen.servicesheets.TierFilter
import com.d102.crescendo.util.getGenreName
import com.d102.crescendo.util.getTierDrawable
import com.d102.crescendo.util.matchThumbnail
import com.d102.crescendo.util.pressClickEffect

@Composable
fun LevelSheetsScreen(
    onSheetClick: (Int) -> Unit = {},
    viewModel: ServiceSheetSearchViewModel = hiltViewModel()
) {
    val filteredSheets by viewModel.filteredLevelSheets.collectAsState()
    val uiState by viewModel.levelUiState.collectAsState()
    val selectedTierFilter by viewModel.selectedTierFilter.collectAsState()

    // 정렬 상태
    val selectedSort by viewModel.selectedSort.collectAsState()
    val showSortDropdown by viewModel.showSortDropdown.collectAsState()

    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        viewModel.loadLevelSheets()
    }

    // 필터 변경 시 스크롤 맨 위로
    LaunchedEffect(selectedTierFilter) {
        gridState.animateScrollToItem(0)
    }

    // 정렬이 변경되면 스크롤을 맨 위로 이동
    LaunchedEffect(selectedSort) {
        gridState.animateScrollToItem(0)
    }

    // 정렬된 악보 리스트
    val sortedSheets = remember(filteredSheets, selectedSort) {
        when (selectedSort) {
            SortType.LATEST -> filteredSheets.sortedByDescending { it.updatedAt }
            SortType.POPULAR -> filteredSheets.sortedByDescending { it.downloadNumber }
            SortType.NAME -> filteredSheets.sortedBy { it.title }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is LevelUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CustomLoadingIndicator()
                }
            }
            is LevelUiState.Success -> {
                Box(modifier = Modifier.fillMaxSize()) {  // Box로 감싸기
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(White)
                    ) {
                        // 티어 필터 탭
                        TierFilterTabs(
                            selectedFilter = selectedTierFilter,
                            onFilterChange = { viewModel.updateTierFilter(it) }
                        )

                        if (sortedSheets.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "해당 난이도의 악보가 없습니다",
                                    color = Gray,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            // 그리드 리스트
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(sortedSheets) { sheet ->
                                    LevelSheetCard(
                                        sheet = sheet,
                                        onClick = { onSheetClick(sheet.sheetId) }
                                    )
                                }
                            }
                        }
                    }

                    // 드롭다운 메뉴 추가
                    if (showSortDropdown) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            DropdownMenu(
                                expanded = showSortDropdown,
                                onDismissRequest = { viewModel.hideSortDropdown() },
                                containerColor = Color.Transparent,
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp,
                                offset = androidx.compose.ui.unit.DpOffset(x = (-8).dp, y = (-16).dp),
                                modifier = Modifier.width(84.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = White,
                                    border = BorderStroke(1.dp, DarkHover)
                                ) {
                                    Column {
                                        SortOption(
                                            text = "최신순",
                                            isFirst = true,
                                            isLast = false,
                                            isSelected = selectedSort == SortType.LATEST,
                                            onClick = {
                                                viewModel.updateSortType(SortType.LATEST)
                                            }
                                        )
                                        SortOption(
                                            text = "인기순",
                                            isFirst = false,
                                            isLast = false,
                                            isSelected = selectedSort == SortType.POPULAR,
                                            onClick = {
                                                viewModel.updateSortType(SortType.POPULAR)
                                            }
                                        )
                                        SortOption(
                                            text = "이름순",
                                            isFirst = false,
                                            isLast = true,
                                            isSelected = selectedSort == SortType.NAME,
                                            onClick = {
                                                viewModel.updateSortType(SortType.NAME)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 외부 클릭 감지 오버레이
                    if (showSortDropdown) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(-1f)
                                .background(Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    viewModel.hideSortDropdown()
                                }
                        )
                    }
                }
            }
            is LevelUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = (uiState as LevelUiState.Error).message ?: "오류가 발생했습니다",
                            color = Gray
                        )
                        TextButton(onClick = { viewModel.loadLevelSheets() }) {
                            Text("다시 시도", color = DarkHover)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TierFilterTabs(
    selectedFilter: TierFilter,
    onFilterChange: (TierFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TierFilterChip(
            text = "전체",
            isSelected = selectedFilter == TierFilter.ALL,
            onClick = { onFilterChange(TierFilter.ALL) },
            modifier = Modifier.weight(1f)
        )
        TierFilterChip(
            text = "브론즈",
            isSelected = selectedFilter == TierFilter.BRONZE,
            onClick = { onFilterChange(TierFilter.BRONZE) },
            modifier = Modifier.weight(1f)
        )
        TierFilterChip(
            text = "실버",
            isSelected = selectedFilter == TierFilter.SILVER,
            onClick = { onFilterChange(TierFilter.SILVER) },
            modifier = Modifier.weight(1f)
        )
        TierFilterChip(
            text = "골드",
            isSelected = selectedFilter == TierFilter.GOLD,
            onClick = { onFilterChange(TierFilter.GOLD) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TierFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) DarkHover else Gray3)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) White else Gray
        )
    }
}

@Composable
fun LevelSheetCard(
    sheet: ServiceSheet,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
    ) {
        // 악보 이미지
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pressClickEffect {
                    onClick()
                }
                .clip(RoundedCornerShape(12.dp))
                .background(Gray3)
        ) {
            Icon(
                painter = painterResource(
                    matchThumbnail(title = sheet.title)
                ),contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = Color.Unspecified
            )

            // 악기 아이콘
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
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
                    tint = DarkHover,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 제목
        Text(
            text = sheet.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkHover,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 작곡가 • 장르
        Text(
            text = "${sheet.composer} • ${getGenreName(sheet.genreId)}",
            fontSize = 12.sp,
            color = Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 다운로드 횟수
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = formatDownloadCount(sheet.downloadNumber),
                fontSize = 12.sp,
                color = Gray,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
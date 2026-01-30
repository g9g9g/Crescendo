package com.d102.crescendo.presentation.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d102.crescendo.domain.model.onboarding.Genre
import com.d102.crescendo.domain.model.onboarding.Instrument
import com.d102.crescendo.domain.model.sheet.RecentPractice
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.presentation.ui.component.home.SelectPracticeModeDialog
import com.d102.crescendo.presentation.ui.component.home.RecentPracticeItem
import com.d102.crescendo.presentation.ui.component.loading.CustomLoadingIndicator
import com.d102.crescendo.util.getInstrumentDrawable
import com.d102.crescendo.util.getTierDrawable
import com.d102.crescendo.util.toTierNameKor

@Composable
fun RecentPracticeListScreen(
    onBackClick: () -> Unit,
    viewModel: HomeViewModel,
    onNavigateToPractice: (Int, String, Int, Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 모달창 상태 관리
    var practiceToStart by remember { mutableStateOf<RecentPractice?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        when (val state = uiState) {
            is HomeUiState.Idle -> {
                CustomLoadingIndicator(
                    title = "최근 연주 기록을 불러오고 있어요",
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is HomeUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is HomeUiState.Error -> {
                Text(
                    text = state.message,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is HomeUiState.Success -> {
                val list = state.recentPracticeList

                if (list.isEmpty()) {
                    // 최근 연주 기록이 없을 때: 정가운데 문구 출력
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "최근 연주 기록이 없습니다.\n좋아하는 곡으로 첫 연주를 남겨보세요!",
                            color = Gray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // 정상 목록
                    RecentPracticeList(
                        practiceList = list,
                        genres = state.genres,
                        instruments = state.instruments,
                        onItemClick = { record ->
                            practiceToStart = record // 모달 띄우기 요청
                        }
                    )
                }
            }
        }

        // 연주 선택 모달
        if (practiceToStart != null) {
            val record = practiceToStart!!

            SelectPracticeModeDialog(
                isPracticeEnabled = record.progressRate == 100,
                onDismiss = { practiceToStart = null },
                onPracticeClick = {
                    onNavigateToPractice(
                        record.userSheetId.toInt(),
                        record.xmlUrl,
                        record.startMeasure,
                        true
                    )
                    practiceToStart = null
                },
                onEvaluationClick = {
                    onNavigateToPractice(
                        record.userSheetId.toInt(),
                        record.xmlUrl,
                        record.startMeasure,
                        false
                    )
                    practiceToStart = null
                }
            )
        }
    }
}

@Composable
private fun RecentPracticeList(
    practiceList: List<RecentPractice>,
    genres: List<Genre>,
    instruments: List<Instrument>,
    onItemClick: (RecentPractice) -> Unit, // (변경)
) {
    // ID -> 이름 변환용 Map 생성
    val genreMap = remember(genres) { genres.associate { it.id to it.korName } }
    val instrumentMap = remember(instruments) { instruments.associate { it.id to it.korName } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 0.dp,
            bottom = 48.dp
        )
    ) {
        itemsIndexed(practiceList) { index, record ->

            RecentPracticeItem(
                thumbnailUrl = record.thumbnailUrl,
                tierDrawableRes = getTierDrawable(record.tierCode, record.tierLevel),
                title = record.title,
                infoText = "${record.composer ?: "N/A"} • ${genreMap[record.genreId] ?: "N/A"}",
                tierText = "난이도: ${record.tierCode.toTierNameKor()} ${record.tierLevel}",
                progressText = "${record.progressRate}% 완료",
                instrumentDrawableRes = getInstrumentDrawable(record.instrumentId),
                lastAccessedAt = record.lastAccessedAt,
                genreId = record.genreId,
                onClick = { onItemClick(record) }
            )

            // 마지막 아이템에는 Divider 제거
            if (index < practiceList.lastIndex) {
                HorizontalDivider(color = GrayLine, thickness = 1.dp)
            }
        }
    }
}
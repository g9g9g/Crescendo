package com.d102.crescendo.presentation.ui.screen.ranking

import TopRankerCard
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d102.crescendo.domain.model.onboarding.Instrument
import com.d102.crescendo.domain.model.rank.Ranker
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.presentation.ui.component.loading.CustomLoadingIndicator
import com.d102.crescendo.presentation.ui.component.ranking.InstrumentDropdown
import com.d102.crescendo.presentation.ui.component.ranking.MyRankFooter
import com.d102.crescendo.presentation.ui.component.ranking.RankerItem
import com.d102.crescendo.presentation.ui.screen.profile.ProfileUiState
import com.d102.crescendo.presentation.ui.screen.profile.ProfileViewModel


private val GoldColor = Color(0xFFFFC107)
private val SilverColor = Color(0xFFC8C8C8)
private val BronzeColor = Color(0xFFB8860B)

@Composable
fun RankingScreen(
    onBackClick: () -> Unit,
    profileViewModel: ProfileViewModel,
    rankingViewModel: RankingViewModel = hiltViewModel()
) {
    val rankingUiState by rankingViewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()

    val selectedInstrumentId by rankingViewModel.selectedInstrumentId.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        when (val state = rankingUiState) {
            is RankingUiState.Loading -> {
                CustomLoadingIndicator(title = "랭킹을 불러오고 있어요", modifier = Modifier.align(Alignment.Center))
            }
            is RankingUiState.Error -> {
                Text(text = state.message, modifier = Modifier.align(Alignment.Center))
            }
            is RankingUiState.Success -> {
                RankingContent(
                    instruments = state.instruments,
                    topRankers = state.topRankers,
                    selectedInstrumentId = selectedInstrumentId ?: -1,
                    onInstrumentSelected = { instrumentId ->
                        rankingViewModel.onInstrumentSelected(instrumentId)
                    }
                )
            }
        }

        if (profileUiState is ProfileUiState.Success && selectedInstrumentId != null) {
            val myProfile = (profileUiState as ProfileUiState.Success).userProfile
            val myRankData = remember(myProfile, selectedInstrumentId) {
                myProfile.instrumentTiers.find { it.instrumentId == selectedInstrumentId }
            }
            MyRankFooter(
                myRankData = myRankData,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun RankingContent(
    instruments: List<Instrument>,
    topRankers: List<Ranker>,
    selectedInstrumentId: Int,
    onInstrumentSelected: (Int) -> Unit
) {
    val top3Rankers = remember(topRankers) { topRankers.filter { it.rank in 1..3 } }
    val otherRankers = remember(topRankers) { topRankers.filter { it.rank > 3 } }

    // 무한 반복 애니메이션 (하나만 사용)
    val infiniteTransition = rememberInfiniteTransition(label = "floating")

    // 모든 카드가 같이 움직임 (0dp ~ -8dp)
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingAnimation"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // TOP 3 카드 (Gray3 배경 추가)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Gray3)
                    .padding(top = 40.dp, bottom = 16.dp)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val rank2 = top3Rankers.find { it.rank == 2 }
                    val rank1 = top3Rankers.find { it.rank == 1 }
                    val rank3 = top3Rankers.find { it.rank == 3 }

                    // 2등 (같은 애니메이션)
                    if (rank2 != null) {
                        TopRankerCard(
                            ranker = rank2,
                            backgroundColor = SilverColor,
                            modifier = Modifier
                                .weight(1f)
                                .offset(y = floatingOffset.dp)
                        )
                    }

                    // 1등 (위로 올림 + 같은 애니메이션)
                    if (rank1 != null) {
                        TopRankerCard(
                            ranker = rank1,
                            backgroundColor = GoldColor,
                            modifier = Modifier
                                .weight(1f)
                                .offset(y = (floatingOffset - 20).dp) // 기본 -20dp + 애니메이션
                        )
                    }

                    // 3등 (같은 애니메이션)
                    if (rank3 != null) {
                        TopRankerCard(
                            ranker = rank3,
                            backgroundColor = BronzeColor,
                            modifier = Modifier
                                .weight(1f)
                                .offset(y = floatingOffset.dp)
                        )
                    }
                }
            }
        }

        // 4등 이하 리스트
        itemsIndexed(otherRankers) { index, ranker ->
            RankerItem(
                ranker = ranker,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            if (index < otherRankers.lastIndex) {
                HorizontalDivider(color = GrayLine, modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
    }
}
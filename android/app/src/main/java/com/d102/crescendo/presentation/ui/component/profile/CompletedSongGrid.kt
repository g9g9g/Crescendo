package com.d102.crescendo.presentation.ui.component.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.profile.Completion
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.Bronze
import com.d102.crescendo.presentation.theme.Gold
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.Silver
import com.d102.crescendo.util.TierBadge

@Composable
fun CompletedSongGrid(
    count: Int,
    completions: List<Completion>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp), // 둥근 모서리
        color = Gray3,
    ) {
        Column(
            modifier = Modifier.padding(16.dp) // 카드 내부 여백
        ) {
            // 카드 제목
            Text(
                text = "완곡한 곡들",
               fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 리스트가 비어있는지 여부를 카드 *내부*에서 확인
            if (completions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 완곡한 곡이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray
                    )
                }
            } else {
                val chunkedCompletions = completions.chunked(10)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    chunkedCompletions.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 1. 실제 아이템 그리기
                            rowItems.forEach { completion ->
                                TierBadge(
                                    tierCode = completion.tierCode,
                                    tierLevel = completion.tierLevel,
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(36.dp) // 크기 지정
                                )
                            }

                            // 2. 빈 공간 채우기 (동일)
                            val emptySpaces = 10 - rowItems.size
                            if (emptySpaces > 0) {
                                repeat(emptySpaces) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
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
 * [수정] 완곡 뱃지
 * (String 대신 Completion 객체를 받음)
 */
@Composable
private fun SongDifficultyBadge(
    completion: Completion,
    modifier: Modifier = Modifier
) {
    // 모델에서 직접 값 추출
    val tierName = completion.tierCode.uppercase()
    val level = completion.tierLevel.toString()

    // 2. 드로어블 리소스 결정 (동일)
    val painterResId = when (level) {
        "1" -> R.drawable.ic_one_closed
        "2" -> R.drawable.ic_two_closed
        "3" -> R.drawable.ic_three_closed
        else -> R.drawable.ic_one_closed
    }

    // 3. 색상 결정 (동일)
    val tint = when (tierName) {
        "GOLD" -> Gold
        "SILVER" -> Silver
        "BRONZE" -> Bronze
        else -> Gray
    }

    // 4. UI 구성 (동일)
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .widthIn(min = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = painterResId),
            contentDescription = "$tierName $level",
            modifier = Modifier.size(32.dp),
            tint = tint
        )
    }
}
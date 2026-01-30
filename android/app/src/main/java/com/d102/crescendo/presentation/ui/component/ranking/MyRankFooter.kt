package com.d102.crescendo.presentation.ui.component.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d102.crescendo.domain.model.profile.InstrumentTier
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.util.toTierNameKor

/**
 * 랭킹 화면 하단에 고정되는 '내 랭크'
 * (ProfileViewModel의 데이터 사용)
 */
@Composable
fun MyRankFooter(
    myRankData: InstrumentTier?,
    modifier: Modifier = Modifier
) {
    if (myRankData == null) {
        return
    }

    val rankText = myRankData.rank.let { "#$it" }
    val tierText = "${myRankData.tierCode.toTierNameKor()} ${myRankData.tierLevel}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Gray3)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 왼쪽: 내 랭킹
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "내 랭킹",
                style = Typography.bodyMedium
            )
            Text(
                text = rankText,
                style = Typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        // 오른쪽: 티어 + 총점
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = tierText,
                style = Typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "${myRankData.exp}P",  // 총점 추가
                style = Typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
package com.d102.crescendo.presentation.ui.component.ranking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.rank.Ranker
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.util.toTierNameKor

/**
 * 랭킹 4등 이하 리스트 아이템
 */
@Composable
fun RankerItem(
    ranker: Ranker,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 등수
        Text(
            text = ranker.rank.toString(),
            style = Typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp) // 정렬을 위한 너비 고정
        )

        // 프로필 이미지
        AsyncImage(
            model = ranker.profileUrl,
            contentDescription = ranker.nickname,
            placeholder = painterResource(id = R.drawable.ic_profile_holder),
            error = painterResource(id = R.drawable.ic_profile_holder),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 닉네임, 티어
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ranker.nickname,
                style = Typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${ranker.tierCode.toTierNameKor()} ${ranker.tierLevel}",
                style = Typography.bodySmall,
                color = Gray
            )
        }

        // 포인트
        Text(
            text = "${ranker.exp}P",
            style = Typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
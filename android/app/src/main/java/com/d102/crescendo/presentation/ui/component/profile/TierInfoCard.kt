package com.d102.crescendo.presentation.ui.component.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.profile.InstrumentTier
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.Normal
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.util.TierBadge

private val BronzeColor = Color(0xFFB8860B)
private val SilverColor = Color(0xFFC8C8C8)
private val GoldColor = Color(0xFFFFC107)

@Composable
fun TierInfoCard(
    tierName: String,
    rank: String,
    pointsToNextTier: String,
    progress: Float,
    progressText: String,
) {
    val tierColor = when {
        tierName.contains("브론즈", ignoreCase = true) -> BronzeColor
        tierName.contains("실버", ignoreCase = true) -> SilverColor
        tierName.contains("골드", ignoreCase = true) -> GoldColor
        else -> DarkHover // 기본 색상
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Gray3
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
            // --- 상단 행 (아이콘, 티어/랭크, 다음티어) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 숫자(레벨)만 추출
                val tierLevel = tierName.filter { it.isDigit() }.toIntOrNull() ?: 0
                val tierCode = tierName.filter { it.isLetter() }
                TierBadge(
                    tierCode = tierCode,
                    tierLevel = tierLevel,
                    modifier = Modifier
                        .size(44.dp) // 크기 지정
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 티어, 랭크
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tierName,
                        style = Typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rank,
                        style = Typography.bodySmall,
                        color = Gray
                    )
                }

                // 다음 티어까지 (포인트 강조)
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Gray, fontWeight = FontWeight.Normal)) {
                            append("다음 티어까지 ")
                        }
                        withStyle(style = SpanStyle(color = Normal, fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
                            append(pointsToNextTier.replace("다음 티어까지 ", ""))
                        }
                    },
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 하단 행 (프로그레스 바, 포인트) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 프로그레스 바
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Normal,
                    trackColor = GrayLine,
                    strokeCap = StrokeCap.Round,
                    gapSize = (-4).dp,
                    drawStopIndicator = {}
                )

                // "1 / 3 Point" 텍스트
                Text(
                    text = progressText,
                    style = Typography.bodySmall,
                    color = Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
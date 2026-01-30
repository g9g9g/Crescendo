package com.d102.crescendo.presentation.ui.component.home

import android.R.attr.text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.profile.UserProfile
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray2
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.Light_Gray
import com.d102.crescendo.presentation.theme.Normal
import com.d102.crescendo.presentation.theme.White

@Composable
fun PracticeTimeSection(
    userProfile: UserProfile,
    instrumentMap: Map<Int, String>
) {
    // 총 연습 시간 (초 단위)
    val totalPracticeTime = userProfile.totalPracticeTime
    val totalMinutes = (totalPracticeTime / 60).toInt()
    val totalHours = totalMinutes / 60
    val totalMins = totalMinutes % 60

    android.util.Log.d("PracticeTime", "=== 연습 시간 데이터 ===")
    android.util.Log.d("PracticeTime", "총 연습 시간(초): $totalPracticeTime")
    android.util.Log.d("PracticeTime", "총 연습 시간(분): $totalMinutes")
    android.util.Log.d("PracticeTime", "총 연습 시간(시간): ${totalHours}시간 ${totalMins}분")

    // 악기별 ID 찾기
    val pianoId = instrumentMap.entries.find { it.value == "피아노" }?.key
    val guitarId = instrumentMap.entries.find { it.value == "기타" }?.key

    // instrumentTiers에서 악기별 연습 시간 추출
    val instrumentTiersMap = remember(userProfile.instrumentTiers) {
        userProfile.instrumentTiers.associateBy { it.instrumentId }
    }

    val pianoPracticeTime = instrumentTiersMap[pianoId]?.practiceTime ?: 0L
    val guitarPracticeTime = instrumentTiersMap[guitarId]?.practiceTime ?: 0L

    android.util.Log.d("PracticeTime", "피아노 연습 시간(초): $pianoPracticeTime")
    android.util.Log.d("PracticeTime", "기타 연습 시간(초): $guitarPracticeTime")

    // 각 악기를 분으로 변환
    val pianoMinutes = (pianoPracticeTime / 60).toInt()
    val guitarMinutes = (guitarPracticeTime / 60).toInt()

    android.util.Log.d("PracticeTime", "피아노 연습 시간(분): $pianoMinutes")
    android.util.Log.d("PracticeTime", "기타 연습 시간(분): $guitarMinutes")

    // UI에 표시되는 분 단위로 합산 (1분 + 2분 = 3분)
    val displayedTotalMinutes = pianoMinutes + guitarMinutes
    val displayedTotalHours = displayedTotalMinutes / 60
    val displayedTotalMins = displayedTotalMinutes % 60

    android.util.Log.d("PracticeTime", "표시용 총 시간: ${displayedTotalHours}시간 ${displayedTotalMins}분")
    android.util.Log.d("PracticeTime", "======================\n")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 총 연습 시간 카드
        TotalTimeCard(
            hours = displayedTotalHours,
            minutes = displayedTotalMins,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 악기별 연습 시간 카드들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InstrumentTimeCard(
                title = "피아노",
                minutes = pianoMinutes,
                color = Color(0xFF6CA5E8),
                modifier = Modifier.weight(1f)
            )

            InstrumentTimeCard(
                title = "기타",
                minutes = guitarMinutes,
                color = Color(0xFFFF6B6B),
                modifier = Modifier.weight(1f)
            )
        }

        // 연습 비율 진행바 (둘 다 연습 시간이 있을 때만 표시)
        if (pianoMinutes > 0 || guitarMinutes > 0) {
            Spacer(modifier = Modifier.height(24.dp))

            PracticeProgress(
                pianoMinutes = pianoMinutes,
                guitarMinutes = guitarMinutes,
                totalMinutes = displayedTotalMinutes
            )
        }
    }
}

@Composable
private fun TotalTimeCard(
    hours: Int,
    minutes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Gray3
        ),
//        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    text = "총 연습시간",
                    fontSize = 16.sp,
                    color = Black,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (hours > 0) "$hours" else "$minutes",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Normal
                    )
                    Text(
                        text = if (hours > 0) "시간 ${minutes}분" else "분",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Black,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )
                }
            }

            // 장식 아이콘
            Icon(
                painter = painterResource(R.drawable.ic_time),
                contentDescription = "시간",
                tint = DarkHover,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
            )
        }
    }
}

@Composable
private fun InstrumentTimeCard(
    title: String,
    minutes: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val hours = minutes / 60
    val mins = minutes % 60

    val lightColor = Color(
        red = color.red * 0.4f + 1f * 0.6f,
        green = color.green * 0.4f + 1f * 0.6f,
        blue = color.blue * 0.4f + 1f * 0.6f,
        alpha = 1f
    )

    Card(
        modifier = modifier.height(200.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Gray3
        ),
//        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 아이콘 + 제목
            Column {
                Icon(
                    painter = if (title == "피아노") painterResource(R.drawable.ic_piano)
                    else painterResource(R.drawable.ic_guitar),
                    contentDescription = "아이콘",
                    tint = DarkHover,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // 여기 간격만 조절하면 됨

            // 하단: 시간 표시
            Column(
                verticalArrangement = Arrangement.Center // ⭐ 세로 중앙 정렬
            ) {
                if (hours > 0) {
                    // 시간과 분이 모두 있을 때
                    Text(
                        text = "${hours}시간",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Normal,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = "${mins}분",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Black
                    )
                } else {
                    // 분만 있을 때 - 세로 중앙 정렬
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$minutes",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Normal,
                            lineHeight = 32.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "분",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun PracticeProgress(
    pianoMinutes: Int,
    guitarMinutes: Int,
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val pianoPercentage = if (totalMinutes > 0) pianoMinutes.toFloat() / totalMinutes else 0f
    val guitarPercentage = if (totalMinutes > 0) guitarMinutes.toFloat() / totalMinutes else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "연습 비율",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkHover,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        // 진행바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Gray3)
        ) {
            // 피아노 비율
            if (pianoPercentage > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(pianoPercentage)
                        .background(Color(0xFF6CA5E8))
                )
            }

            // 기타 비율
            if (guitarPercentage > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(guitarPercentage)
                        .background(Color(0xFFFF6B6B))
                )
            }
        }

        // 범례
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LegendItem(
                color = Color(0xFF6CA5E8),
                label = "피아노",
                percentage = (pianoPercentage * 100).toInt()
            )

            LegendItem(
                color = Color(0xFFFF6B6B),
                label = "기타",
                percentage = (guitarPercentage * 100).toInt()
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    percentage: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = "$label ($percentage%)",
            fontSize = 14.sp,
            color = Gray
        )
    }
}
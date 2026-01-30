package com.d102.crescendo.presentation.ui.component.home

import android.R.attr.scaleX
import android.R.attr.scaleY
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.sheet.RecentPractice
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.LightActive
import com.d102.crescendo.presentation.theme.Normal
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.util.getGenreName
import com.d102.crescendo.util.getInstrumentDrawable
import com.d102.crescendo.util.getTierDrawable
import com.d102.crescendo.util.matchThumbnail
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 3단계: "최근 연주 기록" (큰 카드)
 */
@Composable
fun RecentPracticeCard(
    thumbnailUrl: String?,
    tierDrawableRes: Int,
    progressText: String,
    title: String,
    infoText: String,
    tierText: String,
    instrumentDrawableRes: Int,
    progress: Float,
    lastAccessedAt: String,
    onContinueClick: () -> Unit,
    genreId: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Gray3
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        border = BorderStroke(1.dp, Gray3)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // --- 상단: 악보 정보 ---
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. 썸네일
                Box(
                    modifier = Modifier.size(100.dp) // 썸네일 크기
                ) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize() // Box에 맞춤
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Gray3, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(
                            matchThumbnail(title = title)
                        ),
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd) // 썸네일 우측 하단
                            .padding(6.dp)
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(28.dp)
                            )
                            .size(28.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Gray3),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(instrumentDrawableRes),
                            contentDescription = "악기 아이콘",
                            tint = DarkHover,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 2. 텍스트(제목/작곡가/완료태그) + 버튼을 하나의 Row로 묶기
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 텍스트 영역
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Text(
                            text = title,
                            color = Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = infoText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // --- 완료 태그 (작곡가 텍스트 아래로 이동) ---
                        Text(
                            text = progressText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = White,
                            modifier = Modifier
                                .background(Normal, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp)
                        )
                    }

                    // --- 연주 이어하기 버튼 (텍스트 세 줄 높이의 중앙에 오게 정렬됨) ---
                    ContinuePlayButton(
                        onContinueClick = onContinueClick
                    )

                }
            }

            // --- 하단: 최근 연주 (오른쪽 정렬) ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
            ) {
                val parsed = LocalDateTime.parse(lastAccessedAt.replace(" ", "T")) // 필요 시 T 변환
                val formatted = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                Text(
                    text = "최근 연주: $formatted",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray,
                    lineHeight = 10.sp
                )
            }
        }
    }
}

@Composable
fun ContinuePlayButton(
    onContinueClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 눌렀을 때 살짝 줄어드는 스케일
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = ""
    )

    IconButton(
        onClick = { onContinueClick() },
        modifier = Modifier
            .size(40.dp) // 전체 버튼 사이즈 약간 키워줌
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = 12f      // 튀어나온 느낌
                shape = CircleShape
                clip = false
            }
            .background(DarkHover, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = White
        ),
        interactionSource = interactionSource // <- 이거 중요!
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_music_replay),
            contentDescription = "연주 이어하기",
            modifier = Modifier.size(28.dp)
        )
    }
}
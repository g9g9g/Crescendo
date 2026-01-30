package com.d102.crescendo.presentation.ui.component.home

import android.R.attr.scaleX
import android.R.attr.scaleY
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.d102.crescendo.R
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.Normal
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.util.matchThumbnail

/**
 * "최근 연주 기록" 화면의 리스트 아이템
 * (가공된 String, Int 값만 받음)
 */
@Composable
fun RecentPracticeItem(
    thumbnailUrl: String?,
    tierDrawableRes: Int,
    title: String,
    infoText: String,
    tierText: String,
    progressText: String,
    instrumentDrawableRes: Int,
    lastAccessedAt: String,
    onClick: () -> Unit,
    genreId: Int,
) {
    // 🔹 버튼 눌림 애니메이션 상태
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "recentPracticePlayScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 썸네일 ---
            Box(
                modifier = Modifier.size(84.dp)
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Gray3, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(
                        matchThumbnail(title = title)
                    )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .shadow(2.dp, RoundedCornerShape(22.dp))
                        .size(22.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Gray3),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(instrumentDrawableRes),
                        contentDescription = "악기 아이콘",
                        tint = DarkHover,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- 텍스트 영역 ---
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = Typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = infoText,
                    style = Typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = progressText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = White,
                    modifier = Modifier
                        .background(Normal, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- 연주 버튼 (눌림 애니메이션 적용) ---
            IconButton(
                onClick = { onClick() },
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        shadowElevation = 12f
                        shape = CircleShape
                        clip = false
                    }
                    .background(DarkHover, CircleShape),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = White
                ),
                interactionSource = interactionSource
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_music_replay),
                    contentDescription = "연주 이어하기",
                    tint = White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // --- 아래쪽: 최근 연주 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "최근 연주: $lastAccessedAt",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gray,
                lineHeight = 10.sp
            )
        }
    }
}

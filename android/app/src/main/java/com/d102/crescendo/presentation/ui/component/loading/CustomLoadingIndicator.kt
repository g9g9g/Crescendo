package com.d102.crescendo.presentation.ui.component.loading

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.White

@Composable
fun CustomLoadingIndicator(
    modifier: Modifier = Modifier,
    title: String = "악보를 불러오고 있어요",
    textColor: androidx.compose.ui.graphics.Color = DarkHover,
    iconColor: androidx.compose.ui.graphics.Color = DarkHover
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 음표 아이콘
        Icon(
            painter = painterResource(com.d102.crescendo.R.drawable.ic_music),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(48.dp)
        )

        // 문구 (여러 줄 지원)
        Text(
            text = title,
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        // 통통 튀는 3점 애니메이션
        BouncingDotsRow(dotColor = iconColor)
    }
}

@Composable
private fun BouncingDotsRow(
    dotColor: androidx.compose.ui.graphics.Color = DarkHover
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        BouncingDot(delayMillis = 0, dotColor = dotColor)
        BouncingDot(delayMillis = 120, dotColor = dotColor)
        BouncingDot(delayMillis = 240, dotColor = dotColor)
    }
}

@Composable
private fun BouncingDot(
    delayMillis: Int,
    dotSize: Int = 8,           // dp
    travelY: Float = 8f,        // px 단위로 translationY 적용(음수로 위로 이동)
    durationMillis: Int = 600,
    dotColor: androidx.compose.ui.graphics.Color = DarkHover
) {
    // 위로 통통 튀는 offset 애니메이션
    val infinite = rememberInfiniteTransition(label = "dots")
    val ty by infinite.animateFloat(
        initialValue = 0f,               // 바닥
        targetValue = -travelY,          // 위로 travelY 만큼
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis, StartOffsetType.Delay)
        ),
        label = "dot-translateY"
    )

    // 약간의 깜빡임(존재감) 추가—선택
    val alpha by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis, StartOffsetType.Delay)
        ),
        label = "dot-alpha"
    )

    Box(
        modifier = Modifier
            .size(dotSize.dp)
            .graphicsLayer {
                translationY = ty     // px 단위 이동
                this.alpha = alpha
            }
    ) {
        // 점은 간단히 ▪ 모양으로. 동그라미가 더 좋다면 CircleShape + background로 대체 가능
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { } // no-op
        ) {
            // 동그란 점
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = dotColor)
            }
        }
    }
}
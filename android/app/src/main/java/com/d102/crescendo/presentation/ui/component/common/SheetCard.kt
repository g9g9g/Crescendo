package com.d102.crescendo.presentation.ui.component.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.d102.crescendo.presentation.theme.*
import com.d102.crescendo.util.matchThumbnail
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun SheetCard(
    thumbnailUrl: String?,
    tierDrawableRes: Int,
    title: String,
    infoText: String,
    tierText: String,
    instrumentDrawableRes: Int,
    downloadNumber: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    genreId: Int,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "sheetCardScale"
    )

    Column(
        modifier = modifier
            .width(180.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        // 1. 악보 썸네일
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(12.dp))
                .background(Black)
                .border(
                    width = 1.dp,
                    color = Gray3,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                error = painterResource(
                    matchThumbnail(title = title)
                ),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Gray3),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(instrumentDrawableRes),
                    contentDescription = "악기 아이콘",
                    tint = DarkHover,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. 텍스트 정보
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = infoText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

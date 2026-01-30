package com.d102.crescendo.presentation.ui.component.profile.edit

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.d102.crescendo.R
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.White

/**
 * 3단계: 프로필 이미지 + 카메라 아이콘 편집기
 */
@Composable
fun ProfileImageEditor(
    imageUri: Any?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp) // 디자인에 맞게 크기 조절
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.BottomEnd //카메라 아이콘을 우측 하단에 배치
    ) {
        AsyncImage(
            model = imageUri,
            // (중요) imageUri가 null일 때 기본 프로필 홀더 표시
            fallback = painterResource(id = R.drawable.ic_profile_holder),
            error = painterResource(id = R.drawable.ic_profile_holder),
            placeholder = painterResource(id = R.drawable.ic_profile_holder),
            contentDescription = "프로필 이미지",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Icon(
            painter = painterResource(id = R.drawable.outline_photo_camera_24),
            contentDescription = "이미지 변경",
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(White)
                .border(1.dp, GrayLine, CircleShape)
                .padding(4.dp)
        )
    }
}
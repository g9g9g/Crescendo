package com.d102.crescendo.presentation.ui.component.common

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d102.crescendo.R
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Typography

/**
 * "섹션 제목 >" 형태의 공용 헤더 컴포넌트
 */
@SuppressLint("RememberInComposition")
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onArrowClick: (() -> Unit)? // null이면 화살표 숨김
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp), // 좌우 여백
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // "섹션 제목" 텍스트
        Text(
            text = title,
            fontSize = 20.sp,
            color = DarkHover,
            fontWeight = FontWeight.Bold
        )

        // ">" 화살표 아이콘
        if (onArrowClick != null) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_keyboard_arrow_right_24),
                contentDescription = "$title 더보기",
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = MutableInteractionSource(),
                indication = null
            ) {
                onArrowClick()
            },
                tint = Black
            )
        }
    }
}
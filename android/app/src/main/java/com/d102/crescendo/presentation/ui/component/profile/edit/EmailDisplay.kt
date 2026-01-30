package com.d102.crescendo.presentation.ui.component.profile.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.Typography

/**
 * 4단계: 수정 불가능한 이메일 표시
 */
@Composable
fun EmailDisplay(email: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = email,
            style = Typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Gray,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = GrayLine, thickness = 1.dp)
    }
}
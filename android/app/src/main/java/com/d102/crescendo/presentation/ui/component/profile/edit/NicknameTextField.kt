package com.d102.crescendo.presentation.ui.component.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.Dark
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.Normal
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.presentation.theme.White


/**
 * [수정] 4단계: 닉네임 입력 필드 (중복확인 버튼 제거)
 * @param nickname ViewModel의 현재 닉네임
 * @param onNicknameChanged 텍스트 변경 시 ViewModel에 알림
 */
@Composable
fun NicknameTextField(
    nickname: String,
    onNicknameChanged: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = nickname,
            onValueChange = { onNicknameChanged(it) },
            textStyle = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            singleLine = true,
            cursorBrush = SolidColor(Black)
        ) { innerTextField ->
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp,)
                ) {
                    innerTextField() // BasicTextField의 입력 영역
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = GrayLine, thickness = 1.dp)
            }
        }
    }
}
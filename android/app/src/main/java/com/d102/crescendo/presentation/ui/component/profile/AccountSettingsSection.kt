package com.d102.crescendo.presentation.ui.component.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Typography

/**
 * 6단계: 계정 설정 섹션 (헤더 + 메뉴 아이템)
 */
@Composable
fun AccountSettingsSection(
    onLogoutClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // "계정설정" 헤더
        Text(
            text = "계정설정",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DarkHover,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // "로그아웃" 메뉴
        SettingsMenuItem(
            text = "로그아웃",
            onClick = onLogoutClick
        )
        // "회원탈퇴" 메뉴
        SettingsMenuItem(
            text = "회원탈퇴",
            onClick = onWithdrawClick
        )
    }
}

/**
 * "로그아웃" > 형태의 재사용 가능한 메뉴 아이템
 */
@Composable
private fun SettingsMenuItem(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // 행 전체 클릭 가능
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // "로그아웃", "회원탈퇴" 텍스트
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Gray
        )

        Icon(
            painter = painterResource(id = R.drawable.baseline_keyboard_arrow_right_24),
            contentDescription = "$text 이동",
            modifier = Modifier.size(24.dp),
            tint = Gray // 화살표 아이콘 색상
        )
    }
}
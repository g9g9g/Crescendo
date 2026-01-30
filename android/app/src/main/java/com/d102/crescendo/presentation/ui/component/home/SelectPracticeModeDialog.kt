package com.d102.crescendo.presentation.ui.component.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.d102.crescendo.R
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.presentation.ui.screen.mysheets.PlayModeOption

@Composable
fun SelectPracticeModeDialog(
    isPracticeEnabled: Boolean,
    onDismiss: () -> Unit,
    onPracticeClick: () -> Unit,
    onEvaluationClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = White,
            border = BorderStroke(1.dp, Gray3),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 연습모드
                PlayModeOption(
                    icon = R.drawable.ic_practice_mode,
                    title = "연습모드",
                    description = "정확한 음을 맞추면 악보가 자동으로 넘어갑니다.",
                    onClick = onPracticeClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 평가모드
                PlayModeOption(
                    icon = R.drawable.ic_test_mode,
                    title = "평가모드",
                    description = "정확도와 리듬을 분석해 연주 실력을 확인하세요.",
                    enabled = isPracticeEnabled, // 여기!
                    overlayText = if (!isPracticeEnabled) "진도율 100% 달성해야 평가모드를 사용할 수 있어요" else null, // 여기!
                    onClick = onEvaluationClick
                )
            }
        }
    }
}
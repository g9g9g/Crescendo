package com.d102.crescendo.presentation.ui.component.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.Normal
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.presentation.theme.White

@Composable
fun WithdrawConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    color = Gray3,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "회원탈퇴",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkHover
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "정말 회원을 탈퇴하시겠습니까?",
                        style = Typography.bodyLarge,
                        color = Gray,
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(color = GrayLine, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 확인 버튼
                    DialogButton(
                        text = "확인",
                        textColor = Normal,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )

                    VerticalDivider(
                        modifier = Modifier.height(52.dp),
                        thickness = 1.dp,
                        color = GrayLine
                    )

                    // 취소 버튼
                    DialogButton(
                        text = "취소",
                        textColor = Gray,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        isBold = false
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    textColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBold: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(bottomStart = if (text == "확인") 16.dp else 0.dp, bottomEnd = if (text == "취소") 16.dp else 0.dp))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            )
            .then(
                if (isPressed) Modifier.background(DarkHover)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = Typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isPressed) White else textColor
        )
    }
}
package com.d102.crescendo.presentation.ui.screen.onboarding

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.White

@SuppressLint("RememberInComposition")
@Composable
fun SelectableItem(
    name: String,
    isSelected: Boolean,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) DarkHover else GrayLine

    val itemHeight = if (iconRes != null) 200.dp else 120.dp
    Card(
        modifier = Modifier
            .size(width = 140.dp, height = itemHeight)
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = White),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 아이콘 있을 때만 표시
                iconRes?.let {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = null,
                        tint = if (isSelected) DarkHover else Gray,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Text(
                    text = name,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) DarkHover else Gray,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.padding(top = if (iconRes != null) 8.dp else 0.dp)
                )
            }
        }
    }
}

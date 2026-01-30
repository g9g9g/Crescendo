package com.d102.crescendo.presentation.ui.component.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.d102.crescendo.domain.model.onboarding.OnboardingRecommendSheet
import com.d102.crescendo.presentation.theme.*
import com.d102.crescendo.util.getTierDrawable
import com.d102.crescendo.util.matchThumbnail

@Composable
fun SelectableSheetCard(
    sheet: OnboardingRecommendSheet,
    genreName: String,
    isSelected: Boolean,
    instrumentIdForIcon: Int,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) DarkHover else GrayLine
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 썸네일
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray3)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                if(sheet.thumbnailUrl.isNullOrEmpty()){
                    Icon(
                        painter = painterResource(matchThumbnail(title = sheet.title)),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.Unspecified
                    )
                } else{
                    Icon(
                        painter = painterResource(getTierDrawable(sheet.tierCode, sheet.tierLevel)),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.Unspecified
                    )
                }
            }

            // 텍스트 영역에만 패딩 8dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) DarkHover.copy(alpha = 0.10f)
                        else Color.Transparent
                    )
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = sheet.title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) DarkHover else Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                val subText = buildString {
                    append(sheet.composer)
                    if (genreName.isNotBlank()) append(" · $genreName")
                }

                Text(
                    text = subText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isSelected) DarkHover else Gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

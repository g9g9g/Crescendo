package com.d102.crescendo.util

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.d102.crescendo.R

fun String.toTierNameKor(): String {
    return when (this.uppercase()) {
        "BRONZE" -> "브론즈"
        "SILVER" -> "실버"
        "GOLD" -> "골드"
        else -> this // 모르는 코드는 그대로 반환
    }
}

fun difficultyToTierCode(name: String): String? = when (name) {
    "브론즈" -> "bronze"
    "실버"   -> "silver"
    "골드"   -> "gold"
    else -> null
}

//@Composable
//fun TierBadge(
//    tierCode: String?,
//    tierLevel: Int?,
//    modifier: Modifier = Modifier,
//    onClick: (() -> Unit)? = null
//) {
//    // 티어 정보 없으면 아예 표시 안 함
//    if (tierCode == "" || tierLevel == 0) return
//
//    val BronzeColor = Color(0xFFB8860B)
//    val SilverColor = Color(0xFFC8C8C8)
//    val GoldColor   = Color(0xFFFFC107)
//
//    if (tierCode == null || tierLevel == null) return
//
//    val bgColor = when (tierCode.uppercase()) {
//        "BRONZE" -> BronzeColor
//        "SILVER" -> SilverColor
//        "GOLD" -> GoldColor
//        else -> Gray3
//    }
//
//    val text = when (tierCode.uppercase()) {
//        "BRONZE" -> "B$tierLevel"
//        "SILVER" -> "S$tierLevel"
//        "GOLD" -> "G$tierLevel"
//        else -> "B3"
//    }
//
//    Box(
//        modifier = modifier
//            .shadow(2.dp, RoundedCornerShape(20.dp))
//            .size(36.dp)
//            .clip(RoundedCornerShape(20.dp))
//            .background(bgColor)
//            .let {
//                if (onClick != null) it.pressClickEffect { onClick() } else it
//            },
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            text = text,
//            fontSize = 14.sp,
//            fontWeight = FontWeight.Bold,
//            color = White
//        )
//    }
//}

@Composable
fun TierBadge(
    tierCode: String?,
    tierLevel: Int?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // 티어 정보 없으면 아예 표시 안 함
    if (tierCode == "" || tierLevel == 0) return
    if (tierCode == null || tierLevel == null) return

    // 티어별 아이콘 리소스 결정
    val iconRes = when (tierCode.uppercase()) {
        "BRONZE" -> when (tierLevel) {
            1 -> R.drawable.ic_bronze1
            2 -> R.drawable.ic_bronze2
            3 -> R.drawable.ic_bronze3
            else -> R.drawable.ic_bronze3
        }
        "SILVER" -> when (tierLevel) {
            1 -> R.drawable.ic_silver1
            2 -> R.drawable.ic_silver2
            3 -> R.drawable.ic_silver3
            else -> R.drawable.ic_silver3
        }
        "GOLD" -> when (tierLevel) {
            1 -> R.drawable.ic_gold1
            2 -> R.drawable.ic_gold2
            3 -> R.drawable.ic_gold3
            else -> R.drawable.ic_gold3
        }
        else -> R.drawable.ic_bronze3
    }

    // 배경 없이 아이콘만
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = "$tierCode $tierLevel",
        tint = Color.Unspecified,
        modifier = modifier
            .size(44 .dp)
            .let {
                if (onClick != null) it.pressClickEffect { onClick() } else it
            }
    )
}
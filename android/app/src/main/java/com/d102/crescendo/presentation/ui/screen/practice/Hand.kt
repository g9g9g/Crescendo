package com.d102.crescendo.presentation.ui.screen.practice

import androidx.annotation.DrawableRes
import com.d102.crescendo.R

enum class Hand(
    @DrawableRes val iconRes: Int,
    val displayName: String
) {
    BOTH(R.drawable.ic_both_hands, "양손"),
    LEFT(R.drawable.ic_left_hand, "왼손"),
    RIGHT(R.drawable.ic_right_hand, "오른손")
}
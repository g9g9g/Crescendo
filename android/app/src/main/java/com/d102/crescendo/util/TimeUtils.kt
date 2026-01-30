package com.d102.crescendo.util

import java.util.Locale

object TimeUtils {
    fun makeSecToHourMinute(totalSeconds: Long): String {
        if (totalSeconds < 60) return "0분"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d시간 %d분", hours, minutes)
        } else {
            String.format(Locale.getDefault(), "%d분", minutes)
        }
    }
}

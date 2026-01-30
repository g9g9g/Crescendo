package com.d102.crescendo.domain.model.sheet

/**
 * 오늘의 추천 악보 도메인 모델
 */
data class TodayRecommendSheet(
    val sheetId: Int,
    val title: String,
    val composer: String,
    val genreId: Int,
    val tierCode: String?,
    val tierLevel: Int?,
    val downloadNumber: Int,
    val thumbnailUrl: String?,
    val instrumentId: Int
)
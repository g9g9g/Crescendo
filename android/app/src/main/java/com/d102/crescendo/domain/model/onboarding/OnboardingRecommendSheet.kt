package com.d102.crescendo.domain.model.onboarding

/**
 * [GET /api/user/onboarding/recommend-sheets]
 * 온보딩 추천 악보 UI가 사용할 '깨끗한' 도메인 모델
 */
data class OnboardingRecommendSheet(
    val sheetId: Long,
    val tierCode: String,
    val tierLevel: Int,
    val thumbnailUrl: String?,
    val title: String,
    val composer: String?,
    val genreId: Long
)
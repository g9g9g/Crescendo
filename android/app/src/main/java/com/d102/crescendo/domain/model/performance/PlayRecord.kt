package com.d102.crescendo.domain.model.performance

// API 2.2 요청용 도메인 모델
data class PlayRecord(
    val userSheetId: Int,
    val practiceMode: Boolean,
    val startMeasure: Int,
    val endMeasure: Int,
    val startedAt: String,
    val wavXmlUrl: String?  // practiceMode가 false이면 채워서 보낼 것임.
)

package com.d102.crescendo.domain.model.sheet

data class Performance(
    val performanceId: Int,
    val totalScore: Int,
    val comment: String,
    val endedAt: String,
    val wavXmlUrl: String
)
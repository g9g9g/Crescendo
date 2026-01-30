package com.d102.crescendo.domain.model.performance

/**
 * AI 평가 항목 (템포 안정성 등) 도메인 모델
 */
data class Metric(
    val code: String,
    val name: String,
    val score: Int
)
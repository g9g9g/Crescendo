package com.d102.crescendo.domain.model.performance

/**
 * [GET /api/plays/{playId}] (AI 연주 평가 결과) 도메인 모델
 */
data class PlayResult(
    val status: String, // "COMPLETED", "EVALUATING", "NOT_AVAILABLE"
    val totalScore: Int?,
    val grade: String?,
    val comment: String?,
    val metrics: List<Metric>? // Metric 모델 리스트
)
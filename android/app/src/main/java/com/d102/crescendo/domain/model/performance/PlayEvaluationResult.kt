package com.d102.crescendo.domain.model.performance

// GET /api/plays/{playId}의 도메인 모델
data class PlayEvaluationResult(
    val practiceMode: Boolean,
    val totalScore: Int?,
    val comment: String?
)

package com.d102.crescendo.domain.model.profile

data class UserProfile(
    val userId: Long,
    val nickname: String,
    val email: String,
    val favoriteGenreIds: List<Int>,
    val totalPracticeTime: Long,
    val instrumentTiers: List<InstrumentTier>,
    val profileUrl: String?,
    val completedCount: Int,
    val completions: List<Completion>,
)
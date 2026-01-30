package com.d102.crescendo.domain.model.sheet

data class RecentPractice(
    // --- 고유 필드 ---
    val userSheetId: Long,
    val progressRate: Int,
    val lastAccessedAt: String,
    val xmlUrl: String,
    val startMeasure: Int,

    // --- BaseSheet 구현 ---
    override val title: String,
    override val composer: String?,
    override val genreId: Int,
    override val tierCode: String,
    override val tierLevel: Int,
    override val thumbnailUrl: String?,
    override val instrumentId: Int
) : BaseSheet
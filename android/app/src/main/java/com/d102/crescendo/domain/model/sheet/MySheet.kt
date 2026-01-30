package com.d102.crescendo.domain.model.sheet

/**
 * 내 악보 항목
 */
data class MySheet(
    val userSheetId: Int,
    override val title: String,
    override val composer: String?,
    override val thumbnailUrl: String?,
    override val instrumentId: Int,
    override val genreId: Int,
    override val tierCode: String?,
    override val tierLevel: Int?,
    val progressRate: Int
) : BaseSheet
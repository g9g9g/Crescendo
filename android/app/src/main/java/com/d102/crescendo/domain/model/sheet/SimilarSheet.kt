package com.d102.crescendo.domain.model.sheet

data class SimilarSheet(
    val sheetId: Int,
    val thumbnailUrl: String?,
    val title: String,
    val composer: String?,
    val genreId: Int,
    val tierCode: String,
    val tierLevel: Int,
    val instrumentId: Int?
)
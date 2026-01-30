package com.d102.crescendo.domain.model.sheet

data class MySheetDetail(
    val userSheetId: Int,
    val title: String,
    val composer: String?,
    val genreId: Int,
    val tierCode: String?,
    val tierLevel: Int?,
    val instrumentId: Int,
    val thumbnailUrl: String?,
    val xmlUrl: String,
    val startMeasure: Int,
    val performances: List<Performance>,
    val progress: Int,
)
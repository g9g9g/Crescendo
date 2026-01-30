package com.d102.crescendo.domain.model.sheet

/**
 * 서비스 악보 단건 도메인 모델
 */
data class ServiceSheetDetail(
    override val title: String,
    override val composer: String,
    override val genreId: Int,
    override val instrumentId: Int,
    override val tierCode: String?,
    override val tierLevel: Int?,
    val downloadNumber: Int,
    override val thumbnailUrl: String?,
    val xmlUrlPreview: String?,
    val xmlUrl: String?,
    val metrics: Metrics?,
    val summary: String?,
    val recommendations: List<String>,
    val similarSheets: List<SimilarSheet> = emptyList()
): BaseSheet

data class Metrics(
    val tempo: Double,
    val rhythm: Double,
    val intervals: Double,
    val harmony: Double,
    val technique: Double,
    val length: Double
)
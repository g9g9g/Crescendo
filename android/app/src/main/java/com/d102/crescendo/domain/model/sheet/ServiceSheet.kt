package com.d102.crescendo.domain.model.sheet

/**
 * 앱 내부에서 사용할 모델
 * updatedAt은 우선 String 그대로 두고, 필요하면 LocalDateTime으로 변환 필드 추가하세요.
 */
data class ServiceSheet(
    val sheetId: Int,
    override val title: String,
    override val composer: String,
    override val thumbnailUrl: String?,
    override val genreId: Int,
    override val tierCode: String?,
    override val tierLevel: Int?,
    override val instrumentId: Int,
    val downloadNumber: Int,
    val updatedAt: String
) : BaseSheet
package com.d102.crescendo.domain.model.sheet

/**
 * 공통 UI 필드만 정의
 */
interface BaseSheet {
    val title: String
    val composer: String?
    val genreId: Int
    val tierCode: String?
    val tierLevel: Int?
    val thumbnailUrl: String?
    val instrumentId: Int
}
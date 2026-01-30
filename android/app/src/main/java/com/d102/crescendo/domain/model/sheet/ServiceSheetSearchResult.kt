package com.d102.crescendo.domain.model.sheet

/**
 * 서비스 악보 검색 결과
 */
data class ServiceSheetSearchResult(
    val totalCount: Int,
    val sheetList: List<ServiceSheet>
)
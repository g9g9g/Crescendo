package com.d102.crescendo.domain.model.sheet

/**
 * 내 악보 검색 결과
 */
data class MySheetSearchResult(
    val totalCount: Int,
    val sheetList: List<MySheet>
)
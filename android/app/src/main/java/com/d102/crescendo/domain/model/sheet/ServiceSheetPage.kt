package com.d102.crescendo.domain.model.sheet

/** 페이징 응답 래퍼 */
data class ServiceSheetPage(
    val totalCount: Int,
    val sheets: List<ServiceSheet>
)
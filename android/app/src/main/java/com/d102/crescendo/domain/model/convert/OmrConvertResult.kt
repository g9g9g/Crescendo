package com.d102.crescendo.domain.model.convert

data class OmrConvertResult(
    val message: String,
    val musicXml: String,
    val pagesProcessed: Int,
    val s3Url: String,
    val success: Boolean
)
package com.d102.crescendo.domain.model.rank

/**
 * 랭킹 UI가 사용할 랭커 정보 도메인 모델
 */
data class Ranker(
    val rank: Int,
    val nickname: String,
    val profileUrl: String?,
    val tierCode: String,
    val tierLevel: Int,
    val exp: Long
)
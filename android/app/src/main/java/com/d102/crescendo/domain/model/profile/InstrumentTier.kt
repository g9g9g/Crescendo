package com.d102.crescendo.domain.model.profile

data class InstrumentTier(
    val instrumentId: Int,
    val tierCode: String,
    val tierLevel: Int,
    val exp: Int,
    val expToNext: Int,
    val practiceTime: Long,
    val rank: Int
)
package com.d102.crescendo.domain.repository

import com.d102.crescendo.domain.model.onboarding.Genre
import com.d102.crescendo.domain.model.onboarding.Instrument

interface CommonRepository {

    /**
     * [GET /api/common/genres]
     * 전체 장르 목록을 조회
     */
    suspend fun getGenres(): Result<List<Genre>>

    /**
     * [GET /api/common/instruments]
     * 전체 악기 목록을 조회
     */
    suspend fun getInstruments(): Result<List<Instrument>>
}
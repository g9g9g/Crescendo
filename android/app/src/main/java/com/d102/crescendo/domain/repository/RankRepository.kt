package com.d102.crescendo.domain.repository

import com.d102.crescendo.domain.model.rank.Ranker

/**
 * 04. Rank (랭킹) 관련 Repository 인터페이스
 */
interface RankRepository {

    /**
     * [GET /api/rank]
     * 악기별 일일 랭킹을 조회
     */
    suspend fun getRankings(instrumentId: Int): Result<List<Ranker>>
}
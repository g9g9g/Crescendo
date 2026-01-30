package com.d102.crescendo.domain.repository

import com.d102.crescendo.domain.model.performance.EndPlayResult
import com.d102.crescendo.domain.model.performance.PlayRecord
import com.d102.crescendo.domain.model.performance.PlayResult
import com.d102.crescendo.domain.model.sheet.RecentPractice

/**
 * 03. Performance (연주) 관련 Repository 인터페이스
 */
interface PerformanceRepository {

    /**
     * [GET /api/plays]
     * 최근 연주 기록을 조회
     */
    suspend fun getRecentPractice(): Result<List<RecentPractice>>


    // 연주 기록 저장
    // [Post]
    suspend fun savePlayRecord(record: PlayRecord): Result<EndPlayResult>

    /**
     * AI 연주 평가 결과 조회
     * [GET /api/plays/{playId}]
     */
    suspend fun getPlayResult(playId: Int): Result<PlayResult>
}
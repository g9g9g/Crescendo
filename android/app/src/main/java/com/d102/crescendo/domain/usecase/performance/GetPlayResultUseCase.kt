package com.d102.crescendo.domain.usecase.performance

import com.d102.crescendo.domain.model.performance.PlayResult
import com.d102.crescendo.domain.repository.PerformanceRepository
import javax.inject.Inject

/**
 * [GET /api/plays/{playId}]
 * AI 연주 평가 결과를 조회하는 UseCase
 */
class GetPlayResultUseCase @Inject constructor(
    private val performanceRepository: PerformanceRepository
) {
    suspend operator fun invoke(playId: Int): Result<PlayResult> {
        return performanceRepository.getPlayResult(playId)
    }
}
package com.d102.crescendo.domain.usecase.performance

import com.d102.crescendo.domain.model.sheet.RecentPractice
import com.d102.crescendo.domain.repository.PerformanceRepository
import javax.inject.Inject

/**
 * "최근 연주 기록"을 가져오는 UseCase
 */
class GetRecentPracticeUseCase @Inject constructor(
    private val performanceRepository: PerformanceRepository
) {
    suspend operator fun invoke(): Result<List<RecentPractice>> {
        return performanceRepository.getRecentPractice()
    }
}
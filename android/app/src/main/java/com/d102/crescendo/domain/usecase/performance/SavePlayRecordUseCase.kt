package com.d102.crescendo.domain.usecase.performance

import com.d102.crescendo.domain.model.performance.EndPlayResult
import com.d102.crescendo.domain.model.performance.PlayRecord
import com.d102.crescendo.domain.repository.PerformanceRepository
import com.d102.crescendo.util.PracticeUpdateNotifier
import javax.inject.Inject

class SavePlayRecordUseCase @Inject constructor(
    private val performanceRepository: PerformanceRepository,
    private val notifier: PracticeUpdateNotifier
) {
    suspend operator fun invoke(record: PlayRecord): Result<EndPlayResult> {
        // 1. API 호출
        val result = performanceRepository.savePlayRecord(record)

        // 2. API 호출 성공 시
        if (result.isSuccess) {
            notifier.notifyPracticeSaved()
        }

        return result
    }
}
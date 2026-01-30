package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.TodayRecommendSheet
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class GetTodayRecommendSheetsUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
) {
    /**
     * 오늘의 악보 추천 리스트를 가져온다.
     * - 성공: Result.success(List<TodayRecommendSheet>)
     * - 실패: Result.failure(Throwable)
     */
    suspend operator fun invoke(): Result<List<TodayRecommendSheet>> {
        return sheetRepository.getTodayRecommendSheets()
    }
}
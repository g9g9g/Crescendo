package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.RecommendSheet
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class GetRecommendedSheetsByTierUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
){
    suspend operator fun invoke(): Result<List<RecommendSheet>> {
        return sheetRepository.getRecommendedSheetsByTier()
    }
}

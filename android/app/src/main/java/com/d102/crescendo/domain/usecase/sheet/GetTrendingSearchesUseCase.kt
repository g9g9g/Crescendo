package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.TrendingSearch
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class GetTrendingSearchesUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
) {
    suspend operator fun invoke(): Result<TrendingSearch> {
        return sheetRepository.getTrendingSearches()
    }
}
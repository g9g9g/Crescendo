package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.PopularSheet
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

/**
 * "인기 악보" 목록을 가져오는 UseCase
 */
class GetPopularSheetsUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
) {
    suspend operator fun invoke(): Result<List<PopularSheet>> {
        return sheetRepository.getPopularSheets()
    }
}
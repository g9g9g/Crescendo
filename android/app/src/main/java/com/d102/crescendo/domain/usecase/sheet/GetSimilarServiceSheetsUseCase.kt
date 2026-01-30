package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.SimilarSheet
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class GetSimilarServiceSheetsUseCase @Inject constructor(
    private val repository: SheetRepository
) {
    suspend operator fun invoke(sheetId: Int): Result<List<SimilarSheet>> {
        return repository.getSimilarServiceSheets(sheetId)
    }
}
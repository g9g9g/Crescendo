package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.ServiceSheetPage
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class SearchServiceSheetsUseCase @Inject constructor(
    private val repository: SheetRepository
) {
    suspend operator fun invoke(
        q: String? = null,
        genreId: Int? = null,
        instrumentId: Int? = null,
        tierCode: String? = null,
        page: Int = 1,
        size: Int = 1000
    ): Result<ServiceSheetPage> =
        repository.searchServiceSheets(q, genreId, instrumentId, tierCode, page, size)
}
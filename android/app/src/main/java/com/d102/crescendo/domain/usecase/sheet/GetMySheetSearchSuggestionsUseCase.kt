package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class GetMySheetSearchSuggestionsUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
) {
    suspend operator fun invoke(query: String) =
        sheetRepository.getMySheetSearchSuggestions(query)
}
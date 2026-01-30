package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class GetSearchSuggestionsUseCase @Inject constructor(
    private val repository: SheetRepository
) {
    suspend operator fun invoke(query: String): Result<List<String>> =
        repository.getSearchSuggestions(query)
}
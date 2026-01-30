package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.MySheetSearchResult
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

/**
 * 내 악보 검색 UseCase
 * 검색어와 필터 조건을 조합하여 내 악보를 조회
 */
class SearchMySheetsUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
) {
    /**
     * @param query 자유 검색어 (제목, 작곡가, 장르명, 악기명)
     * @param genreId 장르 ID 필터
     * @param tierCode 난이도 필터 (bronze, silver, gold)
     * @param instrumentId 악기 ID 필터
     * @param sourceType 출처 타입 필터 (system, user, arranged)
     */
    suspend operator fun invoke(
        query: String? = null,
        genreId: Int? = null,
        tierCode: String? = null,
        instrumentId: Int? = null,
        sourceType: String? = null
    ): Result<MySheetSearchResult> {
        return sheetRepository.searchMySheets(
            query = query,
            genreId = genreId,
            tierCode = tierCode,
            instrumentId = instrumentId,
            sourceType = sourceType
        )
    }
}
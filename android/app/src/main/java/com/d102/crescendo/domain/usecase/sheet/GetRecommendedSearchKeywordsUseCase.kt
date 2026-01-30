package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.RecommendationKeywords
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

/**
 * 검색창에 노출할 추천 검색어 5개를 가져오는 UseCase
 *
 * [GET /api/sheets/service/recommendations]
 */
class GetRecommendedSearchKeywordsUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
) {

    /**
     * @return Result<RecommendationKeywords>
     *  - 성공: RecommendationKeywords(keywords = List<String>)
     *  - 실패: 예외 포함된 Result.failure(...)
     */
//    suspend operator fun invoke(): Result<RecommendationKeywords> {
//        return sheetRepository.getRecommendedSearchKeywords()
//    }
}
package com.d102.crescendo.domain.repository

import com.d102.crescendo.domain.model.sheet.MySheetDetail
import com.d102.crescendo.domain.model.sheet.MySheetSearchResult
import com.d102.crescendo.domain.model.sheet.PopularSheet
import com.d102.crescendo.domain.model.sheet.RecommendSheet
import com.d102.crescendo.domain.model.sheet.RecommendationKeywords
import com.d102.crescendo.domain.model.sheet.ServiceSheetDetail
import com.d102.crescendo.domain.model.sheet.ServiceSheetPage
import com.d102.crescendo.domain.model.sheet.SheetRegistration
import com.d102.crescendo.domain.model.sheet.SimilarSheet
import com.d102.crescendo.domain.model.sheet.TodayRecommendSheet
import com.d102.crescendo.domain.model.sheet.TrendingSearch

/**
 * Sheet (악보) 관련 Repository 인터페이스
 */
interface SheetRepository {

    /**
     * [GET /api/sheets/service/popular]
     * 인기 악보 목록을 조회
     */
    suspend fun getPopularSheets(): Result<List<PopularSheet>>

    /**
     * [GET /api/sheets/user]
     * 내 악보 검색
     */
    suspend fun searchMySheets(
        query: String? = null,
        genreId: Int? = null,
        tierCode: String? = null,
        instrumentId: Int? = null,
        sourceType: String? = null
    ): Result<MySheetSearchResult>

    suspend fun getMySheetDetail(userSheetId: Int): Result<MySheetDetail>

    suspend fun registerUserSheet(registration: SheetRegistration): Result<Unit>

    suspend fun searchServiceSheets(
        q: String? = null,
        genreId: Int? = null,
        instrumentId: Int? = null,
        tierCode: String? = null,
        page: Int = 1,
        size: Int = 1000
    ): Result<ServiceSheetPage>

    suspend fun getServiceSheetDetail(sheetId: Int): Result<ServiceSheetDetail>
    suspend fun getSimilarServiceSheets(sheetId: Int): Result<List<SimilarSheet>>
    suspend fun downloadServiceSheet(sheetId: Int): Result<Unit>

    suspend fun getRecommendedSheetsByTier(): Result<List<RecommendSheet>>

    suspend fun getSearchSuggestions(query: String): Result<List<String>>

    suspend fun arrangeUserSheet(
        userSheetId: Int,
        style: String,
        xmlUrl: String
    ): Result<Unit>

    suspend fun deleteUserSheet(userSheetId: Int): Result<Unit>

    /** 검색창 위에 노출할 추천 검색어 5개 조회 */
//    suspend fun getRecommendedSearchKeywords(): Result<RecommendationKeywords>

    suspend fun getTrendingSearches(): Result<TrendingSearch>

    suspend fun getTodayRecommendSheets(): Result<List<TodayRecommendSheet>>

    suspend fun getMySheetSearchSuggestions(query: String): Result<List<String>>

}
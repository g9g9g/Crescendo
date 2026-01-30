package com.d102.crescendo.domain.repository

import com.d102.crescendo.domain.model.onboarding.OnboardingRecommendSheet
import com.d102.crescendo.domain.model.profile.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 정보(프로필, 회원가입 등) 관련 Repository 인터페이스
 */
interface UserRepository {

    /**
     * [POST /api/user/sign-up]
     * 최초 로그인 시 선호 장르와 악기를 서버에 전송.
     * @param genreIds 선택한 장르 ID 리스트
     * @param instrumentId 선택한 악기 ID
     * @return 성공/실패 여부를 Result<Unit>으로 반환
     */
    suspend fun signUp(genreIds: List<Int>, instrumentId: Int, sheetIds: List<Int>): Result<Unit>


    /**
     * [GET /api/user/onboarding/recommend-sheets]
     * 온보딩 추천 악보 목록을 조회합니다.
     */
    suspend fun getOnboardingRecommendSheets(
        instrumentId: Int
    ): Result<List<OnboardingRecommendSheet>>

    /**
     * [GET /api/user/profile]
     * 현재 로그인한 사용자의 프로필을 조회합니다.
     */
    suspend fun getProfile(): Result<UserProfile>

    /**
     * [PATCH /api/user/profile]
     * 닉네임, 선호 장르, 프로필 이미지를 수정합니다.
     */
    suspend fun updateProfile(
        nickname: String?,
        genreIds: List<Int>?,
        profileUrl: String?
    ): Result<Unit>

    suspend fun deleteAccount(): Result<Unit>
}
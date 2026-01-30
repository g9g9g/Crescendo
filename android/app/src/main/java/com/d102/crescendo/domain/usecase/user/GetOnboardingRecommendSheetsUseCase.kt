package com.d102.crescendo.domain.usecase.user

import com.d102.crescendo.domain.model.onboarding.OnboardingRecommendSheet
import com.d102.crescendo.domain.repository.UserRepository
import javax.inject.Inject

/**
 * [GET /api/user/onboarding/recommend-sheets]
 * 온보딩 추천 악보를 가져오는 UseCase
 */
class GetOnboardingRecommendSheetsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(instrumentId: Int): Result<List<OnboardingRecommendSheet>> {
        return userRepository.getOnboardingRecommendSheets(instrumentId)
    }
}
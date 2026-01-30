package com.d102.crescendo.domain.usecase.user

import com.d102.crescendo.domain.model.profile.UserProfile
import com.d102.crescendo.domain.repository.UserRepository
import javax.inject.Inject

/**
 * [GET /api/user/profile]
 * 현재 로그인된 사용자의 프로필 정보를 가져오는 UseCase
 */
class GetProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * UseCase를 함수처럼 호출
     * @return Repository가 반환한 Result<UserProfile> (성공 또는 실패)
     */
    suspend operator fun invoke(): Result<UserProfile> {
        return userRepository.getProfile()
    }
}
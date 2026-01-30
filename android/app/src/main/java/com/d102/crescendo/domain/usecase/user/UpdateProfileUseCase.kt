package com.d102.crescendo.domain.usecase.user

import com.d102.crescendo.domain.repository.UserRepository
import com.d102.crescendo.util.ProfileUpdateNotifier
import javax.inject.Inject

/**
 * [PATCH /api/user/profile]
 * 수정된 프로필 정보(닉네임, 장르, 이미지URL)를 서버에 최종 제출합니다.
 */
class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val notifier: ProfileUpdateNotifier
) {
    /**
     * @param nickname 새 닉네임 (변경 없으면 null)
     * @param genreIds 새 장르 ID 리스트 (변경 없으면 null)
     * @param profileUrl 새 프로필 이미지 URL (변경 없으면 null)
     * @return 200 - OK 등 http response
     */
    suspend operator fun invoke(
        nickname: String?,
        genreIds: List<Int>?,
        profileUrl: String?
    ): Result<Unit> {
        val result = userRepository.updateProfile(nickname, genreIds, profileUrl)

        // API 호출이 성공했을 때만
        if (result.isSuccess) {
            notifier.notifyProfileUpdated() // 추가 "프로필 변경됨" 신호 전송
        }
        return result
    }
}
package com.d102.crescendo.domain.usecase.user

import com.d102.crescendo.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 회원 탈퇴를 수행하는 UseCase
 */
class DeleteAccountUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * ViewModel에서 호출 시, Repository의 회원 탈퇴 로직을 실행
     * @return Repository가 반환한 Result<Unit> (성공/실패)
     */
    suspend operator fun invoke(): Result<Unit> {
        return userRepository.deleteAccount()
    }
}
package com.d102.crescendo.domain.usecase.auth

import com.d102.crescendo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 로그아웃을 수행하는 UseCase
 * (AuthRepositoryImpl이 로컬 토큰 조회 및 서버 로그아웃, 로컬 토큰 삭제)
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * @return AuthRepository가 반환한 Result<Unit>
     */
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.logout()
    }
}
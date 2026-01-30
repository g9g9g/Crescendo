package com.d102.crescendo.domain.usecase.auth

import com.d102.crescendo.domain.model.AuthToken
import com.d102.crescendo.domain.repository.AuthRepository
import javax.inject.Inject

class ReissueTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * @return AuthRepository가 반환한 Result<AuthToken>
     */
    suspend operator fun invoke(): Result<AuthToken> {
        return authRepository.reissueToken()
    }
}
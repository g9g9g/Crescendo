package com.d102.crescendo.domain.usecase.auth

import com.d102.crescendo.domain.model.AuthToken
import com.d102.crescendo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 구글 로그인을 수행하는 UseCase
 * 비즈니스 로직은 AuthRepositoryImpl에 위임되어 있으므로,
 * 이 UseCase는 단순히 해당 함수를 호출
 */
class GoogleLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * @param idToken Google 로그인 SDK에서 받은 ID 토큰
     * @return AuthRepository가 반환한 Result<AuthToken>
     */
    suspend operator fun invoke(idToken: String): Result<AuthToken> {
        return authRepository.googleLogin(idToken)
    }
}
package com.d102.crescendo.domain.usecase.fcm

import com.d102.crescendo.domain.repository.FcmRepository
import javax.inject.Inject

/**
 * FCM 토큰을 서버에 갱신하는 UseCase
 */
class UpdateFcmTokenUseCase @Inject constructor(
    private val fcmRepository: FcmRepository
) {
    suspend operator fun invoke(token: String): Result<Unit> {
        return fcmRepository.updateFcmToken(token)
    }
}
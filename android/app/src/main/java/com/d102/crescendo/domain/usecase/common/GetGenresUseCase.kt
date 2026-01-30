package com.d102.crescendo.domain.usecase.common

import com.d102.crescendo.domain.model.onboarding.Genre
import com.d102.crescendo.domain.repository.CommonRepository
import javax.inject.Inject

/**
 * 장르 목록을 가져오는 UseCase
 */
class GetGenresUseCase @Inject constructor(
    private val commonRepository: CommonRepository
) {
    /**
     * ViewModel에서 호출 시, 캐시된 장르 목록을 반환
     */
    suspend operator fun invoke(): Result<List<Genre>> {
        return commonRepository.getGenres()
    }
}
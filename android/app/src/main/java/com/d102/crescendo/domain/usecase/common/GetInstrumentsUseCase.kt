package com.d102.crescendo.domain.usecase.common

import com.d102.crescendo.domain.model.onboarding.Instrument
import com.d102.crescendo.domain.repository.CommonRepository
import javax.inject.Inject

/**
 * 악기 목록을 가져오는 UseCase (캐시 로직은 Repository에 위임)
 */
class GetInstrumentsUseCase @Inject constructor(
    private val commonRepository: CommonRepository
) {
    /**
     * ViewModel에서 호출 시, 캐시된 악기 목록을 반환
     */
    suspend operator fun invoke(): Result<List<Instrument>> {
        return commonRepository.getInstruments()
    }
}
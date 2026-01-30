package com.d102.crescendo.domain.usecase.rank

import com.d102.crescendo.domain.model.rank.Ranker
import com.d102.crescendo.domain.repository.RankRepository
import javax.inject.Inject

/**
 * "랭킹" 목록을 가져오는 UseCase
 */
class GetRankingsUseCase @Inject constructor(
    private val rankRepository: RankRepository
) {
    suspend operator fun invoke(instrumentId: Int): Result<List<Ranker>> {
        return rankRepository.getRankings(instrumentId)
    }
}
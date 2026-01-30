package com.d102.crescendo.domain.usecase.user

import com.d102.crescendo.domain.repository.TokenRepository
import com.d102.crescendo.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 온보딩(최초 로그인) 시, 사용자 정보를 서버에 전송하는 UseCase
 */
class SignUpUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository
) {
    /**
     * UseCase를 함수처럼 호출할 수 있게 해줍니다. (ViewModel에서 사용)
     * @param genreIds 선택한 장르 ID 리스트
     * @param instrumentId 선택한 악기 ID
     * @return Repository에서 반환된 Result<Unit> (성공/실패)
     */
    suspend operator fun invoke(genreIds: List<Int>, instrumentId: Int, sheetIds: List<Int>): Result<Unit> {
        val apiResult = userRepository.signUp(genreIds, instrumentId, sheetIds)

        if (apiResult.isSuccess) {
            // [Local] API 호출이 성공하면, DataStore의 플래그를 false로 업데이트
            // (AuthToken 객체를 새로 만들 필요 없이, 플래그만 업데이트하는 함수가 필요함)
            tokenRepository.updateFirstLoginStatus(false)
        }

        return apiResult // 3. API 결과(성공/실패)를 ViewModel로 반환
    }
}
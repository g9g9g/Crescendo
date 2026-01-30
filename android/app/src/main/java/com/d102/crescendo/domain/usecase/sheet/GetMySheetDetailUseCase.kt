package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.MySheetDetail
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

/**
 * 내 악보 상세 조회 UseCase
 * 사용자가 소유한 악보의 상세 정보와 연주 기록을 조회
 */
class GetMySheetDetailUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
) {
    /**
     * @param userSheetId 조회할 내 악보 ID
     */
    suspend operator fun invoke(userSheetId: Int): Result<MySheetDetail> {
        return sheetRepository.getMySheetDetail(userSheetId = userSheetId)
    }
}
package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class DeleteUserSheetUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
) {
    suspend operator fun invoke(userSheetId: Int): Result<Unit> {
        return sheetRepository.deleteUserSheet(userSheetId)
    }
}
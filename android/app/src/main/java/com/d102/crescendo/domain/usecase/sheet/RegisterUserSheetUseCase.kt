package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.SheetRegistration
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class RegisterUserSheetUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
) {
    suspend operator fun invoke(registration: SheetRegistration): Result<Unit> {
        return sheetRepository.registerUserSheet(registration)
    }
}
package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.model.sheet.ServiceSheetDetail
import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class GetServiceSheetDetailUseCase @Inject constructor(
    private val repository: SheetRepository
) {
    suspend operator fun invoke(sheetId: Int): Result<ServiceSheetDetail> {
        return repository.getServiceSheetDetail(sheetId)
    }
}
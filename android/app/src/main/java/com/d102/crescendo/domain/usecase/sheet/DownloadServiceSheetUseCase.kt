package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class DownloadServiceSheetUseCase @Inject constructor(
    private val repository: SheetRepository
) {
    suspend operator fun invoke(sheetId: Int): Result<Unit> =
        repository.downloadServiceSheet(sheetId)
}
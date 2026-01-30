package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.repository.SheetRepository
import javax.inject.Inject

class ArrangeUserSheetUseCase @Inject constructor(
    private val sheetRepository: SheetRepository
) {
    suspend operator fun invoke(
        userSheetId: Int,
        style: String,
        xmlUrl: String
    ) = sheetRepository.arrangeUserSheet(
        userSheetId = userSheetId,
        style = style,
        xmlUrl = xmlUrl
    )
}
package com.d102.crescendo.domain.usecase.convert

import com.d102.crescendo.data.remote.dto.convert.ConvertSheetResponse
import com.d102.crescendo.domain.model.convert.OmrConvertResult
import com.d102.crescendo.domain.repository.ConvertRepository
import okhttp3.MultipartBody
import javax.inject.Inject

class ConvertSheetUseCase @Inject constructor(
    private val convertRepository: ConvertRepository
) {
    suspend operator fun invoke(file: MultipartBody.Part): OmrConvertResult {
        return convertRepository.convertSheet(file).getOrThrow()
    }
}
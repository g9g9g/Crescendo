package com.d102.crescendo.domain.repository

import com.d102.crescendo.domain.model.convert.OmrConvertResult
import okhttp3.MultipartBody

interface ConvertRepository {
    suspend fun convertSheet(part: MultipartBody.Part): Result<OmrConvertResult>
}
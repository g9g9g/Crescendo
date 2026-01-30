package com.d102.crescendo.domain.usecase.sheet

import com.d102.crescendo.domain.repository.S3Repository
import javax.inject.Inject

class UploadStringToS3UseCase @Inject constructor(
    private val repo: S3Repository
) {
    suspend operator fun invoke(
        content: String,
        fileName: String,
        mimeType: String,
        uploadPath: String
    ): Result<String> = repo.uploadStringAsFile(content, fileName, mimeType, uploadPath)
}
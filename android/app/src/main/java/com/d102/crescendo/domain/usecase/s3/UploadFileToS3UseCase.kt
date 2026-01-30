package com.d102.crescendo.domain.usecase.s3

import android.net.Uri
import com.d102.crescendo.domain.repository.S3Repository
import javax.inject.Inject

class UploadFileToS3UseCase @Inject constructor(
    private val s3Repository: S3Repository
) {
    suspend operator fun invoke(uri: Uri, uploadPath: String): Result<String> {
        return s3Repository.uploadFile(uri, uploadPath)
    }
}
package com.d102.crescendo.domain.repository

import android.net.Uri

/**
 * S3 파일 업로드(Presigned URL 방식)를 처리하는 Repository
 * UseCase는 이 인터페이스만 바라봄.
 */
interface S3Repository {

    /**
     * Uri를 받아 S3에 업로드하고, 최종 fileUrl을 반환합니다.
     * 모든 복잡한 로직(Presigned URL 요청, 스트리밍 업로드 등)은 Impl에 캡슐화됩니다.
     *
     * @param uri 갤러리 또는 파일 선택기에서 받은 Content Uri
     * @param uploadPath S3에 저장할 경로 (예: "profiles" 또는 "sheets")
     * @return 성공 시 S3에 저장된 최종 fileUrl (String), 실패 시 에러를 포함한 Result
     */
    suspend fun uploadFile(
        uri: Uri,
        uploadPath: String
    ): Result<String>

    suspend fun uploadStringAsFile(
        content: String,
        fileName: String,
        mimeType: String,
        uploadPath: String
    ): Result<String>
}
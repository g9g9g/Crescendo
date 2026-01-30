package com.d102.crescendo.domain.usecase.s3

import android.net.Uri
import com.d102.crescendo.domain.repository.S3Repository
import javax.inject.Inject

/**
 * 파일을 S3에 업로드하는 비즈니스 로직을 실행합니다.
 *
 * ViewModel은 이 유스케이스를 주입받아 사용하며,
 * 복잡한 리포지토리의 구현(Presigned URL, 스트리밍 등)을 알 필요가 없습니다.
 */
class UploadFileUseCase @Inject constructor(
    private val s3Repository: S3Repository // (중요) Impl이 아닌 Interface를 주입받음
) {
    /**
     * 'invoke' 연산자를 오버로딩하여 유스케이스 클래스 자체를 함수처럼 호출할 수 있게 합니다.
     *
     * @param uri 갤러리 등에서 선택한 파일의 Content Uri
     * @param uploadPath S3에 저장할 경로 (예: "profiles" 또는 "sheets")
     * @return 성공 시 S3에 저장된 최종 fileUrl을 담은 [Result]
     */
    suspend operator fun invoke(uri: Uri, uploadPath: String): Result<String> {
        // 모든 복잡한 로직은 S3RepositoryImpl에 위임되어 있습니다.
        // 유스케이스는 단순히 해당 기능을 호출합니다.
        return s3Repository.uploadFile(uri, uploadPath)
    }
}
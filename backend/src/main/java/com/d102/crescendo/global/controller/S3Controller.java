package com.d102.crescendo.global.controller;

import com.d102.crescendo.global.dto.request.PresignedUrlRequest;
import com.d102.crescendo.global.dto.response.PresignedUrlResponse;
import com.d102.crescendo.global.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/presigned-url")
    @Operation(summary = "S3 업로드용 presigned URL 발급")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @RequestBody PresignedUrlRequest request) {
        // 1️⃣ 업로드할 S3 경로 구성
        String path = request.getUploadPath() != null && !request.getUploadPath().isEmpty()
                ? request.getUploadPath() + "/" + request.getFileName()
                : request.getFileName();

        // 2️⃣ Presigned URL 생성
        var presignedUrl = s3Service.generatePresignedUrl(
                path,
                request.getFileType() != null ? request.getFileType() : "application/octet-stream",
                300 // 5분
        );

        // 3️⃣ 업로드 완료 후 접근 가능한 fileUrl 계산
        String fileUrl = s3Service.buildFileUrl(path);

        return ResponseEntity.ok(PresignedUrlResponse.builder()
                .presignedUrl(presignedUrl.toString())
                .fileUrl(fileUrl)
                .expiresIn(300)
                .build());
    }
}
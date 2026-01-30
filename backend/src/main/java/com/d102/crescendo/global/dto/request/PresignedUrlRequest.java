package com.d102.crescendo.global.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PresignedUrlRequest {
    private String fileName;   // 필수
    private String fileType;   // 선택 (MIME 타입)
    private String uploadPath; // 선택 (S3 폴더 경로)
}

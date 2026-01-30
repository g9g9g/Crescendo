package com.d102.crescendo.global.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PresignedUrlResponse {
    private String presignedUrl; // PUT용
    private String fileUrl;      // 업로드 완료 후 접근용
    private long expiresIn;      // 만료 시간(초)
}
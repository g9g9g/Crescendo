package com.d102.crescendo.domain.sheet.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MusicXmlParseResponse {
    private String fullUrl;
    private String previewUrl;        // 미리보기 XML S3 URL
    private Short maxMeasureCount;  // 총 마디 수
    private Short partCount;        // 파트(악기) 수
    private String title;             // 곡 제목
    private String composer;          // 작곡가
}

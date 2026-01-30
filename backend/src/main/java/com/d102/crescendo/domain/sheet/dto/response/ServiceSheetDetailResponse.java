package com.d102.crescendo.domain.sheet.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@Schema(description = "스토어 악보 상세 조회 응답")
public class ServiceSheetDetailResponse {

    private String title;

    private String composer;

    private Integer genreId;

    private Integer instrumentId;

    private String tierCode;

    private Short tierLevel;

    private Integer downloadNumber;

    private String thumbnailUrl;

    private String xmlUrlPreview;

    private String xmlUrl;

    private DifficultyMetrics metrics;

    private String summary;

    private List<String> recommendations;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DifficultyMetrics {
        private Double tempo;
        private Double rhythm;
        private Double intervals;
        private Double harmony;
        private Double technique;
        private Double length;
    }
}
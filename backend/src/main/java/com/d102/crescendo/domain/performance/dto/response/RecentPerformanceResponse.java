package com.d102.crescendo.domain.performance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentPerformanceResponse {
    private Integer totalCount;
    private List<PerformanceRecord> records;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceRecord {
        private Integer userSheetId;
        private String title;
        private String composer;
        private Integer genreId;
        private String tierCode;
        private Short tierLevel;
        private Short progressRate;
        private String thumbnailUrl;
        private String lastAccessedAt;
        private Integer instrumentId;
        private String xmlUrl;
        private Short startMeasure;
    }
}
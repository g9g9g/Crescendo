package com.d102.crescendo.domain.sheet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSheetSearchResponse {

    private Integer totalCount;
    private List<SheetItem> sheetList;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SheetItem {
        private Integer sheetId;
        private String title;
        private String composer;
        private String thumbnailUrl;
        private Integer genreId;
        private String tierCode;
        private Short tierLevel;
        private Integer instrumentId;
        private Integer downloadNumber;
        private LocalDateTime updatedAt;
    }
}
package com.d102.crescendo.domain.sheet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSheetSearchResponse {

    private Integer totalCount;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private List<SheetItem> sheetList;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SheetItem {
        private Integer userSheetId;
        private String title;
        private String composer;
        private String thumbnailUrl;
        private Integer instrumentId;
        private Integer genreId;
        private String tierCode;
        private Short tierLevel;
        private Integer progressRate;
        private List<String> highlightedTitle;  // 하이라이팅된 제목
        private List<String> highlightedComposer;  // 하이라이팅된 작곡가
    }
}
package com.d102.crescendo.domain.sheet.dto.response;

import com.d102.crescendo.domain.sheet.document.SheetMusicDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SearchItemResponse {
    private Integer sheetId;
    private String title;
    private String composer;
    private String thumbnailUrl;
    private Integer genreId;
    private String tierCode;
    private Integer tierLevel;
    private Integer instrumentId;
    private Integer downloadNumber;
    private LocalDateTime updatedAt;

    public static SearchItemResponse from(SheetMusicDocument document) {
        return SearchItemResponse.builder()
                .sheetId(document.getSheetId())
                .title(document.getTitle())
                .composer(document.getComposer())
                .thumbnailUrl(document.getThumbnailUrl())
                .genreId(document.getGenreId())
                .tierCode(document.getTierCode())
                .tierLevel(document.getTierLevel())
                .instrumentId(document.getInstrumentId())
                .downloadNumber(document.getDownloadNumber())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
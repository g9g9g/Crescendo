package com.d102.crescendo.domain.sheet.dto.response;

import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularSheetResponse {
    private Integer sheetId;
    private String title;
    private String composer;
    private Integer genreId;
    private String tierCode;
    private Short tierLevel;
    private Integer downloadNumber;
    private String thumbnailUrl;
    private Integer instrumentId;

    public static PopularSheetResponse from(SheetMusic sheetMusic) {
        return PopularSheetResponse.builder()
                .sheetId(sheetMusic.getSheetId())
                .title(sheetMusic.getTitle())
                .composer(sheetMusic.getComposer())
                .genreId(sheetMusic.getGenre() != null ? sheetMusic.getGenre().getGenreId() : null)
                .tierCode(sheetMusic.getTier() != null ? sheetMusic.getTier().getTierCode() : null)
                .tierLevel(sheetMusic.getTier() != null ? sheetMusic.getTier().getTierLevel() : null)
                .downloadNumber(sheetMusic.getDownloadNumber())
                .thumbnailUrl(sheetMusic.getThumbnailUrl())
                .instrumentId(sheetMusic.getInstrument().getInstrumentId())
                .build();
    }
}
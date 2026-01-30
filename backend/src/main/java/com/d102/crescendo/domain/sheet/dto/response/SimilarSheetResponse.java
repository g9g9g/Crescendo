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
public class SimilarSheetResponse {

    private Integer sheetId;
    private String thumbnailUrl;
    private String title;
    private String composer;
    private Integer genreId;
    private String tierCode;
    private Short tierLevel;
    private Integer instrumentId;

    public static SimilarSheetResponse from(SheetMusic sheetMusic) {
        return SimilarSheetResponse.builder()
                .sheetId(sheetMusic.getSheetId())
                .thumbnailUrl(sheetMusic.getThumbnailUrl())
                .title(sheetMusic.getTitle())
                .composer(sheetMusic.getComposer())
                .genreId(sheetMusic.getGenre().getGenreId())
                .tierCode(sheetMusic.getTier().getTierCode())
                .tierLevel(sheetMusic.getTier().getTierLevel())
                .instrumentId(sheetMusic.getInstrument().getInstrumentId())
                .build();
    }
}

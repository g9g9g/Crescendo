package com.d102.crescendo.domain.sheet.event;

import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SheetCreatedEvent {
    private Integer sheetId;
    private String title;
    private String composer;
    private Integer downloadNumber;
    private String genre;
    private String instrument;
    private String thumbnailUrl;
    private Integer genreId;
    private String tierCode;
    private Short tierLevel;
    private Integer instrumentId;
    private LocalDateTime updatedAt;
    private SheetMusic.SourceType sourceType;

    /**
     * SheetMusic 엔티티로부터 이벤트 생성 (Lazy 로딩 방지를 위해 필요한 데이터만 추출)
     */
    public static SheetCreatedEvent from(SheetMusic sheetMusic) {
        return SheetCreatedEvent.builder()
                .sheetId(sheetMusic.getSheetId())
                .title(sheetMusic.getTitle())
                .composer(sheetMusic.getComposer())
                .downloadNumber(sheetMusic.getDownloadNumber())
                .genre(sheetMusic.getGenre() != null ? sheetMusic.getGenre().getName() : null)
                .instrument(sheetMusic.getInstrument().getName())
                .thumbnailUrl(sheetMusic.getThumbnailUrl())
                .genreId(sheetMusic.getGenre() != null ? sheetMusic.getGenre().getGenreId() : null)
                .tierCode(sheetMusic.getTier() != null ? sheetMusic.getTier().getTierCode() : null)
                .tierLevel(sheetMusic.getTier() != null ? sheetMusic.getTier().getTierLevel() : null)
                .instrumentId(sheetMusic.getInstrument().getInstrumentId())
                .updatedAt(sheetMusic.getUpdatedAt())
                .sourceType(sheetMusic.getSourceType())
                .build();
    }
}
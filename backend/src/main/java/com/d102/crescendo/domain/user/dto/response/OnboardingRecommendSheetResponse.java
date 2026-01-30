package com.d102.crescendo.domain.user.dto.response;

import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingRecommendSheetResponse {
    private List<SheetInfo> sheets;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SheetInfo {
        private Integer sheetId;
        private String thumbnailUrl;
        private String title;
        private String composer;
        private Integer genreId;
        private String tierCode;
        private Short tierLevel;

        public static SheetInfo from(SheetMusic sheetMusic) {
            return SheetInfo.builder()
                    .sheetId(sheetMusic.getSheetId())
                    .thumbnailUrl(sheetMusic.getThumbnailUrl())
                    .title(sheetMusic.getTitle())
                    .composer(sheetMusic.getComposer())
                    .genreId(sheetMusic.getGenre().getGenreId())
                    .tierCode(sheetMusic.getTier().getTierCode())
                    .tierLevel(sheetMusic.getTier().getTierLevel())
                    .build();
        }
    }

    public static OnboardingRecommendSheetResponse from(List<SheetMusic> sheetMusicList) {
        List<SheetInfo> sheets = sheetMusicList.stream()
                .map(SheetInfo::from)
                .toList();

        return OnboardingRecommendSheetResponse.builder()
                .sheets(sheets)
                .build();
    }
}

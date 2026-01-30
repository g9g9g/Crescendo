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
public class UserSheetAutocompleteResponse {

    private List<AutocompleteItem> suggestions;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AutocompleteItem {
        private Integer userSheetId;
        private String title;
        private String composer;
        private String highlightedText;  // 하이라이팅된 텍스트
    }
}
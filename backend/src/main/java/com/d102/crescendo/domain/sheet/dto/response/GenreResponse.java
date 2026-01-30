package com.d102.crescendo.domain.sheet.dto.response;

import com.d102.crescendo.domain.sheet.entity.Genre;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class GenreResponse {
    private Integer id;
    private String engName;
    private String korName;

    private static final Map<String, String> KOR_NAME_MAP = Map.of(
            "classic", "클래식",
            "pop", "팝",
            "new_age", "뉴에이지",
            "ost", "OST",
            "children", "동요",
            "ccm", "CCM"
    );

    public static GenreResponse from(Genre genre) {
        return GenreResponse.builder()
                .id(genre.getGenreId())
                .engName(genre.getName())
                .korName(KOR_NAME_MAP.getOrDefault(genre.getName(), ""))
                .build();
    }
}
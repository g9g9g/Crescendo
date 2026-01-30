package com.d102.crescendo.domain.common.dto.response;

import com.d102.crescendo.domain.sheet.dto.response.GenreResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GenreListResponse {
    private List<GenreResponse> genres;
}
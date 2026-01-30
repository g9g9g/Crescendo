package com.d102.crescendo.domain.sheet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class SearchResponse {
    private long totalCount;
    private List<SearchItemResponse> sheetList;

}

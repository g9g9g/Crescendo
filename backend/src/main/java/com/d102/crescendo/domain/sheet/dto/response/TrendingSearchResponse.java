package com.d102.crescendo.domain.sheet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingSearchResponse {
    private Instant generatedAt;
    private List<String> keywords;
}

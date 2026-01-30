package com.d102.crescendo.domain.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RecommendResponse {
    private Integer queryScoreId;
    private int topK;
    private List<Integer> results;
}
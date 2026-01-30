package com.d102.crescendo.domain.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DifficultyEvaluationResponse {

    private Integer level;  // tierId -> SheetMusic.tier에 저장

    private Metrics metrics;

    private String summary;

    private List<String> recommendations;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Metrics {
        private Double tempo;
        private Double rhythm;
        private Double intervals;
        private Double harmony;
        private Double technique;
        private Double length;
    }
}
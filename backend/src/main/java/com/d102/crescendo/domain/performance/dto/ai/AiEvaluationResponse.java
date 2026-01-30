package com.d102.crescendo.domain.performance.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEvaluationResponse {

    private Boolean success; // 성공 여부

    @JsonProperty("overall_score")
    private Double overallScore; // 총점

    private String grade; // 등급 (A+, B+, etc.)

    private Map<String, MetricDetail> metrics; // 각 지표별 점수

    private List<String> feedback; // 피드백 메시지 배열

    private Stats stats; // 통계 정보

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricDetail {
        private String name; // 지표 영문명
        private Double score; // 점수
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        @JsonProperty("total_notes")
        private Integer totalNotes; // 총 음표 수

        private Double duration; // 연주 시간 (초)

        @JsonProperty("avg_velocity")
        private Double avgVelocity; // 평균 속도
    }
}
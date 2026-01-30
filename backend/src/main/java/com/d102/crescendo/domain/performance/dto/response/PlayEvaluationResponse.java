package com.d102.crescendo.domain.performance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 연주 평가 결과 응답")
public class PlayEvaluationResponse {

    @Schema(description = "평가 상태 (COMPLETED: 평가 완료, EVALUATION: 평가 중, NOT_AVAILABLE: 연습 모드 또는 녹음 파일 없음)", example = "COMPLETED")
    private String status;

    @Schema(description = "총점", example = "91")
    private Short totalScore;

    @Schema(description = "등급", example = "A+")
    private String grade;

    @Schema(description = "AI 코멘트", example = "리듬은 좋지만 후반부 강약 조절이 필요합니다.")
    private String comment;

    @Schema(description = "평가 지표 목록")
    private List<MetricDetail> metrics;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "평가 지표 상세")
    public static class MetricDetail {
        @Schema(description = "지표 코드", example = "tempo_stability")
        private String code;

        @Schema(description = "지표 한글명", example = "템포 안정성")
        private String name;

        @Schema(description = "점수", example = "85")
        private Short score;
    }
}

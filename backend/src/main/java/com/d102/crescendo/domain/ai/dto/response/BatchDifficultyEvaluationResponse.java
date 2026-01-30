package com.d102.crescendo.domain.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDifficultyEvaluationResponse {

    private List<Integer> successIds;  // 성공한 sheetId 리스트
    private Map<Integer, String> failedIds;   // 실패한 sheetId와 실패 이유 맵
    private List<DifficultyEvaluationResponse> results;  // 성공한 난이도 평가 결과들

    private Integer totalCount;        // 전체 요청 수
    private Integer successCount;      // 성공 수
    private Integer failedCount;       // 실패 수
}

package com.d102.crescendo.domain.performance.controller;

import com.d102.crescendo.domain.auth.security.UserDetailsImpl;
import com.d102.crescendo.domain.performance.dto.request.PlayResultRequest;
import com.d102.crescendo.domain.performance.dto.response.PlayEndResponse;
import com.d102.crescendo.domain.performance.dto.response.PlayEvaluationResponse;
import com.d102.crescendo.domain.performance.dto.response.RecentPerformanceResponse;
import com.d102.crescendo.domain.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plays")
@RequiredArgsConstructor
@Tag(name = "03. Performance", description = "연주 관련 API")
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping
    @Operation(summary = "최근 연주 기록 조회", description = "현재 로그인한 사용자의 최근 연주 기록을 조회합니다.")
    public ResponseEntity<RecentPerformanceResponse> getRecentPerformances(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        RecentPerformanceResponse response = performanceService.getRecentPerformances(
                userDetails.getUser().getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/end")
    @Operation(summary = "연주 기록 저장", description = "연주가 종료될 때 호출하여 연주 결과를 저장합니다.")
    public ResponseEntity<PlayEndResponse> endPlay(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody PlayResultRequest request) {
        PlayEndResponse response = performanceService.savePlayResult(
                userDetails.getUser().getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{playId}")
    @Operation(summary = "AI 연주 평가 결과 조회", description = "연주 ID로 AI 평가 결과를 조회합니다. 평가가 완료되지 않았으면 EVALUATION 상태를 반환합니다.")
    public ResponseEntity<PlayEvaluationResponse> getPlayEvaluation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Integer playId) {
        PlayEvaluationResponse response = performanceService.getPlayEvaluation(
                userDetails.getUser().getUserId(), playId);
        return ResponseEntity.ok(response);
    }

}

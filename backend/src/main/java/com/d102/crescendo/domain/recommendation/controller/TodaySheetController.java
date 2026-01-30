package com.d102.crescendo.domain.recommendation.controller;

import com.d102.crescendo.domain.auth.security.UserDetailsImpl;
import com.d102.crescendo.domain.recommendation.dto.TodaySheetListResponse;
import com.d102.crescendo.domain.recommendation.service.DailyRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sheets")
@Tag(name = "05. Sheet")
public class TodaySheetController {

    private final DailyRecommendationService dailyRecommendationService;

    @GetMapping("/today")
    @Operation(summary = "오늘의 추천 악보 조회", description = "사용자의 과거 연주 행동을 기반으로 오늘의 악보를 추천합니다.")
    public ResponseEntity<TodaySheetListResponse> getTodaySheets(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        TodaySheetListResponse res = dailyRecommendationService.getTodaySheets(userDetails.getUser().getUserId());
        return ResponseEntity.ok(res);
    }
}
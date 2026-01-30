package com.d102.crescendo.domain.rank.controller;

import com.d102.crescendo.domain.rank.dto.response.RankResponse;
import com.d102.crescendo.domain.rank.service.RankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rank")
@Tag(name = "04. Rank", description = "랭킹 관련 API")
public class RankController {

    private final RankService rankService;

    /**
     * 상위랭커 TOP 20 조회 API
     * @param instrumentId 악기 ID (required)
     * @return 상위 20명의 랭킹 정보
     */
    @GetMapping
    @Operation(summary = "일일 랭킹 조회", description = "악기별로 상위 20명의 유저를 조회합니다.")
    public ResponseEntity<RankResponse> getTop20Rankings(
            @RequestParam(name = "instrumentId") Integer instrumentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        RankResponse response = rankService.getTop20Rankings(instrumentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 랭킹 수동 계산 API (테스트용)
     * @return 처리 결과 메시지
     */
    @PostMapping("/test")
    @Operation(summary = "랭킹 수동 계산", description = "일일 랭킹을 즉시 계산합니다. (테스트용)")
    public ResponseEntity<String> calculateRankingsManually(
            @AuthenticationPrincipal UserDetails userDetails) {
        rankService.calculateAndSaveDailyRankings();
        return ResponseEntity.ok("Daily rankings calculated successfully");
    }
}

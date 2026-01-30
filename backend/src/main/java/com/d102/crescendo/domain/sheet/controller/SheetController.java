package com.d102.crescendo.domain.sheet.controller;

import com.d102.crescendo.domain.sheet.dto.response.*;
import com.d102.crescendo.domain.sheet.service.SearchLogService;
import com.d102.crescendo.domain.sheet.service.ServiceSheetService;
import com.d102.crescendo.domain.sheet.service.SheetRecommendationService;
import com.d102.crescendo.domain.sheet.service.TrendingSearchService;
import com.d102.crescendo.domain.sheet.service.UserSheetService;
import com.d102.crescendo.domain.auth.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/sheets/service")
@Tag(name = "05. Sheet", description = "스토어 악보 관련 API")
@RequiredArgsConstructor
public class SheetController {

    private final SheetRecommendationService sheetRecommendationService;
    private final ServiceSheetService serviceSheetService;
    private final UserSheetService userSheetService;
    private final SearchLogService searchLogService;
    private final TrendingSearchService trendingSearchService;

    @GetMapping("/popular")
    @Operation(
            summary = "인기 악보 조회",
            description = "다운로드 수 기준 Top 20 악보를 하루 단위로 Redis에 캐싱하고, 조회 시 캐시된 목록을 반환합니다."
    )
    public ResponseEntity<List<PopularSheetResponse>> getPopularSheets() {
        List<PopularSheetResponse> popularSheets = sheetRecommendationService.getPopularSheets();
        return ResponseEntity.ok(popularSheets);
    }

    @GetMapping("/suggestions")
    @Operation(
            summary = "검색어 자동완성",
            description = "사용자가 악보 검색어를 입력할 때, 입력 중인 텍스트를 기반으로 자동완성 추천어를 제공합니다."
    )
    public ResponseEntity<List<String>> getSuggestions(@RequestParam String q) {
        List<String> suggestions = serviceSheetService.getSuggestions(q);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping
    @Operation(
            summary = "스토어 악보 검색",
            description = "Elasticsearch 기반으로 악보를 검색합니다."
    )
    public ResponseEntity<SearchResponse> searchSheets(
            @Parameter(description = "검색어") @RequestParam(required = false) String q,
            @Parameter(description = "장르 ID로 필터링") @RequestParam(required = false) Integer genreId,
            @Parameter(description = "악기 ID로 필터링") @RequestParam(required = false) Integer instrumentId,
            @Parameter(description = "난이도 코드로 필터링") @RequestParam(required = false) String tierCode,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지당 결과 개수") @RequestParam(defaultValue = "5") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal(errorOnInvalidType = false) UserDetailsImpl userDetails
    ) {
        SearchResponse response = serviceSheetService.searchSheets(q, genreId, instrumentId, tierCode, page, size);

        // 검색 로그 저장
        if (q != null && !q.trim().isEmpty()) {
            Integer userId = userDetails != null ? userDetails.getUser().getUserId() : null;
            searchLogService.saveSearchLog(q, userId);
        }

        return ResponseEntity.ok(response);
    }

//    @GetMapping
//    @Operation(
//            summary = "스토어 악보 검색",
//            description = "검색어와 필터 조건으로 스토어 악보를 검색합니다. 모든 파라미터는 선택사항이며, 조건이 없을 경우 전체 리스트를 반환합니다.",
//            security = { @SecurityRequirement(name = "bearerAuth") }
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "검색 성공"),
//            @ApiResponse(responseCode = "401", description = "인증 실패")
//    })
//    public ResponseEntity<ServiceSheetSearchResponse> searchServiceSheets(
//            @Parameter(description = "검색어 (제목, 작곡가)", example = "river")
//            @RequestParam(required = false) String q,
//            @Parameter(description = "악기 ID", example = "1")
//            @RequestParam(required = false) Integer instrumentId,
//            @Parameter(description = "장르 ID (콤마로 구분)", example = "1,3,5")
//            @RequestParam(required = false) String genreId,
//            @Parameter(description = "티어 코드", example = "silver")
//            @RequestParam(required = false) String tierCode,
//            @Parameter(description = "정렬 타입 (1: 최신순, 2: 다운로드순, 3: 제목순)", example = "1")
//            @RequestParam(required = false) Integer sortType) {
//
//        log.info("스토어 악보 검색 요청 - q: {}, instrumentId: {}, genreId: {}, tierCode: {}, sortType: {}",
//                q, instrumentId, genreId, tierCode, sortType);
//
//        ServiceSheetSearchResponse response = serviceSheetService.searchServiceSheets(
//                q, instrumentId, genreId, tierCode, sortType
//        );
//
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/{sheetId}")
    @Operation(summary = "스토어 악보 단건 조회",
            description = "시스템에서 제공하는 악보의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "악보를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "비공개 악보")
    })
    public ResponseEntity<ServiceSheetDetailResponse> getServiceSheet(
            @Parameter(description = "악보 ID", required = true, example = "1")
            @PathVariable("sheetId") Integer sheetId) {

        log.info("스토어 악보 조회 요청 - sheetId: {}", sheetId);

        ServiceSheetDetailResponse response = serviceSheetService.getServiceSheet(sheetId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sheetId}/download")
    @Operation(
            summary = "내 악보로 담기",
            description = "스토어 악보를 사용자의 내 악보에 추가합니다. user_sheet 테이블에 레코드가 생성되고 download_number가 1 증가합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "담기 성공"),
            @ApiResponse(responseCode = "400", description = "스토어 악보가 아님"),
            @ApiResponse(responseCode = "404", description = "악보를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 담은 악보"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> downloadSheetToMyLibrary(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "악보 ID", required = true, example = "1")
            @PathVariable("sheetId") Integer sheetId) {

        Integer userId = userDetails.getUser().getUserId();

        log.info("내 악보로 담기 요청 - userId: {}, sheetId: {}", userId, sheetId);

        userSheetService.addSheetToMyLibrary(userId, sheetId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{sheetId}/similar")
    @Operation(summary = "사용자 추천 악보 조회", description = "지금 보고있는 악보와 비슷한 악보를 추천받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "악보를 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "임베딩 정보가 없음")
    })
    public ResponseEntity<List<SimilarSheetResponse>> getSimilarSheets(
            @Parameter(description = "악보 ID", required = true, example = "1")
            @PathVariable("sheetId") Integer sheetId) {

        log.info("유사 악보 조회 요청 - sheetId: {}", sheetId);

        List<SimilarSheetResponse> similarSheets = sheetRecommendationService.getSimilarSheets(sheetId);

        return ResponseEntity.ok(similarSheets);
    }

    @GetMapping("/trending")
    @Operation(
            summary = "실시간 인기 검색어 조회",
            description = "최근 검색 로그를 기반으로 집계된 실시간 인기 검색어 top 12를 조회합니다. " +
                    "매 1분마다 스케줄러가 집계하여 Redis에 캐싱합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<TrendingSearchResponse> getTrendingSearches() {
        log.info("실시간 인기 검색어 조회 요청");
        TrendingSearchResponse response = trendingSearchService.getTrendingSearches();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/migrate-to-elasticsearch")
    @Operation(
            summary = "[관리자] SheetMusic 데이터 Elasticsearch 마이그레이션",
            description = "PostgreSQL의 모든 SheetMusic 데이터를 Elasticsearch로 일괄 이전합니다. " +
                    "visibleYes=true인 악보만 마이그레이션됩니다. " +
                    "기존 데이터 동기화 또는 재색인 시 사용합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이그레이션 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<java.util.Map<String, Object>> migrateToElasticsearch(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("SheetMusic Elasticsearch 마이그레이션 요청 - userId: {}", userDetails.getUser().getUserId());

        // 마이그레이션 실행
        int migratedCount = serviceSheetService.migrateAllToElasticsearch();

        // 응답 생성
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "SheetMusic 데이터 Elasticsearch 마이그레이션 완료");
        response.put("migratedCount", migratedCount);

        return ResponseEntity.ok(response);
    }
}

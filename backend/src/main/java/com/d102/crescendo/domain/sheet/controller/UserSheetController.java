package com.d102.crescendo.domain.sheet.controller;

import com.d102.crescendo.domain.auth.security.UserDetailsImpl;
import com.d102.crescendo.domain.sheet.dto.request.UserSheetArrangeRequest;
import com.d102.crescendo.domain.sheet.dto.request.UserSheetCreateRequest;
import com.d102.crescendo.domain.sheet.dto.response.UserSheetAutocompleteResponse;
import com.d102.crescendo.domain.sheet.dto.response.UserSheetCreateResponse;
import com.d102.crescendo.domain.sheet.dto.response.UserSheetDetailResponse;
import com.d102.crescendo.domain.sheet.dto.response.UserSheetSearchResponse;
import com.d102.crescendo.domain.sheet.service.SheetRegistrationService;
import com.d102.crescendo.domain.sheet.service.UserSheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/sheets/user")
@Tag(name = "06. UserSheet", description = "사용자 악보 관련 API")
@RequiredArgsConstructor
public class UserSheetController {

    private final SheetRegistrationService sheetRegistrationService;
    private final UserSheetService userSheetService;

    @Operation(
            summary = "사용자 악보 등록",
            description = "사용자가 소유/업로드한 악보를 등록합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping
    public ResponseEntity<UserSheetCreateResponse> registerUserSheet(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UserSheetCreateRequest req) {

        Integer userId = userDetails.getUser().getUserId();

        log.info("악보등록요청-사용자: {}, 제목:{}, Xml Url:{}",
                userId, req.getTitle(), req.getXmlUrl());

        UserSheetCreateResponse response = sheetRegistrationService.registerUserSheet(userId, req);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "내 악보 검색",
            description = "검색어와 필터 조건으로 내 악보를 검색합니다. " +
                    "제목, 작곡가, 악기, 장르를 가중치 기반으로 검색하며 오타를 허용합니다. " +
                    "검색 결과는 하이라이팅되어 반환됩니다. 기본 5개씩 페이지네이션 처리됩니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<UserSheetSearchResponse> searchUserSheets(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "검색어 (제목, 작곡가, 악기, 장르)", example = "river")
            @RequestParam(required = false) String q,
            @Parameter(description = "악기 ID (필터)", example = "1")
            @RequestParam(required = false) Integer instrumentId,
            @Parameter(description = "장르 ID (콤마로 구분, 필터)", example = "1,3,5")
            @RequestParam(required = false) String genreId,
            @Parameter(description = "티어 코드 (필터)", example = "silver")
            @RequestParam(required = false) String tierCode,
            @Parameter(description = "최소 티어 레벨 (범위 검색)", example = "1")
            @RequestParam(required = false) Integer minTierLevel,
            @Parameter(description = "최대 티어 레벨 (범위 검색)", example = "5")
            @RequestParam(required = false) Integer maxTierLevel,
            @Parameter(description = "정렬 타입 (1: 최신순, 2: 진행률순, 3: 제목순)", example = "1")
            @RequestParam(required = false) Integer sortType,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기", example = "5")
            @RequestParam(required = false, defaultValue = "5") Integer size) {

        Integer userId = userDetails.getUser().getUserId();

        log.info("내 악보 검색 요청 - userId: {}, q: {}, instrumentId: {}, genreId: {}, tierCode: {}, tierLevel: {}-{}, sortType: {}, page: {}, size: {}",
                userId, q, instrumentId, genreId, tierCode, minTierLevel, maxTierLevel, sortType, page, size);

        UserSheetSearchResponse response = userSheetService.searchUserSheets(
                userId, q, instrumentId, genreId, tierCode, minTierLevel, maxTierLevel, sortType, page, size
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/autocomplete")
    @Operation(
            summary = "내 악보 자동완성",
            description = "제목 또는 작곡가를 기반으로 내 악보를 자동완성합니다. 연관된 모든 결과를 반환합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "자동완성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<List<String>> autocompleteUserSheets(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "검색어", required = true, example = "river")
            @RequestParam String q) {

        Integer userId = userDetails.getUser().getUserId();

        log.info("내 악보 자동완성 요청 - userId: {}, q: {}", userId, q);

        List<String> response = userSheetService.autocompleteUserSheets(userId, q);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userSheetId}")
    @Operation(
            summary = "내 악보 상세 조회",
            description = "사용자가 소유한 악보의 상세 정보와 연주 기록을 조회합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자 악보를 찾을 수 없음")
    })
    public ResponseEntity<UserSheetDetailResponse> getUserSheetDetail(
            @Parameter(description = "사용자 악보 ID", required = true, example = "1")
            @PathVariable("userSheetId") Integer userSheetId) {

        log.info("내 악보 상세 조회 요청 - userSheetId: {}", userSheetId);

        UserSheetDetailResponse response = userSheetService.getUserSheetDetail(userSheetId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userSheetId}")
    @Operation(
            summary = "내 악보 삭제",
            description = "사용자가 소유한 악보를 소프트 삭제합니다 (deleted_yes=true, deleted_at=현재시간).",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "사용자 악보를 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteUserSheet(
            @Parameter(description = "사용자 악보 ID", required = true, example = "1")
            @PathVariable("userSheetId") Integer userSheetId) {

        log.info("내 악보 삭제 요청 - userSheetId: {}", userSheetId);

        userSheetService.deleteUserSheet(userSheetId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userSheetId}/arrange")
    @Operation(
            summary = "악보 편곡",
            description = "AI 서버를 통해 악보를 편곡합니다. 편곡된 악보는 새로운 SheetMusic과 UserSheet로 저장됩니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<Void> arrangeUserSheet(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "사용자 악보 ID", required = true, example = "1")
            @PathVariable("userSheetId") Integer userSheetId,
            @Valid @RequestBody UserSheetArrangeRequest req) {
        Integer userId = userDetails.getUser().getUserId();
        userSheetService.arrangeUserSheet(userId, userSheetId, req.getStyle(), req.getXmlUrl());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/migrate-to-elasticsearch")
    @Operation(
            summary = "[관리자] UserSheet 데이터 Elasticsearch 마이그레이션",
            description = "PostgreSQL의 모든 UserSheet 데이터를 Elasticsearch로 일괄 이전합니다. " +
                    "기존 데이터 동기화 또는 재색인 시 사용합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이그레이션 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<java.util.Map<String, Object>> migrateToElasticsearch(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("UserSheet Elasticsearch 마이그레이션 요청 - userId: {}", userDetails.getUser().getUserId());

        // 마이그레이션 실행
        int migratedCount = userSheetService.migrateAllToElasticsearch();

        // 응답 생성
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "UserSheet 데이터 Elasticsearch 마이그레이션 완료");
        response.put("migratedCount", migratedCount);

        return ResponseEntity.ok(response);
    }

}

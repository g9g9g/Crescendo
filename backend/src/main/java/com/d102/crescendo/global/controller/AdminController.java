package com.d102.crescendo.global.controller;

import com.d102.crescendo.domain.auth.security.UserDetailsImpl;
import com.d102.crescendo.domain.recommendation.service.TodaySheetBatchService;
import com.d102.crescendo.domain.sheet.service.ServiceSheetService;
import com.d102.crescendo.global.dto.request.UpdateSheetMusicRequest;
import com.d102.crescendo.global.dto.response.SeedDataResponse;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import com.d102.crescendo.global.service.SeedDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "관리자 전용 API")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final SeedDataService seedDataService;
    private final ServiceSheetService serviceSheetService;
    private final TodaySheetBatchService todaySheetBatchService;

    @PostMapping("/seed-user-sheets")
    @Operation(
            summary = "사용자 악보 시드 데이터 등록",
            description = "resources/seed-data/sheets/ 폴더의 모든 XML 파일을 S3에 업로드하고 DB에 등록합니다. ADMIN 권한 필요.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<SeedDataResponse> seedUserSheets(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "악보를 등록할 사용자 ID", example = "1")
            @RequestParam Integer userId,
            @Parameter(description = "악기 ID (선택, 기본값: 1)", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer instrumentId,
            @Parameter(description = "장르 ID", example = "1")
            @RequestParam Integer genreId) {

        // ADMIN 권한 체크
        if (!userDetails.getUser().getRole().name().equals("ADMIN")) {
            throw new BusinessException(BusinessError.FORBIDDEN);
        }

        log.info("시드 데이터 등록 요청 - userId: {}, genreId: {}",
                userId, genreId);

        SeedDataResponse response = seedDataService.seedUserSheets(userId, genreId);

        return ResponseEntity.ok(response);
    }

//    @PatchMapping("/sheets/{sheet-id}/visibility")
//    @Operation(
//            summary = "시스템 악보 숨김/노출 상태 변경",
//            description = "ADMIN 권한으로 시스템 악보의 노출 여부를 변경합니다. " +
//                    "visible=false인 경우 Elasticsearch에서 문서가 제거되고, " +
//                    "visible=true인 경우 Elasticsearch에 문서가 생성됩니다.",
//            security = { @SecurityRequirement(name = "bearerAuth") }
//    )
//    public ResponseEntity<Void> updateSheetVisibility(
//            @AuthenticationPrincipal UserDetailsImpl userDetails,
//            @Parameter(description = "악보 ID", example = "1", required = true)
//            @PathVariable("sheet-id") Integer sheetId,
//            @RequestBody UpdateSheetVisibilityRequest request) {
//
//        // ADMIN 권한 체크
//        if (!userDetails.getUser().getRole().name().equals("ADMIN")) {
//            throw new BusinessException(BusinessError.FORBIDDEN);
//        }
//
//        log.info("악보 숨김/노출 변경 요청 - sheetId: {}, visible: {}, adminId: {}",
//                sheetId, request.getVisible(), userDetails.getUser().getUserId());
//
//        serviceSheetService.updateSheetVisibility(sheetId, request.getVisible());
//
//        return ResponseEntity.ok().build();
//    }

    @PatchMapping("/sheets/{sheet-id}")
    @Operation(
            summary = "악보 정보 수정",
            description = "ADMIN 권한으로 악보의 모든 정보를 수정합니다. " +
                    "수정하고자 하는 필드만 요청 바디에 포함하면 됩니다. " +
                    "변경 후 Elasticsearch 문서도 자동으로 동기화됩니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<Void> updateSheetMusic(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "악보 ID", example = "1", required = true)
            @PathVariable("sheet-id") Integer sheetId,
            @RequestBody UpdateSheetMusicRequest request) {

        // ADMIN 권한 체크
        if (!userDetails.getUser().getRole().name().equals("ADMIN")) {
            throw new BusinessException(BusinessError.FORBIDDEN);
        }

        log.info("악보 정보 수정 요청 - sheetId: {}, adminId: {}, request: {}",
                sheetId, userDetails.getUser().getUserId(), request);

        serviceSheetService.updateSheetMusic(sheetId, request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch/recommendation")
    @Operation(summary = "오늘의 추천 배치 수동 실행")
    public ResponseEntity<String> runRecommendationBatch(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        todaySheetBatchService.runDailyRecommendation();
        return ResponseEntity.ok("배치 실행 완료");
    }
}
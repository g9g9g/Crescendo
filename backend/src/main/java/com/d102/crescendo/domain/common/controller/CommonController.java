package com.d102.crescendo.domain.common.controller;

import com.d102.crescendo.domain.common.dto.response.GenreListResponse;
import com.d102.crescendo.domain.common.dto.response.InstrumentListResponse;
import com.d102.crescendo.domain.common.service.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
@Tag(name = "00. Common", description = "공통 코드 관련 API")
public class CommonController {

    private final CommonService commonService;

    @GetMapping("/genres")
    @Operation(summary = "장르 목록 조회", description = "전체 장르 목록을 조회합니다.")
    public ResponseEntity<GenreListResponse> getGenres() {
        return ResponseEntity.ok(new GenreListResponse(commonService.getGenres()));
    }

    @GetMapping("/instruments")
    @Operation(summary = "악기 목록 조회", description = "전체 악기 목록을 조회합니다.")
    public ResponseEntity<InstrumentListResponse> getInstruments() {
        return ResponseEntity.ok(new InstrumentListResponse(commonService.getInstruments()));
    }
}

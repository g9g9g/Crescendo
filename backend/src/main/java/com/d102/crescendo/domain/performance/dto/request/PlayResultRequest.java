package com.d102.crescendo.domain.performance.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "연주 결과 저장 요청")
public class PlayResultRequest {

    @NotNull(message = "소유 악보 ID는 필수입니다.")
    @Schema(description = "소유 악보 ID", example = "123")
    private Integer userSheetId;

    @NotNull(message = "연습 모드 여부는 필수입니다.")
    @Schema(description = "연습 모드 여부 (true: 연습 모드, false: 평가 모드)", example = "false")
    private Boolean practiceMode;

    @NotNull(message = "시작 마디는 필수입니다.")
    @Min(value = 1, message = "시작 마디는 1 이상이어야 합니다.")
    @Schema(description = "시작 마디", example = "1")
    private Short startMeasure;

    @NotNull(message = "종료 마디는 필수입니다.")
    @Min(value = 1, message = "종료 마디는 1 이상이어야 합니다.")
    @Schema(description = "종료 마디", example = "5")
    private Short endMeasure;

    @NotNull(message = "연주 시작 시간은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "연주 시작 시간", example = "2025-11-03 22:55:17")
    private LocalDateTime startedAt;

    private String wavXmlUrl;

}

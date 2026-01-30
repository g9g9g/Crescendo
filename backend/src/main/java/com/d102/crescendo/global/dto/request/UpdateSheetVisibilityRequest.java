package com.d102.crescendo.global.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "악보 숨김/노출 변경 요청")
public class UpdateSheetVisibilityRequest {

    @NotNull(message = "노출 여부는 필수입니다.")
    @Schema(description = "노출 여부 (true: 노출, false: 숨김)", example = "true", required = true)
    private Boolean visible;
}
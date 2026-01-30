package com.d102.crescendo.domain.sheet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class UserSheetArrangeRequest {

    @NotBlank(message = "편곡 스타일은 필수입니다")
    @Schema(description = "편곡 스타일 (예: 재즈풍으로 변형해줘)", example = "재즈풍으로 변형해줘")
    private String style;

    @NotBlank(message = "xml url은 필수입니다")
    private String xmlUrl;
}
package com.d102.crescendo.domain.sheet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class UserSheetCreateRequest {

    @NotBlank(message = "악보제목은 필수입니다.")
    @Schema(description = "악보제목")
    private String title;

    private String composer;

    @NotBlank(message = "XML URL은 필수입니다.")
    @Schema(description = "클라이언트가 S3에 업로드한 MusicXML 파일 URL")
    private String xmlUrl;

    @NotNull(message = "악기는 필수입니다.")
    private Integer instrumentId;

    @NotNull(message = "장르는 필수입니다.")
    private Integer genreId;
}

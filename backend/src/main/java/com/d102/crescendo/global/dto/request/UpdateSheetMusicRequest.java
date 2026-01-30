package com.d102.crescendo.global.dto.request;

import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "악보 정보 수정 요청")
public class UpdateSheetMusicRequest {

    @Schema(description = "악보 노출 여부", example = "true")
    private Boolean visibleYes;

    @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다.")
    @Schema(description = "악보 제목", example = "베토벤 소나타")
    private String title;

    @Size(max = 50, message = "작곡가는 최대 50자까지 입력 가능합니다.")
    @Schema(description = "작곡가", example = "베토벤")
    private String composer;

    @Schema(description = "장르 ID", example = "1")
    private Integer genreId;

    @Schema(description = "악기 ID", example = "1")
    private Integer instrumentId;

    @Schema(description = "티어 ID", example = "1")
    private Integer tierId;

    @Schema(description = "악보 소스 타입", example = "SYSTEM", allowableValues = {"SYSTEM", "USER", "ARRANGED"})
    private SheetMusic.SourceType sourceType;

    @Size(max = 2048, message = "썸네일 URL은 최대 2048자까지 입력 가능합니다.")
    @Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.jpg")
    private String thumbnailUrl;

    @Size(max = 2048, message = "XML URL은 최대 2048자까지 입력 가능합니다.")
    @Schema(description = "전체 XML URL", example = "https://example.com/sheet.xml")
    private String xmlUrl;

    @Size(max = 2048, message = "미리보기 XML URL은 최대 2048자까지 입력 가능합니다.")
    @Schema(description = "미리보기 XML URL", example = "https://example.com/preview.xml")
    private String xmlUrlPreview;

    @Max(value = Short.MAX_VALUE, message = "최대 마디 수는 " + Short.MAX_VALUE + "을 초과할 수 없습니다.")
    @Schema(description = "최대 마디 수", example = "100")
    private Short maxMeasureCnt;

    @Size(max = 500, message = "스타일은 최대 500자까지 입력 가능합니다.")
    @Schema(description = "편곡 스타일", example = "jazz")
    private String style;

    @Schema(description = "원본 악보 ID (편곡된 경우)", example = "1")
    private Integer originSheetId;

    @Schema(description = "다운로드 수", example = "100")
    private Integer downloadNumber;

    @Schema(description = "임베딩 벡터 (1024차원)", example = "[0.0123, -0.211, 0.993, ...]")
    private float[] embedding;

    @Schema(description = "생성일시", example = "2025-11-03T22:55:17")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-11-11T17:32:54")
    private LocalDateTime updatedAt;
}

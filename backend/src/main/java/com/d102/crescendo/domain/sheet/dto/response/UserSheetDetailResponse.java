package com.d102.crescendo.domain.sheet.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Builder
public class UserSheetDetailResponse {

        @Schema(description = "사용자 악보 ID", example = "1")
        private Integer userSheetId;

        @Schema(description = "악보 제목", example = "River Flows in You")
        private String title;

        @Schema(description = "작곡가", example = "Yiruma")
        private String composer;

        @Schema(description = "장르 ID", example = "3")
        private Integer genreId;

        @Schema(description = "티어 코드", example = "bronze")
        private String tierCode;

        @Schema(description = "티어 레벨", example = "2")
        private Short tierLevel;

        @Schema(description = "악기 ID", example = "1")
        private Integer instrumentId;

        @Schema(description = "썸네일 URL")
        private String thumbnailUrl;

        @Schema(description = "XML URL")
        private String xmlUrl;

        @Schema(description = "시작 마디 (이전 연주 기록의 끝 마디, 없으면 1)", example = "10")
        private Short startMeasure;

        @Schema(description = "연주 기록 목록")
        private PerformanceList performances;

        @Schema(description = "난이도 지표 (티어가 있는 경우에만)")
        private DifficultyMetrics metrics;

        @Schema(description = "진도율", example = "75")
        private Integer progress;

        @Getter
        @Builder
        @Schema(description = "연주 기록 목록")
        public static class PerformanceList {
            @Schema(description = "연주 기록 아이템들")
            private List<PerformanceItem> items;
        }

        @Getter
        @Builder
        @Schema(description = "연주 기록 아이템")
        public static class PerformanceItem {

            @Schema(description = "연주 ID", example = "1")
            private Integer performanceId;


            @Schema(description = "총점", example = "80")
            private Short totalScore;

            @Schema(description = "코멘트", example = "박자감을 더 신경 쓰면 훨씬 자연스럽게 들릴 거예요!")
            private String comment;

            @Schema(description = "연주 종료 시간", example = "2025-10-21T20:15:30")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime endedAt;

            private String wavXmlUrl;
        }

        @Getter
        @Builder
        @Schema(description = "난이도 지표")
        public static class DifficultyMetrics {
            @Schema(description = "템포 난이도", example = "0.75")
            private Double tempo;

            @Schema(description = "리듬 난이도", example = "0.82")
            private Double rhythm;

            @Schema(description = "음정 난이도", example = "0.68")
            private Double intervals;

            @Schema(description = "화성 난이도", example = "0.90")
            private Double harmony;

            @Schema(description = "테크닉 난이도", example = "0.85")
            private Double technique;

            @Schema(description = "길이 난이도", example = "0.72")
            private Double length;
        }

}

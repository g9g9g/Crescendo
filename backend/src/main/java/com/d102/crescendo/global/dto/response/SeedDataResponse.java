package com.d102.crescendo.global.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SeedDataResponse {
    private Integer totalFiles;
    private Integer successCount;
    private Integer failCount;
    private List<SheetResult> results;

    @Getter
    @Builder
    public static class SheetResult {
        private String fileName;
        private boolean success;
        private String message;
        private Integer sheetId;
        private Integer userSheetId;
    }
}
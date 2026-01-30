package com.d102.crescendo.domain.recommendation.dto;

public record TodaySheetItemResponse(
        Integer sheetId,
        String title,
        String composer,
        Integer genreId,
        String tierCode,
        Short tierLevel,
        Integer downloadNumber,
        String thumbnailUrl,
        Integer instrumentId
) {}

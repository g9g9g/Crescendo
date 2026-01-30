package com.d102.crescendo.domain.recommendation.dto;

import java.util.List;

public record TodaySheetListResponse(
        List<TodaySheetItemResponse> items
) {}

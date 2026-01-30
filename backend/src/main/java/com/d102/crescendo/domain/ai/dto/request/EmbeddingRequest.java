package com.d102.crescendo.domain.ai.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class EmbeddingRequest {
    private List<Integer> sheetIds;
}
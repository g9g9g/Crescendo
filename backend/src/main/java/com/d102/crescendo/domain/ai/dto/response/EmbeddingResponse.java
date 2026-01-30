package com.d102.crescendo.domain.ai.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class EmbeddingResponse {
    private Integer embeddingDim;
    private float[] embedding;
}

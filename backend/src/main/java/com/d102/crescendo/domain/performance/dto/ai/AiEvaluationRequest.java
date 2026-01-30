package com.d102.crescendo.domain.performance.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEvaluationRequest {

    @JsonProperty("audio_url")
    private String audioUrl;
}
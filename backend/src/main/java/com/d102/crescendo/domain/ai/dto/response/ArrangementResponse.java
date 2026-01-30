package com.d102.crescendo.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ArrangementResponse {
    @JsonProperty("s3_url")
    private String s3Url;
}
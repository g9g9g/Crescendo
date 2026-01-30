package com.d102.crescendo.domain.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ArrangementRequest {
    private String style;

    @JsonProperty("s3_url")
    private String s3Url;
}
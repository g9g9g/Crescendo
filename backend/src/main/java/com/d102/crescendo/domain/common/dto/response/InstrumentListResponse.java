package com.d102.crescendo.domain.common.dto.response;

import com.d102.crescendo.domain.sheet.dto.response.InstrumentResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InstrumentListResponse {
    private List<InstrumentResponse> instruments;
}
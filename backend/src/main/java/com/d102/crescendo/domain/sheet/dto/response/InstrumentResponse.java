package com.d102.crescendo.domain.sheet.dto.response;

import com.d102.crescendo.domain.sheet.entity.Instrument;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class InstrumentResponse {

    private Integer id;
    private String engName;
    private String korName;

    // 영어 이름 → 한글 이름 매핑
    private static final Map<String, String> KOR_NAME_MAP = Map.ofEntries(
            Map.entry("piano", "피아노"),
            Map.entry("guitar", "기타")
    );

    public static InstrumentResponse from(Instrument instrument) {
        return InstrumentResponse.builder()
                .id(instrument.getInstrumentId())
                .engName(instrument.getName())
                .korName(KOR_NAME_MAP.getOrDefault(instrument.getName(), ""))
                .build();
    }
}
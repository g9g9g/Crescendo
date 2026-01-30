package com.d102.crescendo.domain.sheet.dto.response;


import lombok.Builder;
import lombok.Getter;
import lombok.val;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserSheetCreateResponse {
    private Integer sheetId;
    private Integer userSheetId;
    private LocalDateTime createdAt;


}

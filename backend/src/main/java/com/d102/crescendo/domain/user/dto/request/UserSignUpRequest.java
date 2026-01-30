package com.d102.crescendo.domain.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSignUpRequest {
    private List<Integer> favoriteGenreIds;
    private Integer startInstrumentId;
    private List<Integer> sheetIds;
}
package com.d102.crescendo.domain.rank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankResponse {

    private List<RankUserInfo> topUsers;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankUserInfo {
        private Integer rank;
        private String nickname;
        private String profileUrl;
        private String tierCode;
        private Short tierLevel;
        private Integer exp;
    }
}
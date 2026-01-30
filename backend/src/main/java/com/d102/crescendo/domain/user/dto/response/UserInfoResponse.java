package com.d102.crescendo.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Integer userId;
    private String nickname;
    private String email;
    private String profileUrl;
    private List<Integer> favoriteGenreIds;
    private Integer totalPracticeTime;
    private List<InstrumentTierInfo> instrumentTiers;
    private Integer completedCount;
    private List<CompletionInfo> completions;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstrumentTierInfo {
        private Integer instrumentId;
        private String tierCode;
        private Short tierLevel;
        private Integer exp;
        private Integer expToNext;
        private Integer practiceTime;
        private Integer rank;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletionInfo {
        private String tierCode;
        private Short tierLevel;
    }
}

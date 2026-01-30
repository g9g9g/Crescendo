package com.d102.crescendo.domain.recommendation.dto;

import java.time.LocalDateTime;

/**
 * 사용자의 악보 연주 활동 기록
 */
public record UserPlayRecord(
        Integer sheetId,
        Integer practiceTime,          // 총 연습 시간 (초)
        Short progressRate,            // 진도율 (0-100)
        LocalDateTime lastAccessedAt   // 마지막 접속 시간
) {
    /**
     * 완주 여부 확인
     */
    public boolean isCompleted() {
        return progressRate != null && progressRate == 100;
    }

    /**
     * 최근 N일 이내 활동인지 확인
     */
    public boolean isRecentActivity(int days) {
        if (lastAccessedAt == null) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return lastAccessedAt.isAfter(threshold);
    }
}
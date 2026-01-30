package com.d102.crescendo.domain.rank.scheduler;

import com.d102.crescendo.domain.rank.service.RankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 랭킹 스케줄러
 * 매일 자정에 일일 랭킹을 계산하고 DB에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankScheduler {

    private final RankService rankService;

    @PostConstruct
    public void init() {
        log.info("=== RankScheduler Bean Initialized ===");
        log.info("Scheduler will run daily at 00:00:00 (Asia/Seoul timezone)");
    }

    /**
     * 매일 자정(00:00)에 실행
     * cron: 초 분 시 일 월 요일
     * "0 0 0 * * *" = 매일 00시 00분 00초
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void calculateDailyRankings() {
        log.info("=== Daily Ranking Calculation Job Started ===");
        try {
            rankService.calculateAndSaveDailyRankings();
            log.info("=== Daily Ranking Calculation Job Completed Successfully ===");
        } catch (Exception e) {
            log.error("=== Daily Ranking Calculation Job Failed ===", e);
        }
    }
}
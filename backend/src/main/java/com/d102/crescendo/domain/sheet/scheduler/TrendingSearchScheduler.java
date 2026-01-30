package com.d102.crescendo.domain.sheet.scheduler;

import com.d102.crescendo.domain.sheet.service.TrendingSearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 인기 검색어 집계 스케줄러
 * 매 1분마다 실행하여 최근 검색어를 집계하고 Redis에 캐싱
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrendingSearchScheduler {

    private final TrendingSearchService trendingSearchService;

    @PostConstruct
    public void init() {
        log.info("=== TrendingSearchScheduler Bean Initialized ===");
        log.info("Scheduler will run every 1 minute (fixedRate = 60000ms)");
    }

    /**
     * 매 1분마다 실행
     * fixedRate: 이전 실행 시작 시점부터 1분 후에 다음 실행
     */
    @Scheduled(fixedRate = 60000)
    public void aggregateTrendingSearches() {
        log.info("=== Trending Search Aggregation Job Started ===");
        try {
            trendingSearchService.aggregateAndCacheTrendingSearches();
            log.info("=== Trending Search Aggregation Job Completed Successfully ===");
        } catch (Exception e) {
            log.error("=== Trending Search Aggregation Job Failed ===", e);
        }
    }
}

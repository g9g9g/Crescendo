package com.d102.crescendo.domain.recommendation.config;

import com.d102.crescendo.domain.recommendation.service.TodaySheetBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class TodaySheetBatchConfig {

    private final TodaySheetBatchService todaySheetBatchService;

    // 매일 새벽 4시 (한국 기준)
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void scheduleDailyRecommendation() {
        todaySheetBatchService.runDailyRecommendation();
    }
}

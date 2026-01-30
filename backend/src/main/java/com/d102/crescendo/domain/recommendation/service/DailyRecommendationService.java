package com.d102.crescendo.domain.recommendation.service;

import com.d102.crescendo.domain.recommendation.dto.RecommendationCandidate;
import com.d102.crescendo.domain.recommendation.dto.TodaySheetItemResponse;
import com.d102.crescendo.domain.recommendation.dto.TodaySheetListResponse;
import com.d102.crescendo.domain.recommendation.entity.DailyRecommendation;
import com.d102.crescendo.domain.recommendation.repository.DailyRecommendationRepository;
import com.d102.crescendo.domain.sheet.dto.response.ServiceSheetDetailResponse;
import com.d102.crescendo.domain.sheet.service.ServiceSheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRecommendationService {

    private final DailyRecommendationRepository dailyRecommendationRepository;
    private final ServiceSheetService serviceSheetService;
    private final TodaySheetBatchService todaySheetBatchService;

    @Transactional(readOnly = true)
    public TodaySheetListResponse getTodaySheets(Integer userId) {
        LocalDate today = LocalDate.now();
        final int SUMMER_SHEET_ID = 93;
        final int TOTAL_SHEET_COUNT = 11;

        List<DailyRecommendation> recs =
                dailyRecommendationRepository.findByUserIdAndRecDateOrderByRankAsc(userId, today);

        if (recs.isEmpty()) {
            // ⭐ 실시간 추천 계산 (회원가입 직후, 배치가 아직 안 돌았을 때)
            log.info("사용자 {}의 오늘 추천 데이터 없음 - 실시간 계산 시작", userId);
            return calculateRecommendationsOnTheFly(userId);
        }

        // Summer 악보 정보 조회
        ServiceSheetDetailResponse summerSheet = serviceSheetService.getServiceSheet(SUMMER_SHEET_ID);
        TodaySheetItemResponse summerItem = new TodaySheetItemResponse(
                SUMMER_SHEET_ID,
                summerSheet.getTitle(),
                summerSheet.getComposer(),
                summerSheet.getGenreId(),
                summerSheet.getTierCode(),
                summerSheet.getTierLevel(),
                summerSheet.getDownloadNumber(),
                summerSheet.getThumbnailUrl(),
                summerSheet.getInstrumentId()
        );

        // 나머지 추천 악보들 (Summer 제외)
        List<TodaySheetItemResponse> otherItems = recs.stream()
                .filter(rec -> rec.getSheetId() != SUMMER_SHEET_ID)
                .limit(TOTAL_SHEET_COUNT - 1) // Summer 1개 + 나머지 10개 = 총 11개
                .map(rec -> {
                    ServiceSheetDetailResponse sheet = serviceSheetService.getServiceSheet(rec.getSheetId());

                    return new TodaySheetItemResponse(
                            rec.getSheetId(),
                            sheet.getTitle(),
                            sheet.getComposer(),
                            sheet.getGenreId(),
                            sheet.getTierCode(),
                            sheet.getTierLevel(),
                            sheet.getDownloadNumber(),
                            sheet.getThumbnailUrl(),
                            sheet.getInstrumentId()
                    );
                })
                .toList();

        // Summer를 맨 앞에 추가
        List<TodaySheetItemResponse> items = new java.util.ArrayList<>();
        items.add(summerItem);
        items.addAll(otherItems);

        return new TodaySheetListResponse(items);
    }

    /**
     * 실시간 추천 계산
     * - 배치가 아직 실행되지 않은 신규 사용자를 위한 즉시 추천
     * - DB에 저장하지 않고 바로 반환 (다음날 배치에서 정식 저장됨)
     */
    private TodaySheetListResponse calculateRecommendationsOnTheFly(Integer userId) {
        final int SUMMER_SHEET_ID = 93;
        final int TOTAL_SHEET_COUNT = 11;

        try {
            // 1. 배치 로직을 재사용하여 추천 계산
            List<RecommendationCandidate> candidates = todaySheetBatchService.calculateRecommendationsForUser(userId);

            if (candidates.isEmpty()) {
                log.info("사용자 {}에 대한 실시간 추천 결과 없음", userId);
                return new TodaySheetListResponse(List.of());
            }

            // Summer 악보 정보 조회
            ServiceSheetDetailResponse summerSheet = serviceSheetService.getServiceSheet(SUMMER_SHEET_ID);
            TodaySheetItemResponse summerItem = new TodaySheetItemResponse(
                    SUMMER_SHEET_ID,
                    summerSheet.getTitle(),
                    summerSheet.getComposer(),
                    summerSheet.getGenreId(),
                    summerSheet.getTierCode(),
                    summerSheet.getTierLevel(),
                    summerSheet.getDownloadNumber(),
                    summerSheet.getThumbnailUrl(),
                    summerSheet.getInstrumentId()
            );

            // 2. RecommendationCandidate → TodaySheetItemResponse 변환 (Summer 제외)
            List<TodaySheetItemResponse> otherItems = candidates.stream()
                    .filter(candidate -> candidate.sheetId() != SUMMER_SHEET_ID)
                    .limit(TOTAL_SHEET_COUNT - 1) // 총 11개
                    .map(candidate -> {
                        ServiceSheetDetailResponse sheet = serviceSheetService.getServiceSheet(candidate.sheetId());

                        return new TodaySheetItemResponse(
                                candidate.sheetId(),
                                sheet.getTitle(),
                                sheet.getComposer(),
                                sheet.getGenreId(),
                                sheet.getTierCode(),
                                sheet.getTierLevel(),
                                sheet.getDownloadNumber(),
                                sheet.getThumbnailUrl(),
                                sheet.getInstrumentId()
                        );
                    })
                    .toList();

            // Summer를 맨 앞에 추가
            List<TodaySheetItemResponse> items = new java.util.ArrayList<>();
            items.add(summerItem);
            items.addAll(otherItems);

            log.info("사용자 {}에 대한 실시간 추천 완료: {}건", userId, items.size());
            return new TodaySheetListResponse(items);

        } catch (Exception e) {
            log.error("사용자 {}의 실시간 추천 계산 실패", userId, e);
            return new TodaySheetListResponse(List.of());
        }
    }
}

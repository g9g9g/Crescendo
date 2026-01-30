package com.d102.crescendo.domain.recommendation.service;

import com.d102.crescendo.domain.recommendation.dto.RecommendationCandidate;
import com.d102.crescendo.domain.recommendation.dto.UserPlayRecord;
import com.d102.crescendo.domain.recommendation.entity.DailyRecommendation;
import com.d102.crescendo.domain.recommendation.repository.DailyRecommendationRepository;
import com.d102.crescendo.domain.recommendation.repository.RecommendationRepositoryCustom;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import com.d102.crescendo.domain.sheet.entity.UserSheet;
import com.d102.crescendo.domain.user.entity.User;
import com.d102.crescendo.domain.user.repository.UserRepository;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 하이브리드 벡터 기반 콘텐츠 필터링 추천 배치 서비스
 *
 * 벡터 구성: 난이도(6) + 장르(N) + 음표임베딩(1024)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodaySheetBatchService {

    private final JdbcTemplate jdbcTemplate;
    private final DailyRecommendationRepository dailyRecommendationRepository;
    private final RecommendationRepositoryCustom recommendationRepository;
    private final UserRepository userRepository;
    private final VectorGenerationService vectorService;
    private final NormalizationService normalizationService;

    /**
     * 매일 새벽에 실행되는 배치: 하이브리드 벡터 기반으로 오늘의 악보 추천을 계산해서 저장
     */
    @Transactional
    public void runDailyRecommendation() {
        LocalDate today = LocalDate.now();

        log.info("=== 오늘의 추천 악보 배치 시작 ({}) ===", today);

        // 0) Min-Max 정규화 캐시 갱신
        log.info("난이도 지표 Min-Max 캐시 갱신 시작");
        normalizationService.refreshMinMaxCache();

        // 1) 오늘 것만 저장하는 전략 → 테이블 싹 비우기
        log.info("기존 추천 데이터 삭제");
        jdbcTemplate.update("TRUNCATE TABLE daily_recommendation");

        // 2) 모든 유저 조회
        List<Integer> userIds = userRepository.findAllUserIds();
        log.info("활성 사용자 {}명 발견", userIds.size());

        // 3) 각 유저별로 TOP 10 악보 추천 계산 & 저장
        int successCount = 0;
        int failCount = 0;

        for (Integer userId : userIds) {
            try {
                List<RecommendationCandidate> candidates = calculateRecommendationsForUser(userId);

                if (!candidates.isEmpty()) {
                    saveDailyRecommendations(userId, today, candidates);
                    successCount++;
                } else {
                    log.warn("사용자 {}에 대한 추천 결과 없음 (활동 기록 부족 또는 후보 악보 없음)", userId);
                }
            } catch (Exception e) {
                log.error("사용자 {} 추천 계산 실패", userId, e);
                failCount++;
            }
        }

        log.info("=== 오늘의 추천 악보 배치 완료 ===");
        log.info("성공: {}명, 실패: {}명", successCount, failCount);
    }

    /**
     * 특정 사용자에 대한 추천 계산 (하이브리드 벡터 기반)
     * 실시간 추천 생성에도 사용됨
     */
    public List<RecommendationCandidate> calculateRecommendationsForUser(Integer userId) {
        // 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));

        // 2. 사용자 연주 기록 조회
        List<UserPlayRecord> plays = recommendationRepository.findUserPlayRecords(userId);

        // 3. 내 악보 조회
        List<UserSheet> librarySheets = user.getUserSheets().stream()
                .filter(us -> !us.isDeletedYes())
                .toList();

        // 4. 최소 데이터 체크
        boolean hasActivities = !plays.isEmpty();
        boolean hasLibrary = !librarySheets.isEmpty();
        boolean hasGenres = user.getUserGenres() != null && !user.getUserGenres().isEmpty();

        // 4-1. 신규 사용자 처리 (연주 기록과 라이브러리는 없지만 선호 장르는 있는 경우)
        if (!hasActivities && !hasLibrary && hasGenres) {
            log.debug("사용자 {}는 신규 사용자 (연주기록, 라이브러리 없음, 선호장르만 있음) - 인기 악보 기반 추천", userId);
            return calculatePopularBasedRecommendations(userId);
        }

        // 4-2. 데이터가 전혀 없는 경우
        if (!hasActivities && !hasLibrary && !hasGenres) {
            log.debug("사용자 {}는 추천에 필요한 데이터가 전혀 없음 (연주기록, 라이브러리, 선호장르 모두 없음)", userId);
            return List.of();
        }

        // 5. 연주 기록 + 라이브러리 악보 엔티티 조회
        Set<Integer> allSheetIds = new HashSet<>();
        if (hasActivities) {
            allSheetIds.addAll(plays.stream()
                    .map(UserPlayRecord::sheetId)
                    .toList());
        }
        if (hasLibrary) {
            allSheetIds.addAll(librarySheets.stream()
                    .map(us -> us.getSheet().getSheetId())
                    .toList());
        }
        Map<Integer, SheetMusic> userSheets = recommendationRepository.findSheetsByIds(new ArrayList<>(allSheetIds));

        // 6. 사용자 프로필 벡터 생성 (하이브리드 벡터)
        double[] userVector = vectorService.generateFlexibleUserVector(
                plays,           // 연주 기록 (비어있을 수 있음)
                user.getUserGenres(), // 선호 장르 (비어있을 수 있음)
                librarySheets,        // 내 악보 (비어있을 수 있음)
                userSheets            // 악보 엔티티 맵
        );

        // 7. 추천 후보 악보 조회 (UserInstrumentTier 기반 악기 필터링 적용)
        List<SheetMusic> candidateSheets = recommendationRepository.findRecommendationCandidates(userId);

        // 7-1. 임베딩이 없는 악보 필터링 (하이브리드 벡터 생성에 필수)
        candidateSheets = candidateSheets.stream()
                .filter(sheet -> sheet.getEmbedding() != null)
                .toList();

        if (candidateSheets.isEmpty()) {
            log.debug("사용자 {}에 대한 추천 후보 악보 없음 (악기에 맞는 악보 없거나 모두 라이브러리에 존재 또는 임베딩 없음)", userId);
            return List.of();
        }

        // 8. 각 후보 악보에 대해 유사도 계산
        List<RecommendationCandidate> candidates = new ArrayList<>();

        for (SheetMusic sheet : candidateSheets) {
            try {
                // 아이템 벡터 생성 (하이브리드)
                double[] itemVector = vectorService.generateItemVector(sheet);

                // 코사인 유사도 계산
                double similarity = vectorService.calculateCosineSimilarity(userVector, itemVector);

                candidates.add(new RecommendationCandidate(sheet.getSheetId(), similarity));
            } catch (Exception e) {
                log.error("악보 {} 벡터 생성 실패", sheet.getSheetId(), e);
            }
        }

        // 9. 점수 기준 내림차순 정렬 후 상위 10개 반환
        return candidates.stream()
                .sorted()
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 신규 사용자를 위한 인기 악보 기반 추천
     * - 연주 기록과 라이브러리는 없지만 선호 장르는 있는 경우
     * - 선호 악기(UserInstrumentTier) + 선호 장르(UserGenre)에 맞는 악보 중
     * - 다운로드 수가 많은 순서로 TOP 10 추천
     */
    private List<RecommendationCandidate> calculatePopularBasedRecommendations(Integer userId) {
        // 1. 선호 악기 + 선호 장르 기반 인기 악보 조회 (다운로드 수 많은 순)
        List<SheetMusic> popularSheets = recommendationRepository.findPopularSheetsByUserPreferences(userId, 10);

        if (popularSheets.isEmpty()) {
            log.debug("사용자 {}에 대한 인기 악보 없음 (선호 악기/장르에 맞는 악보 없음)", userId);
            return List.of();
        }

        // 2. download_number를 정규화하여 점수로 사용 (0.0 ~ 1.0 범위)
        int maxDownloads = popularSheets.stream()
                .mapToInt(SheetMusic::getDownloadNumber)
                .max()
                .orElse(1);

        List<RecommendationCandidate> candidates = new ArrayList<>();
        for (SheetMusic sheet : popularSheets) {
            // 다운로드 수를 0.0 ~ 1.0 범위로 정규화하여 점수로 사용
            double score = maxDownloads > 0 ? (double) sheet.getDownloadNumber() / maxDownloads : 0.0;
            candidates.add(new RecommendationCandidate(sheet.getSheetId(), score));
        }

        log.debug("사용자 {}에 대한 인기 악보 기반 추천 완료: {}건", userId, candidates.size());
        return candidates;
    }

    /**
     * 추천 결과를 DailyRecommendation 테이블에 저장
     */
    public void saveDailyRecommendations(Integer userId, LocalDate date,
                                          List<RecommendationCandidate> candidates) {
        int rank = 1;
        for (RecommendationCandidate c : candidates) {
            DailyRecommendation rec = DailyRecommendation.builder()
                    .userId(userId)
                    .recDate(date)
                    .sheetId(c.sheetId())
                    .rank(rank)
                    .score(c.score())
                    .build();

            dailyRecommendationRepository.save(rec);
            rank++;
        }
    }

    /**
     * 특정 사용자의 오늘의 추천을 재계산
     * ADMIN이 악보를 삭제했을 때 해당 악보를 추천받은 사용자들의 추천을 다시 생성
     */
    @Transactional
    public void recalculateRecommendationForUser(Integer userId) {
        LocalDate today = LocalDate.now();

        log.info("사용자 {} 오늘의 추천 재계산 시작", userId);

        try {
            // 1. 기존 추천 삭제
            dailyRecommendationRepository.deleteByUserIdAndRecDate(userId, today);

            // 2. 추천 재계산
            List<RecommendationCandidate> candidates = calculateRecommendationsForUser(userId);

            // 3. 새로운 추천 저장
            if (!candidates.isEmpty()) {
                saveDailyRecommendations(userId, today, candidates);
                log.info("사용자 {} 오늘의 추천 재계산 완료 - {}건", userId, candidates.size());
            } else {
                log.warn("사용자 {}에 대한 추천 결과 없음", userId);
            }
        } catch (Exception e) {
            log.error("사용자 {} 추천 재계산 실패", userId, e);
        }
    }
}

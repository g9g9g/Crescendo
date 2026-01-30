package com.d102.crescendo.domain.performance.service;

import com.d102.crescendo.domain.performance.dto.request.PlayResultRequest;
import com.d102.crescendo.domain.performance.dto.response.PlayEndResponse;
import com.d102.crescendo.domain.performance.dto.response.PlayEvaluationResponse;
import com.d102.crescendo.domain.performance.dto.response.RecentPerformanceResponse;
import com.d102.crescendo.domain.performance.entity.Performance;
import com.d102.crescendo.domain.performance.entity.PerformanceEvaluation;
import com.d102.crescendo.domain.performance.repository.PerformanceEvaluationRepository;
import com.d102.crescendo.domain.performance.repository.PerformanceRepository;
import com.d102.crescendo.domain.sheet.document.UserSheetDocument;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import com.d102.crescendo.domain.sheet.entity.UserSheet;
import com.d102.crescendo.domain.sheet.repository.UserSheetDocumentRepository;
import com.d102.crescendo.domain.sheet.repository.UserSheetRepository;
import com.d102.crescendo.domain.user.entity.User;
import com.d102.crescendo.domain.user.entity.UserInstrumentTier;
import com.d102.crescendo.domain.user.entity.UserInstrumentTierId;
import com.d102.crescendo.domain.user.repository.UserInstrumentTierRepository;
import com.d102.crescendo.domain.common.entity.Tier;
import com.d102.crescendo.domain.common.repository.TierRepository;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final UserSheetRepository userSheetRepository;
    private final UserSheetDocumentRepository userSheetDocumentRepository;
    private final PerformanceRepository performanceRepository;
    private final PerformanceEvaluationRepository performanceEvaluationRepository;
    private final PerformanceAsyncService performanceAsyncService;
    private final UserInstrumentTierRepository userInstrumentTierRepository;
    private final TierRepository tierRepository;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public RecentPerformanceResponse getRecentPerformances(Integer userId) {
        List<UserSheet> userSheets = userSheetRepository.findRecentUserSheetsByUserId(userId);

        List<RecentPerformanceResponse.PerformanceRecord> records = userSheets.stream()
                .map(userSheet -> RecentPerformanceResponse.PerformanceRecord.builder()
                        .userSheetId(userSheet.getUserSheetId())
                        .title(userSheet.getSheet().getTitle())
                        .composer(userSheet.getSheet().getComposer())
                        .genreId(userSheet.getSheet().getGenre().getGenreId())
                        .tierCode(userSheet.getSheet().getTier().getTierCode())
                        .tierLevel(userSheet.getSheet().getTier().getTierLevel())
                        .progressRate(userSheet.getProgressRate())
                        .thumbnailUrl(userSheet.getSheet().getThumbnailUrl())
                        .lastAccessedAt(userSheet.getLastAccessedAt().format(DATE_TIME_FORMATTER))
                        .instrumentId(userSheet.getSheet().getInstrument().getInstrumentId())
                        .xmlUrl(userSheet.getSheet().getXmlUrl())
                        .startMeasure(userSheet.getEndMeasure())
                        .build())
                .collect(Collectors.toList());

        return RecentPerformanceResponse.builder()
                .totalCount(records.size())
                .records(records)
                .build();
    }

    @Transactional
    public PlayEndResponse savePlayResult(Integer userId, PlayResultRequest request) {
        // 1. UserSheet 조회
        UserSheet userSheet = userSheetRepository.findUserSheetDetailById(request.getUserSheetId())
                .orElseThrow(() -> new BusinessException(BusinessError.SHEET_NOT_FOUND));

        // 2. 권한 확인 (해당 악보의 소유자인지)
        if (!userSheet.getUser().getUserId().equals(userId)) {
            throw new BusinessException(BusinessError.FORBIDDEN);
        }

        // 3. Performance 엔티티 생성 및 저장
        LocalDateTime endedAt = LocalDateTime.now();

        Performance performance = Performance.builder()
                .userSheet(userSheet)
                .startedAt(request.getStartedAt())
                .endedAt(endedAt)
                .startMeasure(request.getStartMeasure())
                .endMeasure(request.getEndMeasure())
                .practiceMode(request.getPracticeMode())
                .wavXmlUrl(request.getWavXmlUrl())
                .build();

        // 4. Performance 저장
        performance = performanceRepository.save(performance);

        // 5. UserSheet의 lastAccessedAt, practiceTime 업데이트
        userSheet.updateLastAccessedAt(endedAt);
        Duration playDuration = Duration.between(request.getStartedAt(), endedAt);
        int playTimeInSeconds = (int) playDuration.getSeconds();
        userSheet.updatePracticeTime(playTimeInSeconds);

        // UserInstrumentTier의 practiceTime 업데이트 (없으면 생성)
        User user =  userSheet.getUser();
        Integer instrumentId = userSheet.getSheet().getInstrument().getInstrumentId();
        UserInstrumentTier userInstrumentTier = userInstrumentTierRepository
                .findByUserIdAndInstrumentId(userId, instrumentId)
                .orElseGet(() -> {
                    // UserInstrumentTier가 없으면 새로 생성 (초기 티어: tierId=1)
                    Tier initialTier = tierRepository.findByTierId(1)
                            .orElseThrow(() -> new BusinessException(BusinessError.TIER_NOT_FOUND));

                    UserInstrumentTier newUserInstrumentTier = UserInstrumentTier.builder()
                            .id(new UserInstrumentTierId(userId, instrumentId))
                            .user(user)
                            .instrument(userSheet.getSheet().getInstrument())
                            .tier(initialTier)
                            .exp(0)
                            .practiceTime(0)
                            .build();

                    return userInstrumentTierRepository.save(newUserInstrumentTier);
                });

        userInstrumentTier.updatePracticeTime(playTimeInSeconds);

        // User의 totalPracticeTime 업데이트
        user.updateTotalPracticeTime(playTimeInSeconds);

        // 6. UserSheet의 진행도 업데이트 (endMeasure와 progressRate)
        // 만약 userSheet.endMeasure < performance.endMeasure 이라면 userSheet 값을 업데이트
        Short currentEndMeasure = userSheet.getEndMeasure();
        Short performanceEndMeasure = performance.getEndMeasure();

        if (currentEndMeasure == null || currentEndMeasure < performanceEndMeasure) {
            Short maxMeasureCnt = userSheet.getSheet().getMaxMeasureCnt();
            Short progressRate = 0;

            // progressRate 계산: (endMeasure / maxMeasureCnt) * 100
            if (maxMeasureCnt != null && maxMeasureCnt > 0) {
                progressRate = (short) ((performanceEndMeasure * 100) / maxMeasureCnt);
                // 100을 초과하지 않도록 제한
                if (progressRate > 100) {
                    progressRate = 100;
                }
            }

            userSheet.updateProgress(performanceEndMeasure, progressRate);

            // 진도율 업데이트 후 Elasticsearch 동기화
            syncToElasticsearch(userSheet);
            log.info("진도율 업데이트 및 Elasticsearch 동기화 완료 - userSheetId: {}, progressRate: {}",
                    userSheet.getUserSheetId(), progressRate);
        }

        // 7. 평가 모드인 경우 비동기로 AI 서버에 평가 요청 (진도율이 100%일 때만)
        if (!request.getPracticeMode() && request.getWavXmlUrl() != null && userSheet.getProgressRate() == 100) {
            // DTO 생성 (비동기 메서드에 필요한 데이터 추출)
            PerformanceAsyncService.PerformanceData performanceData =
                    new PerformanceAsyncService.PerformanceData(
                            performance.getPlayId(),
                            userId,
                            userSheet.getUserSheetId(),
                            userSheet.getSheet().getTitle()
                    );
            performanceAsyncService.evaluatePerformanceAsync(performanceData, request.getWavXmlUrl());
        }

        // 8. 응답 반환
        return PlayEndResponse.builder()
                .playId(performance.getPlayId())
                .build();
    }

    public PlayEvaluationResponse getPlayEvaluation(Integer userId, Integer playId) {
        // Performance 조회 (UserSheet와 함께)
        Performance performance = performanceRepository.findByIdWithUserSheet(playId)
                .orElseThrow(() -> new BusinessException(BusinessError.PERFORMANCE_NOT_FOUND));

        // 권한 확인: 해당 연주 기록의 소유자인지 확인
        Integer ownerId = performance.getUserSheet().getUser().getUserId();
        if (!ownerId.equals(userId)) {
            throw new BusinessException(BusinessError.PERFORMANCE_ACCESS_DENIED);
        }

        // 연습 모드이거나 녹음 파일이 없는 경우 NOT_AVAILABLE 반환
        if (performance.isPracticeMode() || performance.getWavXmlUrl() == null) {
            return PlayEvaluationResponse.builder()
                    .status("NOT_AVAILABLE")
                    .totalScore(null)
                    .grade(null)
                    .comment(null)
                    .metrics(null)
                    .build();
        }

        // 평가 모드인 경우 PerformanceEvaluation 조회
        Optional<PerformanceEvaluation> evaluationOpt = performanceEvaluationRepository.findByPlayId(playId);

        if (evaluationOpt.isPresent()) {
            // 평가 완료: 결과 데이터 반환
            PerformanceEvaluation evaluation = evaluationOpt.get();

            // 9개 평가 지표를 리스트로 구성
            List<PlayEvaluationResponse.MetricDetail> metrics = new ArrayList<>();
            metrics.add(PlayEvaluationResponse.MetricDetail.builder()
                    .code("tempo_stability")
                    .name("템포 안정성")
                    .score(evaluation.getTempoStability())
                    .build());
            metrics.add(PlayEvaluationResponse.MetricDetail.builder()
                    .code("rhythm_consistency")
                    .name("리듬 일관성")
                    .score(evaluation.getRhythmConsistency())
                    .build());
            metrics.add(PlayEvaluationResponse.MetricDetail.builder()
                    .code("dynamics_expression")
                    .name("다이내믹 표현력")
                    .score(evaluation.getDynamicsExpression())
                    .build());
            metrics.add(PlayEvaluationResponse.MetricDetail.builder()
                    .code("articulation_balance")
                    .name("아티큘레이션 균형")
                    .score(evaluation.getArticulationBalance())
                    .build());
            metrics.add(PlayEvaluationResponse.MetricDetail.builder()
                    .code("clean_technique")
                    .name("깔끔한 연주 기술")
                    .score(evaluation.getCleanTechnique())
                    .build());
            metrics.add(PlayEvaluationResponse.MetricDetail.builder()
                    .code("pitch_diversity")
                    .name("음역대 다양성")
                    .score(evaluation.getPitchDiversity())
                    .build());
            metrics.add(PlayEvaluationResponse.MetricDetail.builder()
                    .code("polyphony_control")
                    .name("화음 제어")
                    .score(evaluation.getPolyphonyControl())
                    .build());
            metrics.add(PlayEvaluationResponse.MetricDetail.builder()
                    .code("phrase_variety")
                    .name("프레이즈 다양성")
                    .score(evaluation.getPhraseVariety())
                    .build());
            metrics.add(PlayEvaluationResponse.MetricDetail.builder()
                    .code("pacing_balance")
                    .name("페이싱 균형")
                    .score(evaluation.getPacingBalance())
                    .build());

            return PlayEvaluationResponse.builder()
                    .status("COMPLETED")
                    .totalScore(evaluation.getScore())
                    .grade(evaluation.getGrade())
                    .comment(evaluation.getComment())
                    .metrics(metrics)
                    .build();
        } else {
            // 평가 결과가 없는 경우: 평가 중
            return PlayEvaluationResponse.builder()
                    .status("EVALUATION")
                    .totalScore(null)
                    .grade(null)
                    .comment(null)
                    .metrics(null)
                    .build();
        }
    }

    /**
     * UserSheet를 Elasticsearch에 동기화
     * @param userSheet UserSheet 엔티티
     */
    private void syncToElasticsearch(UserSheet userSheet) {
        try {
            SheetMusic sheet = userSheet.getSheet();

            UserSheetDocument document = UserSheetDocument.builder()
                    .userSheetId(userSheet.getUserSheetId())
                    .userId(userSheet.getUser().getUserId())
                    .sheetId(sheet.getSheetId())
                    .title(sheet.getTitle())
                    .composer(sheet.getComposer())
                    .instrumentId(sheet.getInstrument() != null ? sheet.getInstrument().getInstrumentId() : null)
                    .genreId(sheet.getGenre() != null ? sheet.getGenre().getGenreId() : null)
                    .tierCode(sheet.getTier() != null ? sheet.getTier().getTierCode() : null)
                    .tierLevel(sheet.getTier() != null ? Integer.valueOf(sheet.getTier().getTierLevel()) : null)
                    .progressRate(userSheet.getProgressRate())
                    .thumbnailUrl(sheet.getThumbnailUrl())
                    .lastAccessedAt(userSheet.getLastAccessedAt())
                    .deletedYes(userSheet.isDeletedYes())
                    .genre(sheet.getGenre() != null ? sheet.getGenre().getName() : null)
                    .instrument(sheet.getInstrument() != null ? sheet.getInstrument().getName() : null)
                    .build();

            userSheetDocumentRepository.save(document);
            log.info("Elasticsearch 동기화 완료 - userSheetId: {}, progressRate: {}",
                    userSheet.getUserSheetId(), userSheet.getProgressRate());
        } catch (Exception e) {
            log.error("Elasticsearch 동기화 실패 - userSheetId: {}, error: {}",
                    userSheet.getUserSheetId(), e.getMessage(), e);
            // Elasticsearch 동기화 실패가 전체 트랜잭션을 롤백하지 않도록 예외를 삼킴
        }
    }
}
package com.d102.crescendo.domain.performance.service;

import com.d102.crescendo.domain.common.service.ExpService;
import com.d102.crescendo.domain.fcm.entity.NotificationType;
import com.d102.crescendo.domain.fcm.service.FcmService;
import com.d102.crescendo.domain.performance.client.AiEvaluationClient;
import com.d102.crescendo.domain.performance.dto.ai.AiEvaluationResponse;
import com.d102.crescendo.domain.performance.entity.Performance;
import com.d102.crescendo.domain.performance.entity.PerformanceEvaluation;
import com.d102.crescendo.domain.performance.repository.PerformanceEvaluationRepository;
import com.d102.crescendo.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 연주 관련 비동기 작업을 처리하는 서비스
 * @Async 어노테이션이 제대로 동작하도록 별도 클래스로 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceAsyncService {

    private final PerformanceRepository performanceRepository;
    private final PerformanceEvaluationRepository performanceEvaluationRepository;
    private final AiEvaluationClient aiEvaluationClient;
    private final FcmService fcmService;
    private final ExpService expService;

    /**
     * Performance 데이터를 담는 DTO
     */
    public record PerformanceData(
            Integer playId,
            Integer userId,
            Integer userSheetId,
            String sheetTitle
    ) {}

    /**
     * 비동기로 AI 연주 평가 수행 및 결과 저장
     * @param performanceData 연주 데이터
     * @param wavXmlUrl 연주 음원 URL
     */
    @Async
    public void evaluatePerformanceAsync(PerformanceData performanceData, String wavXmlUrl) {
        try {
            log.info("AI 평가 시작 - playId: {}, wavXmlUrl: {}", performanceData.playId(), wavXmlUrl);

            // AI 서버에 연주 평가 요청
            AiEvaluationResponse aiResponse = aiEvaluationClient.evaluatePerformance(wavXmlUrl);

            // 성공 여부 확인
            if (aiResponse.getSuccess() == null || !aiResponse.getSuccess()) {
                log.error("AI 평가 실패 - playId: {}", performanceData.playId());
                return;
            }

            // metrics에서 각 지표 추출 (없으면 0으로 처리)
            Short tempoStability = extractScore(aiResponse.getMetrics(), "tempo_stability");
            Short rhythmConsistency = extractScore(aiResponse.getMetrics(), "rhythm_consistency");
            Short dynamicsExpression = extractScore(aiResponse.getMetrics(), "dynamics_expression");
            Short articulationBalance = extractScore(aiResponse.getMetrics(), "articulation_balance");
            Short cleanTechnique = extractScore(aiResponse.getMetrics(), "clean_technique");
            Short pitchDiversity = extractScore(aiResponse.getMetrics(), "pitch_diversity");
            Short polyphonyControl = extractScore(aiResponse.getMetrics(), "polyphony_control");
            Short phraseVariety = extractScore(aiResponse.getMetrics(), "phrase_variety");
            Short pacingBalance = extractScore(aiResponse.getMetrics(), "pacing_balance");

            // feedback 배열을 하나의 문자열로 결합
            String comment = aiResponse.getFeedback() != null ? String.join("\n", aiResponse.getFeedback()) : "";

            // overall_score를 Short로 변환
            Short score = aiResponse.getOverallScore() != null ? aiResponse.getOverallScore().shortValue() : 0;

            String grade = aiResponse.getGrade() != null ? aiResponse.getGrade() : "";

            // DB 저장 로직은 별도 트랜잭션 메서드로 분리
            saveEvaluationResult(performanceData.playId(), tempoStability, rhythmConsistency, dynamicsExpression,
                    articulationBalance, cleanTechnique, pitchDiversity, polyphonyControl,
                    phraseVariety, pacingBalance, comment, score, grade);

            log.info("AI 평가 완료 및 저장 - playId: {}, score: {}, bestScore updated", performanceData.playId(), score);

            // 경험치 지급 (진도율 100% + 시스템 악보 + 첫 완료 시)
            ExpService.TierUpgradeInfo tierUpgradeInfo = null;
            try {
                tierUpgradeInfo = expService.grantExpForCompletion(performanceData.userSheetId());
            } catch (Exception expException) {
                // 경험치 지급 실패는 전체 프로세스를 중단시키지 않음
                log.error("경험치 지급 중 오류 발생 - playId: {}, error: {}", performanceData.playId(), expException.getMessage(), expException);
            }

            // 티어 승급 FCM 알림 발송
            if (tierUpgradeInfo != null) {
                try {
                    // tierCode가 달라지면 "승격", 같으면 "승급"
                    String titleAction = tierUpgradeInfo.fromTierCode().equals(tierUpgradeInfo.toTierCode()) ? "승급" : "승격";
                    String title = "티어 " + titleAction + "!";

                    // 티어 코드를 한글로 변환
                    String fromTierKorean = convertTierCodeToKorean(tierUpgradeInfo.fromTierCode());
                    String toTierKorean = convertTierCodeToKorean(tierUpgradeInfo.toTierCode());

                    String body = String.format("[%s] %s %d에서 %s %d로 상승했어요!",
                            tierUpgradeInfo.instrumentName(),
                            fromTierKorean, tierUpgradeInfo.fromTierLevel(),
                            toTierKorean, tierUpgradeInfo.toTierLevel());

                    fcmService.sendNotification(tierUpgradeInfo.userId(), title, body, NotificationType.TIERUP);
                    log.info("티어 {} 알림 전송 완료 - userId: {}, instrument: {}, tier: {}{} -> {}{}",
                            titleAction,
                            tierUpgradeInfo.userId(),
                            tierUpgradeInfo.instrumentName(),
                            tierUpgradeInfo.fromTierCode(), tierUpgradeInfo.fromTierLevel(),
                            tierUpgradeInfo.toTierCode(), tierUpgradeInfo.toTierLevel());
                } catch (Exception e) {
                    log.error("티어 승급 알림 전송 실패 - userId: {}, error: {}",
                            tierUpgradeInfo.userId(), e.getMessage());
                    // 알림 전송 실패는 전체 프로세스를 중단하지 않음
                }
            }

            try {
                String title = "띵동🔔 " + performanceData.sheetTitle() + " 평가 도착";
                String body = String.format("이번 연주 점수는 %d점, %s 등급이에요!", score, grade);

                fcmService.sendNotification(performanceData.userId(), title, body, NotificationType.EVALUATION);
                log.info("FCM 푸시 알림 발송 완료 - userId: {}, playId: {}", performanceData.userId(), performanceData.playId());
            } catch (Exception fcmException) {
                // FCM 발송 실패는 전체 프로세스를 중단시키지 않음
                log.error("FCM 푸시 알림 발송 실패 - playId: {}, error: {}", performanceData.playId(), fcmException.getMessage());
            }
        } catch (Exception e) {
            log.error("AI 평가 중 오류 발생 - playId: {}", performanceData.playId(), e);
        }
    }

    /**
     * 평가 결과를 DB에 저장 (별도 트랜잭션)
     */
    @Transactional
    public void saveEvaluationResult(Integer playId, Short tempoStability, Short rhythmConsistency,
                                       Short dynamicsExpression, Short articulationBalance, Short cleanTechnique,
                                       Short pitchDiversity, Short polyphonyControl, Short phraseVariety,
                                       Short pacingBalance, String comment, Short score, String grade) {
        // Performance 조회 (UserSheet와 함께 fetch join)
        Performance performance = performanceRepository.findByIdWithUserSheet(playId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 연주 기록입니다."));

        // PerformanceEvaluation 엔티티 생성 및 저장
        PerformanceEvaluation evaluation = PerformanceEvaluation.builder()
                .performance(performance)
                .tempoStability(tempoStability)
                .rhythmConsistency(rhythmConsistency)
                .dynamicsExpression(dynamicsExpression)
                .articulationBalance(articulationBalance)
                .cleanTechnique(cleanTechnique)
                .pitchDiversity(pitchDiversity)
                .polyphonyControl(polyphonyControl)
                .phraseVariety(phraseVariety)
                .pacingBalance(pacingBalance)
                .comment(comment)
                .score(score)
                .grade(grade)
                .build();

        performanceEvaluationRepository.save(evaluation);

        // UserSheet의 bestScore 업데이트 (최고 점수만 갱신)
        performance.getUserSheet().updateBestScore(score);
    }

    /**
     * metrics Map에서 특정 키의 점수를 추출하여 Short로 변환
     */
    private Short extractScore(Map<String, AiEvaluationResponse.MetricDetail> metrics, String key) {
        if (metrics == null || !metrics.containsKey(key)) {
            return 0;
        }
        AiEvaluationResponse.MetricDetail detail = metrics.get(key);
        return detail != null && detail.getScore() != null ? detail.getScore().shortValue() : 0;
    }

    /**
     * 티어 코드를 한글로 변환
     */
    private String convertTierCodeToKorean(String tierCode) {
        return switch (tierCode.toLowerCase()) {
            case "bronze" -> "브론즈";
            case "silver" -> "실버";
            case "gold" -> "골드";
            case "platinum" -> "플래티넘";
            case "diamond" -> "다이아몬드";
            default -> tierCode; // 알 수 없는 티어는 그대로 반환
        };
    }
}

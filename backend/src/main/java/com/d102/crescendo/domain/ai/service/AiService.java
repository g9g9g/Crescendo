package com.d102.crescendo.domain.ai.service;

import com.d102.crescendo.domain.ai.dto.request.ArrangementRequest;
import com.d102.crescendo.domain.ai.dto.response.ArrangementResponse;
import com.d102.crescendo.domain.ai.dto.response.BatchDifficultyEvaluationResponse;
import com.d102.crescendo.domain.ai.dto.response.BatchEmbeddingResponse;
import com.d102.crescendo.domain.ai.dto.response.DifficultyEvaluationResponse;
import com.d102.crescendo.domain.ai.dto.response.EmbeddingResponse;
import com.d102.crescendo.domain.common.entity.Tier;
import com.d102.crescendo.domain.common.repository.TierRepository;
import com.d102.crescendo.domain.sheet.entity.SheetDifficultyMetric;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import com.d102.crescendo.domain.sheet.repository.SheetDifficultyMetricRepository;
import com.d102.crescendo.domain.sheet.repository.SheetMusicRepository;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiService {

    private final SheetMusicRepository sheetMusicRepository;
    private final TierRepository tierRepository;
    private final SheetDifficultyMetricRepository difficultyMetricRepository;
    private final ObjectMapper objectMapper;

    private final WebClient webClient;

    public AiService(SheetMusicRepository sheetMusicRepository,
                     TierRepository tierRepository,
                     SheetDifficultyMetricRepository difficultyMetricRepository,
                     ObjectMapper objectMapper) {
        this.sheetMusicRepository = sheetMusicRepository;
        this.tierRepository = tierRepository;
        this.difficultyMetricRepository = difficultyMetricRepository;
        this.objectMapper = objectMapper;

        // AI서버가 오래 걸려도 타임아웃 되지않게 설정
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(0))
                                .addHandlerLast(new WriteTimeoutHandler(0))
                );

        this.webClient = WebClient.builder()
                .baseUrl("https://cresd102.duckdns.org/ai")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public EmbeddingResponse requestEmbedding(Integer sheetId) {
        // 1. SheetMusic 조회
        SheetMusic sheet = sheetMusicRepository.findById(sheetId)
                .orElseThrow(() -> new BusinessException(BusinessError.SHEET_NOT_FOUND));

        // 2. AI 서버에 임베딩 요청
        EmbeddingResponse response;
        try {
            response = webClient.post()
                    .uri("/embedding")
                    .bodyValue(Map.of("s3_url", sheet.getXmlUrl()))
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block(); // 동기 호출 (필요시 비동기로 전환 가능)
        } catch (Exception e) {
            log.error("AI 서버 임베딩 요청 실패: {}", e.getMessage());
            throw new BusinessException(BusinessError.AI_SERVER_ERROR);
        }

        // 3. DB에 저장
        sheet.updateEmbedding(response.getEmbedding());
        sheetMusicRepository.save(sheet);

        return response;
    }

    /**
     * 비동기로 임베딩을 요청하고 DB에 저장
     * ADMIN이 악보를 등록할 때 사용
     */
    @Async
    public void requestEmbeddingAsync(Integer sheetId) {
        log.info("비동기 임베딩 요청 시작: sheetId={}", sheetId);
        try {
            requestEmbedding(sheetId);
            log.info("임베딩 완료: sheetId={}", sheetId);
        } catch (Exception e) {
            log.error("비동기 임베딩 요청 중 에러 발생: sheetId={}", sheetId, e);
            // 비동기이므로 예외를 던지지 않고 로그만 남김
        }
    }

    /**
     * 여러 악보에 대해 임베딩 생성 요청 후 DB에 저장
     * 각 악보별 독립적인 트랜잭션으로 처리하여 일부 실패해도 성공한 악보는 저장됨
     */
    public BatchEmbeddingResponse generateAndSaveEmbeddings(List<Integer> sheetIds) {
        log.info("배치 임베딩 생성 시작: sheetIds={}", sheetIds);

        List<EmbeddingResponse> responses = new ArrayList<>();
        List<Integer> successIds = new ArrayList<>();
        Map<Integer, String> failedIdsWithReasons = new HashMap<>();

        for (Integer sheetId : sheetIds) {
            try {
                // 각 악보별로 독립적인 트랜잭션으로 처리
                EmbeddingResponse response = generateEmbeddingSingleSheetWithTransaction(sheetId);
                responses.add(response);
                successIds.add(sheetId);
                log.info("악보 임베딩 생성 성공 - sheetId: {}", sheetId);

            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.error("임베딩 생성 처리 중 에러 발생 - sheetId: {}, error: {}", sheetId, errorMessage, e);
                failedIdsWithReasons.put(sheetId, errorMessage);
            }
        }

        log.info("배치 임베딩 생성 완료 - 성공: {}/{}", successIds.size(), sheetIds.size());

        return BatchEmbeddingResponse.builder()
                .successIds(successIds)
                .failedIds(failedIdsWithReasons)
                .results(responses)
                .totalCount(sheetIds.size())
                .successCount(successIds.size())
                .failedCount(failedIdsWithReasons.size())
                .build();
    }

    /**
     * 단일 악보에 대한 임베딩 생성 및 저장 (독립 트랜잭션)
     * REQUIRES_NEW: 새로운 트랜잭션을 생성하여 각 악보별 실패가 다른 악보에 영향 없도록 함
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmbeddingResponse generateEmbeddingSingleSheetWithTransaction(Integer sheetId) {
        // 1. SheetMusic 조회
        SheetMusic sheet = sheetMusicRepository.findById(sheetId)
                .orElseThrow(() -> new BusinessException(BusinessError.SHEET_NOT_FOUND));

        // 2. AI 서버에 임베딩 요청
        EmbeddingResponse response;
        try {
            response = webClient.post()
                    .uri("/embedding")
                    .bodyValue(Map.of("s3_url", sheet.getXmlUrl()))
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("AI 서버 임베딩 요청 실패 - sheetId: {}, xmlUrl: {}", sheetId, sheet.getXmlUrl(), e);
            throw new BusinessException(BusinessError.AI_SERVER_ERROR);
        }

        // 3. DB에 임베딩 저장
        sheet.updateEmbedding(response.getEmbedding());
        sheetMusicRepository.save(sheet);
        log.info("임베딩 저장 완료 - sheetId: {}", sheetId);

        return response;
    }

    /**
     * AI 서버에 악보 편곡 요청
     * @param style 편곡 스타일 (예: "재즈풍으로 변형해줘")
     * @param s3Url 원본 악보 S3 URL
     * @return AI 서버가 S3에 업로드한 편곡된 악보 URL
     */
    public ArrangementResponse requestArrangement(String style, String s3Url) {
        log.info("AI 서버에 편곡 요청 - style: {}, s3Url: {}", style, s3Url);

        try {
            ArrangementResponse response = webClient.post()
                    .uri("/arrangement")
                    .bodyValue(new ArrangementRequest(style, s3Url))
                    .retrieve()
                    .bodyToMono(ArrangementResponse.class)
                    .block();

            log.info("AI 서버 편곡 요청 완료 - 결과 s3Url: {}", response.getS3Url());
            return response;
        } catch (Exception e) {
            log.error("AI 서버 편곡 요청 실패: {}", e.getMessage(), e);
            throw new BusinessException(BusinessError.AI_SERVER_ERROR);
        }
    }

    /**
     * AI 서버에 난이도 평가 요청
     */
    public DifficultyEvaluationResponse requestDifficultyEvaluation(String s3Url) {
        log.info("난이도 평가 요청 시작: s3Url={}", s3Url);

        DifficultyEvaluationResponse response;
        try {
            response = webClient.post()
                    .uri("/difficulty/evaluation")
                    .bodyValue(Map.of("s3_url", s3Url))
                    .retrieve()
                    .bodyToMono(DifficultyEvaluationResponse.class)
                    .block(); // 동기 호출

            log.info("난이도 평가 응답 성공: level={}, summary={}",
                    response.getLevel(), response.getSummary());

        } catch (Exception e) {
            log.error("AI 서버 난이도 평가 요청 실패: {}", e.getMessage(), e);
            throw new BusinessException(BusinessError.AI_SERVER_ERROR);
        }

        return response;
    }

    /**
     * 여러 악보에 대해 난이도 평가 요청 후 DB에 저장
     * 각 악보별 독립적인 트랜잭션으로 처리하여 일부 실패해도 성공한 악보는 저장됨
     */
    public BatchDifficultyEvaluationResponse evaluateAndSaveDifficulties(List<Integer> sheetIds) {
        log.info("배치 난이도 평가 시작: sheetIds={}", sheetIds);

        List<DifficultyEvaluationResponse> responses = new ArrayList<>();
        List<Integer> successIds = new ArrayList<>();
        Map<Integer, String> failedIdsWithReasons = new HashMap<>();

        for (Integer sheetId : sheetIds) {
            try {
                // 각 악보별로 독립적인 트랜잭션으로 처리
                DifficultyEvaluationResponse response = evaluateSingleSheetWithTransaction(sheetId);
                responses.add(response);
                successIds.add(sheetId);
                log.info("악보 난이도 평가 성공 - sheetId: {}", sheetId);

            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.error("난이도 평가 처리 중 에러 발생 - sheetId: {}, error: {}", sheetId, errorMessage, e);
                failedIdsWithReasons.put(sheetId, errorMessage);
            }
        }

        log.info("배치 난이도 평가 완료 - 성공: {}/{}", successIds.size(), sheetIds.size());

        return BatchDifficultyEvaluationResponse.builder()
                .successIds(successIds)
                .failedIds(failedIdsWithReasons)
                .results(responses)
                .totalCount(sheetIds.size())
                .successCount(successIds.size())
                .failedCount(failedIdsWithReasons.size())
                .build();
    }

    /**
     * 단일 악보에 대한 난이도 평가 및 저장 (독립 트랜잭션)
     * REQUIRES_NEW: 새로운 트랜잭션을 생성하여 각 악보별 실패가 다른 악보에 영향 없도록 함
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DifficultyEvaluationResponse evaluateSingleSheetWithTransaction(Integer sheetId) {
        // 1. SheetMusic 조회
        SheetMusic sheet = sheetMusicRepository.findById(sheetId)
                .orElseThrow(() -> new BusinessException(BusinessError.SHEET_NOT_FOUND));

        // 2. 기존 난이도 메트릭이 있으면 삭제 (재평가 허용)
        difficultyMetricRepository.deleteBySheetSheetId(sheetId);
        log.info("기존 난이도 메트릭 삭제 완료 - sheetId: {}", sheetId);

        // 3. AI 서버에 난이도 평가 요청
        DifficultyEvaluationResponse response = requestDifficultyEvaluation(sheet.getXmlUrl());

        // 4. Tier 업데이트
        if (response.getLevel() != null) {
            Tier tier = tierRepository.findById(response.getLevel())
                    .orElse(null);
            sheet.updateTier(tier);
            sheetMusicRepository.save(sheet);
            log.info("SheetMusic Tier 업데이트 완료 - sheetId: {}, tierId: {}", sheetId, response.getLevel());
        }

        // 5. SheetDifficultyMetric 저장
        if (response.getMetrics() != null) {
            String recommendationsJson = null;
            if (response.getRecommendations() != null) {
                try {
                    recommendationsJson = objectMapper.writeValueAsString(response.getRecommendations());
                } catch (JsonProcessingException e) {
                    log.error("recommendations JSON 변환 실패 - sheetId: {}", sheetId, e);
                }
            }

            SheetDifficultyMetric metric = SheetDifficultyMetric.builder()
                    .sheet(sheet)
                    .tempo(response.getMetrics().getTempo())
                    .rhythm(response.getMetrics().getRhythm())
                    .intervals(response.getMetrics().getIntervals())
                    .harmony(response.getMetrics().getHarmony())
                    .technique(response.getMetrics().getTechnique())
                    .length(response.getMetrics().getLength())
                    .summary(response.getSummary())
                    .recommendations(recommendationsJson)
                    .build();

            difficultyMetricRepository.save(metric);
            log.info("SheetDifficultyMetric 저장 완료 - sheetId: {}", sheetId);
        }

        return response;
    }
}

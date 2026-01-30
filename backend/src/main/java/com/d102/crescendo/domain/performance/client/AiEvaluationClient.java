package com.d102.crescendo.domain.performance.client;

import com.d102.crescendo.domain.performance.dto.ai.AiEvaluationRequest;
import com.d102.crescendo.domain.performance.dto.ai.AiEvaluationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class AiEvaluationClient {

    private final WebClient webClient;

    public AiEvaluationClient(
            @Value("${ai.server.base-url:https://cresd102.duckdns.org/ai}") String aiServerBaseUrl,
            WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(aiServerBaseUrl)
                .build();
    }

    /**
     * AI 서버에 연주 평가 요청
     * @param audioUrl 연주 음원 URL
     * @return AI 평가 결과
     */
    public AiEvaluationResponse evaluatePerformance(String audioUrl) {
        AiEvaluationRequest request = AiEvaluationRequest.builder()
                .audioUrl(audioUrl)
                .build();

        try {
            return webClient.post()
                    .uri("/evaluate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiEvaluationResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("AI 서버 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("AI 평가 요청에 실패했습니다.", e);
        }
    }
}
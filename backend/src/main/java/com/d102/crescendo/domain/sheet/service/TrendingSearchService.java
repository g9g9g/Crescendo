package com.d102.crescendo.domain.sheet.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.d102.crescendo.domain.sheet.document.SearchLogDocument;
import com.d102.crescendo.domain.sheet.dto.response.TrendingSearchResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendingSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String TRENDING_CACHE_KEY = "trending:searches";
    private static final long CACHE_TTL_SECONDS = 70;
    private static final int TARGET_COUNT = 12;

    // 기본 추천 검색어
    private static final List<String> DEFAULT_KEYWORDS = Arrays.asList(
            "비긴어게인",
            "아이유",
            "데이식스",
            "백예린",
            "체인소맨",
            "귀멸의칼날",
            "클래식",
            "피아노",
            "기타",
            "뉴에이지",
            "재즈",
            "발라드"
    );

    /**
     * 실시간 인기 검색어 조회 (Redis 캐시 사용)
     */
    public TrendingSearchResponse getTrendingSearches() {
        try {
            String cachedData = redisTemplate.opsForValue().get(TRENDING_CACHE_KEY);
            if (cachedData != null) {
                log.debug("캐시 히트: Redis에서 인기 검색어 조회");
                return objectMapper.readValue(cachedData, TrendingSearchResponse.class);
            }
        } catch (JsonProcessingException e) {
            log.error("Redis 캐시 역직렬화 실패", e);
        } catch (Exception e) {
            log.error("Redis 조회 실패", e);
        }

        // 캐시 미스 시 기본값 반환
        log.warn("캐시 미스: 기본 추천 검색어 반환");
        return TrendingSearchResponse.builder()
                .generatedAt(Instant.now())
                .keywords(DEFAULT_KEYWORDS.subList(0, Math.min(TARGET_COUNT, DEFAULT_KEYWORDS.size())))
                .build();
    }

    /**
     * 인기 검색어 집계 및 Redis 캐싱
     * 스케줄러에서 호출
     */
    public void aggregateAndCacheTrendingSearches() {
        try {
            log.info("인기 검색어 집계 시작");

            List<String> trendingKeywords = new ArrayList<>();
            Instant now = Instant.now();

            // 1. 최근 1분 집계
            trendingKeywords = aggregateSearches(now.minusSeconds(60), now, TARGET_COUNT);
            log.info("최근 1분 검색어 집계 결과: {} 개", trendingKeywords.size());

            // 2. 12개 미만이면 최근 10분 추가
            if (trendingKeywords.size() < TARGET_COUNT) {
                List<String> tenMinutesKeywords = aggregateSearches(now.minusSeconds(600), now, TARGET_COUNT);
                trendingKeywords = mergeKeywords(trendingKeywords, tenMinutesKeywords, TARGET_COUNT);
                log.info("최근 10분 추가 후: {} 개", trendingKeywords.size());
            }

            // 3. 여전히 12개 미만이면 최근 1시간 추가  // 임시로 24시간으로 설정
            if (trendingKeywords.size() < TARGET_COUNT) {
                List<String> oneHourKeywords = aggregateSearches(now.minusSeconds(86400), now, TARGET_COUNT);
                trendingKeywords = mergeKeywords(trendingKeywords, oneHourKeywords, TARGET_COUNT);
                log.info("최근 24시간 추가 후: {} 개", trendingKeywords.size());
            }

            // 4. 여전히 12개 미만이면 기본 추천어 추가
            if (trendingKeywords.size() < TARGET_COUNT) {
                trendingKeywords = mergeKeywords(trendingKeywords, DEFAULT_KEYWORDS, TARGET_COUNT);
                log.info("기본 추천어 추가 후: {} 개", trendingKeywords.size());
            }

            // 5. Redis에 캐싱
            TrendingSearchResponse response = TrendingSearchResponse.builder()
                    .generatedAt(now)
                    .keywords(trendingKeywords)
                    .build();

            cacheTrendingSearches(response);
            log.info("인기 검색어 집계 완료: {}", trendingKeywords);

        } catch (Exception e) {
            log.error("인기 검색어 집계 실패", e);
        }
    }

    /**
     * 특정 시간 범위의 검색어 집계 (Elasticsearch Aggregation API 사용)
     */
    private List<String> aggregateSearches(Instant from, Instant to, int size) {
        try {
            // Elasticsearch Java Client를 사용한 직접 검색
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("search_logs")
                    .size(0)
                    .query(q -> q
                            .range(r -> r
                                    .date(d -> d
                                            .field("timestamp")
                                            .gte(from.toString())
                                            .lt(to.toString())
                                    )
                            )
                    )
                    .aggregations("top_keywords", a -> a
                            .terms(t -> t
                                    .field("query")
                                    .size(size)
                                    .minDocCount(1)
                            )
                    )
            );

            // 검색 수행
            SearchResponse<SearchLogDocument> response = elasticsearchClient.search(
                    searchRequest,
                    SearchLogDocument.class
            );

            // Aggregation 결과 추출
            var topKeywordsAgg = response.aggregations().get("top_keywords");
            if (topKeywordsAgg == null || !topKeywordsAgg.isSterms()) {
                log.warn("top_keywords 집계 결과 없음 - from: {}, to: {}", from, to);
                return new ArrayList<>();
            }

            // Bucket에서 검색어 추출 (빈 문자열 필터링)
            return topKeywordsAgg.sterms().buckets().array().stream()
                    .map(StringTermsBucket::key)
                    .map(FieldValue::stringValue)
                    .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("검색어 집계 실패 (IOException) - from: {}, to: {}", from, to, e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("검색어 집계 실패 - from: {}, to: {}", from, to, e);
            return new ArrayList<>();
        }
    }

    /**
     * 두 검색어 리스트를 병합 (중복 제거, 순서 유지)
     */
    private List<String> mergeKeywords(List<String> existing, List<String> additional, int maxSize) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(existing);
        for (String keyword : additional) {
            if (merged.size() >= maxSize) {
                break;
            }
            merged.add(keyword);
        }
        return new ArrayList<>(merged);
    }

    /**
     * 인기 검색어를 Redis에 캐싱
     */
    private void cacheTrendingSearches(TrendingSearchResponse response) {
        try {
            String jsonData = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                    TRENDING_CACHE_KEY,
                    jsonData,
                    CACHE_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
            log.info("인기 검색어 캐싱 완료 (TTL: {}초)", CACHE_TTL_SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Redis 캐시 직렬화 실패", e);
        } catch (Exception e) {
            log.error("Redis 저장 실패", e);
        }
    }
}

package com.d102.crescendo.domain.recommendation.service;

import com.d102.crescendo.domain.recommendation.dto.MinMaxRange;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 난이도 지표 Min-Max 정규화 서비스
 * Redis 캐싱을 통해 성능 최적화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NormalizationService {

    private final EntityManager entityManager;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "difficulty:min_max:";
    private static final long CACHE_TTL_HOURS = 24;

    // 6개 난이도 지표
    private static final String[] METRIC_TYPES = {
            "tempo", "rhythm", "intervals", "harmony", "technique", "length"
    };

    /**
     * 배치 실행 시 모든 난이도 지표의 Min-Max 값을 갱신
     */
    public void refreshMinMaxCache() {
        log.info("난이도 지표 Min-Max 캐시 갱신 시작");

        for (String metricType : METRIC_TYPES) {
            MinMaxRange range = calculateMinMaxFromDB(metricType);
            cacheMinMax(metricType, range);
            log.info("캐시 갱신 완료 - {}: min={}, max={}", metricType, range.min(), range.max());
        }

        log.info("난이도 지표 Min-Max 캐시 갱신 완료");
    }

    /**
     * 특정 난이도 지표의 Min-Max 범위 조회 (캐시 우선)
     *
     * @param metricType 지표 타입 (tempo, rhythm, intervals, harmony, technique, length)
     * @return Min-Max 범위
     */
    public MinMaxRange getMinMax(String metricType) {
        // 1. Redis 캐시에서 조회
        MinMaxRange cached = getMinMaxFromCache(metricType);
        if (cached != null) {
            return cached;
        }

        // 2. 캐시 미스 시 DB에서 계산 후 캐싱
        log.info("캐시 미스 - DB에서 {} Min-Max 계산", metricType);
        MinMaxRange range = calculateMinMaxFromDB(metricType);
        cacheMinMax(metricType, range);

        return range;
    }

    /**
     * DB에서 특정 지표의 Min-Max 계산
     */
    private MinMaxRange calculateMinMaxFromDB(String metricType) {
        String sql = String.format(
                "SELECT MIN(m.%s) as min_val, MAX(m.%s) as max_val " +
                        "FROM sheet_difficulty_metric m " +
                        "WHERE m.%s IS NOT NULL",
                metricType, metricType, metricType
        );

        Query query = entityManager.createNativeQuery(sql);
        Object[] result = (Object[]) query.getSingleResult();

        Double min = result[0] != null ? ((Number) result[0]).doubleValue() : 0.0;
        Double max = result[1] != null ? ((Number) result[1]).doubleValue() : 1.0;

        return new MinMaxRange(min, max);
    }

    /**
     * Redis에 Min-Max 값 캐싱
     */
    private void cacheMinMax(String metricType, MinMaxRange range) {
        String key = CACHE_KEY_PREFIX + metricType;
        String value = String.format("%f,%f", range.min(), range.max());

        redisTemplate.opsForValue().set(key, value, CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * Redis에서 Min-Max 값 조회
     */
    private MinMaxRange getMinMaxFromCache(String metricType) {
        String key = CACHE_KEY_PREFIX + metricType;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        try {
            String[] parts = value.split(",");
            Double min = Double.parseDouble(parts[0]);
            Double max = Double.parseDouble(parts[1]);
            return new MinMaxRange(min, max);
        } catch (Exception e) {
            log.error("Redis 캐시 파싱 실패: {}", value, e);
            return null;
        }
    }

    /**
     * 값을 정규화 (0.0 ~ 1.0)
     *
     * @param value      정규화할 값
     * @param metricType 지표 타입
     * @return 정규화된 값
     */
    public double normalize(Double value, String metricType) {
        if (value == null) {
            return 0.0;
        }

        MinMaxRange range = getMinMax(metricType);
        return range.normalize(value);
    }
}
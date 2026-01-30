package com.d102.crescendo.domain.sheet.service;

import com.d102.crescendo.domain.ai.dto.response.RecommendResponse;
import com.d102.crescendo.domain.sheet.dto.response.PopularSheetResponse;
import com.d102.crescendo.domain.sheet.dto.response.SimilarSheetResponse;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import com.d102.crescendo.domain.sheet.repository.SheetMusicRepository;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SheetRecommendationService {

    private final SheetMusicRepository sheetMusicRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String POPULAR_SHEETS_CACHE_KEY = "popular:sheets:top20";
    private static final long CACHE_TTL_HOURS = 24;

    /**
     * 인기 악보 조회 (다운로드 수 기준 top 20개)
     * Redis 캐싱 적용 (하루 주기로 저장)
     * @return 인기 악보 리스트
     */
    public List<PopularSheetResponse> getPopularSheets() {
        // 1. Redis에서 캐시된 top 20 데이터 조회
        List<PopularSheetResponse> top20Sheets = getTop20FromCache();

        // 2. 캐시에 데이터가 없으면 DB에서 조회 후 캐싱
        if (top20Sheets == null || top20Sheets.isEmpty()) {
            log.info("캐시 미스: DB에서 인기 악보 top 20 조회");
            top20Sheets = loadTop20FromDB();
            cacheTop20Sheets(top20Sheets);
        } else {
            log.info("캐시 히트: Redis에서 인기 악보 top 20 조회");
        }

        return top20Sheets;
    }

    /**
     * Redis에서 캐시된 top 20 데이터 조회
     */
    private List<PopularSheetResponse> getTop20FromCache() {
        try {
            String cachedData = redisTemplate.opsForValue().get(POPULAR_SHEETS_CACHE_KEY);
            if (cachedData == null) {
                return null;
            }
            return objectMapper.readValue(cachedData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("Redis 캐시 역직렬화 실패", e);
            return null;
        } catch (Exception e) {
            log.error("Redis 조회 실패", e);
            return null;
        }
    }

    /**
     * DB에서 top 20 악보 조회 및 DTO 변환
     */
    private List<PopularSheetResponse> loadTop20FromDB() {
        List<SheetMusic> top20Sheets = sheetMusicRepository.findTopByDownloadNumber(
                PageRequest.of(0, 20)
        );

        return top20Sheets.stream()
                .map(PopularSheetResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * top 20 악보를 Redis에 캐싱 (24시간 TTL)
     */
    private void cacheTop20Sheets(List<PopularSheetResponse> sheets) {
        try {
            String jsonData = objectMapper.writeValueAsString(sheets);
            redisTemplate.opsForValue().set(
                    POPULAR_SHEETS_CACHE_KEY,
                    jsonData,
                    CACHE_TTL_HOURS,
                    TimeUnit.HOURS
            );
            log.info("인기 악보 캐싱 완료 (TTL: {}시간)", CACHE_TTL_HOURS);
        } catch (JsonProcessingException e) {
            log.error("Redis 캐시 직렬화 실패", e);
        } catch (Exception e) {
            log.error("Redis 저장 실패", e);
        }
    }

    /**
     * 유사한 악보 목록을 조회합니다.
     * @param sheetId 기준이 되는 악보 ID
     * @return 유사한 악보 목록 (최대 10개)
     */
    public List<SimilarSheetResponse> getSimilarSheets(Integer sheetId) {
        try {
            // 1. 기준 악보 조회
            SheetMusic querySheet = sheetMusicRepository.findById(sheetId)
                    .orElseThrow(() -> new BusinessException(BusinessError.SHEET_NOT_FOUND));

            // 2. 추천 악보 ID 목록 조회 (기본 10개)
            RecommendResponse recommendation = getRecommendation(sheetId, 10);
            List<Integer> similarSheetIds = recommendation.getResults();

            // 3. 유사 악보 ID 리스트로 실제 악보 정보 조회
            List<SheetMusic> similarSheets = sheetMusicRepository.findAllById(similarSheetIds);

            // 4. 장르가 같은 악보를 맨 앞에 오도록 정렬
            Integer queryGenreId = querySheet.getGenre().getGenreId();
            similarSheets.sort((sheet1, sheet2) -> {
                boolean sheet1SameGenre = sheet1.getGenre().getGenreId().equals(queryGenreId);
                boolean sheet2SameGenre = sheet2.getGenre().getGenreId().equals(queryGenreId);

                if (sheet1SameGenre && !sheet2SameGenre) {
                    return -1;
                } else if (!sheet1SameGenre && sheet2SameGenre) {
                    return 1;
                } else {
                    // 같은 장르끼리는 원래 순서 유지 (similarSheetIds 순서)
                    return similarSheetIds.indexOf(sheet1.getSheetId()) -
                           similarSheetIds.indexOf(sheet2.getSheetId());
                }
            });

            // 5. DTO로 변환하여 반환
            return similarSheets.stream()
                    .map(SimilarSheetResponse::from)
                    .collect(Collectors.toList());

        } catch (BusinessException e) {
            log.error("유사 악보 조회 실패 - sheetId: {}, error: {}", sheetId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("유사 악보 조회 중 예외 발생 - sheetId: {}", sheetId, e);
            throw new BusinessException(BusinessError.AI_RECOMMENDATION_FAILED);
        }
    }

    public RecommendResponse getRecommendation(Integer scoreId, Integer topK) {
        try {
            // 1. 기준 악보가 존재하는지 확인
            SheetMusic querySheet = sheetMusicRepository.findById(scoreId)
                    .orElseThrow(() -> new BusinessException(BusinessError.SHEET_NOT_FOUND));
            log.info("querySheet: {}", scoreId);

            // 2. 임베딩이 존재하는지 확인
            if (querySheet.getEmbedding() == null) {
                log.error("악보 ID {}의 임베딩이 없습니다.", scoreId);
                throw new BusinessException(BusinessError.EMBEDDING_NOT_FOUND);
            }

            // 3. pgvector를 이용한 유사 악보 검색
            log.info("pgvector 검색 시작 - scoreId: {}, topK: {}", scoreId, topK);
            List<Integer> similarSheetIds = sheetMusicRepository.findSimilarSheetsByEmbedding(scoreId, topK);
            log.info("pgvector 검색 완료 - 결과 개수: {}", similarSheetIds.size());

            // 4. 결과 반환
            return RecommendResponse.builder()
                    .queryScoreId(scoreId)
                    .topK(topK)
                    .results(similarSheetIds)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("유사 악보 추천 실패: {}", e.getMessage(), e);
            throw new BusinessException(BusinessError.AI_RECOMMENDATION_FAILED);
        }
    }

    /**
     * Redis에서 인기 악보 캐시 무효화
     */
    public void invalidatePopularSheetsCache() {
        try {
            Boolean deleted = redisTemplate.delete(POPULAR_SHEETS_CACHE_KEY);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Redis 인기 악보 캐시 삭제 완료");
            } else {
                log.warn("Redis 인기 악보 캐시 키가 존재하지 않음");
            }
        } catch (Exception e) {
            log.error("Redis 인기 악보 캐시 삭제 실패", e);
        }
    }
}
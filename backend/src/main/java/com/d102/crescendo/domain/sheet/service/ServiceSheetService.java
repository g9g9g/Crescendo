package com.d102.crescendo.domain.sheet.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.d102.crescendo.domain.common.entity.Tier;
import com.d102.crescendo.domain.common.repository.TierRepository;
import com.d102.crescendo.domain.recommendation.repository.DailyRecommendationRepository;
import com.d102.crescendo.domain.recommendation.service.TodaySheetBatchService;
import com.d102.crescendo.domain.sheet.document.SheetMusicDocument;
import com.d102.crescendo.domain.sheet.dto.response.SearchItemResponse;
import com.d102.crescendo.domain.sheet.dto.response.SearchResponse;
import com.d102.crescendo.domain.sheet.dto.response.ServiceSheetDetailResponse;
import com.d102.crescendo.domain.sheet.dto.response.ServiceSheetSearchResponse;
import com.d102.crescendo.domain.sheet.entity.Genre;
import com.d102.crescendo.domain.sheet.entity.Instrument;
import com.d102.crescendo.domain.sheet.entity.SheetDifficultyMetric;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import com.d102.crescendo.domain.sheet.repository.GenreRepository;
import com.d102.crescendo.domain.sheet.repository.InstrumentRepository;
import com.d102.crescendo.domain.sheet.repository.SheetDifficultyMetricRepository;
import com.d102.crescendo.domain.sheet.repository.SheetMusicDocumentRepository;
import com.d102.crescendo.domain.sheet.repository.SheetMusicRepository;
import com.d102.crescendo.global.dto.request.UpdateSheetMusicRequest;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceSheetService {

    private final SheetMusicRepository sheetMusicRepository;
    private final SheetMusicDocumentRepository sheetMusicDocumentRepository;
    private final SheetDifficultyMetricRepository difficultyMetricRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;
    private final GenreRepository genreRepository;
    private final InstrumentRepository instrumentRepository;
    private final TierRepository tierRepository;
    private final SheetRecommendationService sheetRecommendationService;
    private final DailyRecommendationRepository dailyRecommendationRepository;
    private final TodaySheetBatchService todaySheetBatchService;

    /**
     * 서비스 악보 단건 조회
     * - sourceType이 SYSTEM인 악보만 조회
     * - visibleYes가 true인 악보만 조회
     */
    @Transactional(readOnly = true)
    public ServiceSheetDetailResponse getServiceSheet(Integer sheetId){

        SheetMusic sheet=sheetMusicRepository.findServiceSheetByIdWithDetails(sheetId)
                .orElseThrow(() -> {
                    log.warn("서비스 악보를 찾을 수 없음 - sheetId: {}", sheetId);
                    return new BusinessException(BusinessError.SHEET_NOT_FOUND);
                });
        // 2. visibleYes 체크
        if (!sheet.getVisibleYes()) {
            log.warn("비공개 악보 접근 시도 - sheetId: {}", sheetId);
            throw new BusinessException(BusinessError.SHEET_NOT_VISIBLE);
        }

        log.info("서비스 악보 조회 성공 - sheetId: {}, title: {}", sheetId, sheet.getTitle());

        // 3. 난이도 정보 조회
        ServiceSheetDetailResponse.DifficultyMetrics metrics = null;
        String summary = null;
        List<String> recommendations = null;

        SheetDifficultyMetric difficultyMetric = difficultyMetricRepository
                .findBySheetSheetId(sheetId)
                .orElse(null);

        if (difficultyMetric != null) {
            // metrics 매핑
            metrics = ServiceSheetDetailResponse.DifficultyMetrics.builder()
                    .tempo(difficultyMetric.getTempo())
                    .rhythm(difficultyMetric.getRhythm())
                    .intervals(difficultyMetric.getIntervals())
                    .harmony(difficultyMetric.getHarmony())
                    .technique(difficultyMetric.getTechnique())
                    .length(difficultyMetric.getLength())
                    .build();

            summary = difficultyMetric.getSummary();

            // recommendations JSON 문자열을 List<String>으로 변환
            if (difficultyMetric.getRecommendations() != null) {
                try {
                    recommendations = objectMapper.readValue(
                            difficultyMetric.getRecommendations(),
                            new TypeReference<List<String>>() {}
                    );
                } catch (Exception e) {
                    log.error("recommendations JSON 파싱 실패 - sheetId: {}", sheetId, e);
                }
            }
        }

        // 4. 응답 생성
        return ServiceSheetDetailResponse.builder()
                .title(sheet.getTitle())
                .composer(sheet.getComposer())
                .genreId(sheet.getGenre() != null ? sheet.getGenre().getGenreId() : null)
                .instrumentId(sheet.getInstrument() != null ? sheet.getInstrument().getInstrumentId() : null)
                .tierCode(sheet.getTier() != null ? sheet.getTier().getTierCode() : null)
                .tierLevel(sheet.getTier() != null ? sheet.getTier().getTierLevel() : null)
                .downloadNumber(sheet.getDownloadNumber())
                .thumbnailUrl(sheet.getThumbnailUrl())
                .xmlUrlPreview(sheet.getXmlUrlPreview())
                .xmlUrl(sheet.getXmlUrl())
                .metrics(metrics)
                .summary(summary)
                .recommendations(recommendations)
                .build();
    }

    /**
     * 서비스 악보 검색
     * @param searchKeyword 검색어 (제목, 작곡가)
     * @param instrumentId 악기 ID
     * @param genreIdsStr 장르 ID 문자열 (콤마로 구분)
     * @param tierCode 티어 코드
     * @param sortType 정렬 타입 (1: 최신순, 2: 다운로드순, 3: 제목순)
     * @return 서비스 악보 검색 결과
     */
    @Transactional(readOnly = true)
    public ServiceSheetSearchResponse searchServiceSheets(
            String searchKeyword,
            Integer instrumentId,
            String genreIdsStr,
            String tierCode,
            Integer sortType
    ) {

       log.info("서비스 악보 검색 - keyword: {}, instrumentId: {}, genreIds: {}, tierCode: {}, sortType: {}",
                searchKeyword, instrumentId, genreIdsStr, tierCode, sortType);

        // genreIds 문자열 파싱 (예: "1,3,5" -> [1, 3, 5])
        List<Integer> genreIds = null;
        if (genreIdsStr != null && !genreIdsStr.trim().isEmpty()) {
            try {
                genreIds = Arrays.stream(genreIdsStr.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            } catch (NumberFormatException e) {
                log.warn("잘못된 genreIds 형식: {}", genreIdsStr);
            }
        }

        // Repository를 통한 검색
        List<SheetMusic> sheets = sheetMusicRepository.searchServiceSheets(
                searchKeyword,
                instrumentId,
                genreIds,
                tierCode,
                sortType
        );

        // DTO 변환
        List<ServiceSheetSearchResponse.SheetItem> sheetItems = sheets.stream()
                .map(sheet -> ServiceSheetSearchResponse.SheetItem.builder()
                        .sheetId(sheet.getSheetId())
                        .title(sheet.getTitle())
                        .composer(sheet.getComposer())
                        .thumbnailUrl(sheet.getThumbnailUrl())
                        .genreId(sheet.getGenre().getGenreId())
                        .tierCode(sheet.getTier() != null ? sheet.getTier().getTierCode() : null)
                        .tierLevel(sheet.getTier() != null ? sheet.getTier().getTierLevel() : null)
                        .instrumentId(sheet.getInstrument().getInstrumentId())
                        .downloadNumber(sheet.getDownloadNumber())
                        .updatedAt(sheet.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        log.info("서비스 악보 검색 완료 - 결과: {}건", sheetItems.size());

        return ServiceSheetSearchResponse.builder()
                .totalCount(sheetItems.size())
                .sheetList(sheetItems)
                .build();
    }

    public List<String> getSuggestions(String query) {
        // title 자동완성 쿼리
        Query titleQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .type(TextQueryType.BoolPrefix)
                .fields("title.auto_complete", "title.auto_complete._2gram", "title.auto_complete._3gram")
        )._toQuery();

        // composer 자동완성 쿼리
        Query composerQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .type(TextQueryType.BoolPrefix)
                .fields("composer.auto_complete", "composer.auto_complete._2gram", "composer.auto_complete._3gram")
        )._toQuery();

        // 각각 검색
        NativeQuery titleNativeQuery = NativeQuery.builder()
                .withQuery(titleQuery)
                .withPageable(PageRequest.of(0, 5))
                .build();

        NativeQuery composerNativeQuery = NativeQuery.builder()
                .withQuery(composerQuery)
                .withPageable(PageRequest.of(0, 5))
                .build();

        List<String> suggestions = new ArrayList<>();

        // title 결과
        SearchHits<SheetMusicDocument> titleHits =
                elasticsearchOperations.search(titleNativeQuery, SheetMusicDocument.class);
        titleHits.forEach(hit -> {
            String title = hit.getContent().getTitle();
            if (title != null && !title.isEmpty()) suggestions.add(title);
        });

        // composer 결과
        SearchHits<SheetMusicDocument> composerHits =
                elasticsearchOperations.search(composerNativeQuery, SheetMusicDocument.class);
        composerHits.forEach(hit -> {
            String composer = hit.getContent().getComposer();
            if (composer != null && !composer.isEmpty()) suggestions.add(composer);
        });

        // 중복 제거 후 상위 10개만 반환
        return suggestions.stream()
                .distinct()
                .limit(10)
                .toList();
    }

    public SearchResponse searchSheets(
            String query,
            Integer genreId,
            Integer instrumentId,
            String tierCode,
            int page,
            int size
    ) {
        Query finalQuery;

        // query가 없으면 전체 검색
        if (query == null || query.trim().isEmpty()) {
            finalQuery = MatchAllQuery.of(m -> m)._toQuery();
        } else {
            // 1. multi_match 쿼리 - 제목, 작곡가, 장르, 악기명으로 검색
            Query multiMatchQuery = MultiMatchQuery.of(m -> m
                    .query(query)
                    .fields("title^2", "composer^1.5", "genre^1", "instrument^1")
                    .fuzziness("AUTO")
            )._toQuery();

            // 2. term: analyzer에서 걸러지는 단어도 정확히 검색 (예: "푹신푹신")
            Query termQuery = TermQuery.of(t -> t
                    .field("title.keyword")
                    .value(query)
                    .boost(3.0f) // 가중치
            )._toQuery();

            // 3. 두 쿼리 병합
            finalQuery = BoolQuery.of(b -> b
                    .should(multiMatchQuery)
                    .should(termQuery)
                    .minimumShouldMatch("1")
            )._toQuery();
        }

        List<Query> filters = new ArrayList<>();

        // term filter: sourceType=SYSTEM인 악보만 조회
        Query sourceTypeFilter = TermQuery.of(t -> t
                .field("sourceType")
                .value("SYSTEM")
        )._toQuery();
        filters.add(sourceTypeFilter);

        // term filter: 장르 ID로 필터링
        if (genreId != null) {
            Query genreFilter = TermQuery.of(t -> t
                    .field("genreId")
                    .value(genreId)
            )._toQuery();
            filters.add(genreFilter);
        }

        // term filter: 악기 ID로 필터링
        if (instrumentId != null) {
            Query instrumentFilter = TermQuery.of(t -> t
                    .field("instrumentId")
                    .value(instrumentId)
            )._toQuery();
            filters.add(instrumentFilter);
        }

        // term filter: 난이도 코드로 필터링
        if (tierCode != null && !tierCode.isEmpty()) {
            Query tierFilter = TermQuery.of(t -> t
                    .field("tierCode")
                    .value(tierCode)
            )._toQuery();
            filters.add(tierFilter);
        }

        // should: downloadNumber > 2000
        Query downloadNumShould = NumberRangeQuery.of(r -> r
                .field("downloadNumber")
                .gt(2000.0)
        )._toRangeQuery()._toQuery();

        // bool query 조합
        Query boolQuery = BoolQuery.of(b -> b
                .must(finalQuery)
                .filter(filters)
                .should(downloadNumShould)  // 가중치
        )._toQuery();

        // NativeQuery 생성
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(page - 1, size))
                .build();

        // 쿼리 실행
        SearchHits<SheetMusicDocument> searchHits = this.elasticsearchOperations.search(nativeQuery, SheetMusicDocument.class);

        // 결과 매핑
        List<SearchItemResponse> results = searchHits.getSearchHits().stream()
                .map(hit -> {
                    SheetMusicDocument doc = hit.getContent();
                    return SearchItemResponse.from(doc);
                })
                .toList();

        // SearchResponse 반환
        return SearchResponse.builder()
                .totalCount(searchHits.getTotalHits())
                .sheetList(results)
                .build();
    }

    /**
     * 악보 정보 수정 (관리자 전용)
     * - 요청된 필드만 업데이트
     * - Elasticsearch 동기화
     */
    @Transactional
    public void updateSheetMusic(Integer sheetId, UpdateSheetMusicRequest request) {

        log.info("악보 정보 수정 요청 - sheetId: {}", sheetId);

        // 1. 악보 조회
        SheetMusic sheet = sheetMusicRepository.findById(sheetId)
                .orElseThrow(() -> {
                    log.warn("악보를 찾을 수 없음 - sheetId: {}", sheetId);
                    return new BusinessException(BusinessError.SHEET_NOT_FOUND);
                });

        boolean visibleYesChanged = false;
        boolean newVisibleYes = sheet.getVisibleYes();

        // 2. 필드별 업데이트 (null이 아닌 필드만)
        if (request.getTitle() != null) {
            sheet.updateTitle(request.getTitle());
            log.info("제목 수정 - sheetId: {}, title: {}", sheetId, request.getTitle());
        }

        if (request.getComposer() != null) {
            sheet.updateComposer(request.getComposer());
            log.info("작곡가 수정 - sheetId: {}, composer: {}", sheetId, request.getComposer());
        }

        if (request.getGenreId() != null) {
            Genre genre = genreRepository.findById(request.getGenreId())
                    .orElseThrow(() -> {
                        log.warn("장르를 찾을 수 없음 - genreId: {}", request.getGenreId());
                        return new BusinessException(BusinessError.GENRE_NOT_FOUND);
                    });
            sheet.updateGenre(genre);
            log.info("장르 수정 - sheetId: {}, genreId: {}", sheetId, request.getGenreId());
        }

        if (request.getInstrumentId() != null) {
            Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                    .orElseThrow(() -> {
                        log.warn("악기를 찾을 수 없음 - instrumentId: {}", request.getInstrumentId());
                        return new BusinessException(BusinessError.INSTRUMENT_NOT_FOUND);
                    });
            sheet.updateInstrument(instrument);
            log.info("악기 수정 - sheetId: {}, instrumentId: {}", sheetId, request.getInstrumentId());
        }

        if (request.getTierId() != null) {
            Tier tier = tierRepository.findById(request.getTierId())
                    .orElseThrow(() -> {
                        log.warn("티어를 찾을 수 없음 - tierId: {}", request.getTierId());
                        return new BusinessException(BusinessError.TIER_NOT_FOUND);
                    });
            sheet.updateTier(tier);
            log.info("티어 수정 - sheetId: {}, tierId: {}", sheetId, request.getTierId());
        }

        if (request.getThumbnailUrl() != null) {
            sheet.updateThumbnailUrl(request.getThumbnailUrl());
            log.info("썸네일 URL 수정 - sheetId: {}", sheetId);
        }

        if (request.getXmlUrl() != null) {
            sheet.updateXmlUrl(request.getXmlUrl());
            log.info("XML URL 수정 - sheetId: {}", sheetId);
        }

        if (request.getXmlUrlPreview() != null) {
            sheet.updateXmlUrlPreview(request.getXmlUrlPreview());
            log.info("미리보기 XML URL 수정 - sheetId: {}", sheetId);
        }

        if (request.getMaxMeasureCnt() != null) {
            sheet.updateMaxMeasureCnt(request.getMaxMeasureCnt());
            log.info("최대 마디 수 수정 - sheetId: {}, maxMeasureCnt: {}", sheetId, request.getMaxMeasureCnt());
        }

        if (request.getStyle() != null) {
            sheet.updateStyle(request.getStyle());
            log.info("스타일 수정 - sheetId: {}, style: {}", sheetId, request.getStyle());
        }

        if (request.getSourceType() != null) {
            sheet.updateSourceType(request.getSourceType());
            log.info("소스 타입 수정 - sheetId: {}, sourceType: {}", sheetId, request.getSourceType());
        }

        if (request.getOriginSheetId() != null) {
            SheetMusic originSheet = sheetMusicRepository.findById(request.getOriginSheetId())
                    .orElseThrow(() -> {
                        log.warn("원본 악보를 찾을 수 없음 - originSheetId: {}", request.getOriginSheetId());
                        return new BusinessException(BusinessError.SHEET_NOT_FOUND);
                    });
            sheet.updateOriginSheet(originSheet);
            log.info("원본 악보 수정 - sheetId: {}, originSheetId: {}", sheetId, request.getOriginSheetId());
        }

        if (request.getDownloadNumber() != null) {
            sheet.updateDownloadNumber(request.getDownloadNumber());
            log.info("다운로드 수 수정 - sheetId: {}, downloadNumber: {}", sheetId, request.getDownloadNumber());
        }

        if (request.getEmbedding() != null) {
            sheet.updateEmbedding(request.getEmbedding());
            log.info("임베딩 수정 - sheetId: {}", sheetId);
        }

        if (request.getCreatedAt() != null) {
            sheet.updateCreatedAt(request.getCreatedAt());
            log.info("생성일시 수정 - sheetId: {}, createdAt: {}", sheetId, request.getCreatedAt());
        }

        if (request.getUpdatedAt() != null) {
            sheet.updateUpdatedAt(request.getUpdatedAt());
            log.info("수정일시 수정 - sheetId: {}, updatedAt: {}", sheetId, request.getUpdatedAt());
        }

        if (request.getVisibleYes() != null) {
            // visibleYes 변경 여부 확인
            if (!sheet.getVisibleYes().equals(request.getVisibleYes())) {
                visibleYesChanged = true;
                newVisibleYes = request.getVisibleYes();
                log.info("노출 여부 변경 감지 - sheetId: {}, {} -> {}",
                        sheetId, sheet.getVisibleYes(), request.getVisibleYes());
            }
            sheet.updateVisibleYes(request.getVisibleYes());
            log.info("노출 여부 수정 - sheetId: {}, visibleYes: {}", sheetId, request.getVisibleYes());
        }

        // 3. DB 저장 (변경 감지로 자동 저장됨)
        sheetMusicRepository.save(sheet);

        // 4. Elasticsearch 동기화
        syncElasticsearch(sheet);

        // 5. visibleYes가 false로 변경된 경우 캐시 및 추천 목록 갱신
        if (visibleYesChanged && !newVisibleYes) {
            log.info("악보 비공개 처리 - 캐시 및 추천 목록 갱신 시작 - sheetId: {}", sheetId);

            // Redis 인기 악보 캐시 무효화
            try {
                sheetRecommendationService.invalidatePopularSheetsCache();
                log.info("Redis 인기 악보 캐시 무효화 완료 - sheetId: {}", sheetId);
            } catch (Exception e) {
                log.error("Redis 인기 악보 캐시 무효화 실패 - sheetId: {}, error: {}",
                        sheetId, e.getMessage(), e);
            }

            // 오늘의 추천 악보 목록 재계산
            try {
                // 1. 숨겨진 악보를 추천받은 사용자 ID 목록 조회
                List<Integer> affectedUserIds = dailyRecommendationRepository.findUserIdsBySheetId(sheetId);
                log.info("숨겨진 악보를 추천받은 사용자 {}명 발견 - sheetId: {}", affectedUserIds.size(), sheetId);

                // 2. 해당 사용자들의 추천을 재계산
                int successCount = 0;
                int failCount = 0;
                for (Integer affectedUserId : affectedUserIds) {
                    try {
                        todaySheetBatchService.recalculateRecommendationForUser(affectedUserId);
                        successCount++;
                    } catch (Exception e) {
                        log.error("사용자 {} 추천 재계산 실패 - sheetId: {}", affectedUserId, sheetId, e);
                        failCount++;
                    }
                }
                log.info("오늘의 추천 악보 재계산 완료 - 성공: {}명, 실패: {}명", successCount, failCount);
            } catch (Exception e) {
                log.error("오늘의 추천 악보 재계산 실패 - sheetId: {}, error: {}",
                        sheetId, e.getMessage(), e);
            }
        }

        log.info("악보 정보 수정 완료 - sheetId: {}", sheetId);
    }

    /**
     * Elasticsearch 동기화
     */
    private void syncElasticsearch(SheetMusic sheet) {
        try {
            if (sheet.getVisibleYes()) {
                // visibleYes가 true인 경우: Elasticsearch에 문서 생성/업데이트
                SheetMusicDocument document = SheetMusicDocument.builder()
                        .sheetId(sheet.getSheetId())
                        .title(sheet.getTitle())
                        .composer(sheet.getComposer())
                        .thumbnailUrl(sheet.getThumbnailUrl())
                        .genreId(sheet.getGenre().getGenreId())
                        .tierCode(sheet.getTier() != null ? sheet.getTier().getTierCode() : null)
                        .tierLevel(sheet.getTier() != null ? sheet.getTier().getTierLevel().intValue() : null)
                        .instrumentId(sheet.getInstrument().getInstrumentId())
                        .downloadNumber(sheet.getDownloadNumber())
                        .updatedAt(sheet.getUpdatedAt())
                        .sourceType(sheet.getSourceType())
                        .genre(sheet.getGenre().getName())
                        .instrument(sheet.getInstrument().getName())
                        .build();

                sheetMusicDocumentRepository.save(document);
                log.info("Elasticsearch에 악보 문서 저장 완료 - sheetId: {}", sheet.getSheetId());
            } else {
                // visibleYes가 false인 경우: Elasticsearch에서 문서 제거
                sheetMusicDocumentRepository.deleteById(String.valueOf(sheet.getSheetId()));
                log.info("Elasticsearch에서 악보 문서 삭제 완료 - sheetId: {}", sheet.getSheetId());
            }
        } catch (Exception e) {
            log.error("Elasticsearch 동기화 실패 - sheetId: {}, error: {}",
                    sheet.getSheetId(), e.getMessage(), e);
        }
    }

    /**
     * 기존 PostgreSQL의 모든 SheetMusic을 Elasticsearch로 마이그레이션
     * 초기 데이터 동기화 또는 재색인 시 사용
     * @return 마이그레이션 성공 건수
     */
    @Transactional(readOnly = true)
    public int migrateAllToElasticsearch() {
        log.info("===== SheetMusic Elasticsearch 마이그레이션 시작 =====");

        // 1. PostgreSQL에서 모든 SheetMusic 조회 (visibleYes=true인 것만)
        List<SheetMusic> allSheets = sheetMusicRepository.findAll();
        log.info("총 {}건의 SheetMusic 발견", allSheets.size());

        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;

        // 2. 각 SheetMusic을 Elasticsearch에 저장
        for (SheetMusic sheet : allSheets) {
            try {
                // visibleYes가 false인 경우 스킵
                if (!sheet.getVisibleYes()) {
                    skippedCount++;
                    continue;
                }

                syncElasticsearch(sheet);
                successCount++;

                // 100건마다 진행 상황 로그
                if (successCount % 100 == 0) {
                    log.info("진행 상황: {}건 완료 / {}건 중", successCount, allSheets.size());
                }
            } catch (Exception e) {
                failCount++;
                log.error("마이그레이션 실패 - sheetId: {}, error: {}",
                        sheet.getSheetId(), e.getMessage(), e);
            }
        }

        log.info("===== SheetMusic Elasticsearch 마이그레이션 완료 =====");
        log.info("성공: {}건, 실패: {}건, 스킵(비공개): {}건, 전체: {}건",
                successCount, failCount, skippedCount, allSheets.size());

        return successCount;
    }
}

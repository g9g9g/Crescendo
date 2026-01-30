package com.d102.crescendo.domain.sheet.service;


import com.d102.crescendo.domain.ai.dto.response.ArrangementResponse;
import com.d102.crescendo.domain.ai.service.AiService;
import com.d102.crescendo.domain.performance.entity.Performance;
import com.d102.crescendo.domain.performance.entity.PerformanceEvaluation;
import com.d102.crescendo.domain.performance.repository.PerformanceRepository;
import com.d102.crescendo.domain.recommendation.repository.DailyRecommendationRepository;
import com.d102.crescendo.domain.recommendation.service.TodaySheetBatchService;
import com.d102.crescendo.domain.sheet.document.UserSheetDocument;
import com.d102.crescendo.domain.sheet.dto.response.MusicXmlParseResponse;
import com.d102.crescendo.domain.sheet.dto.response.UserSheetDetailResponse;
import com.d102.crescendo.domain.sheet.dto.response.UserSheetSearchResponse;
import com.d102.crescendo.domain.sheet.entity.SheetDifficultyMetric;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import com.d102.crescendo.domain.sheet.entity.UserSheet;
import com.d102.crescendo.domain.sheet.repository.SheetDifficultyMetricRepository;
import com.d102.crescendo.domain.sheet.repository.SheetMusicDocumentRepository;
import com.d102.crescendo.domain.sheet.repository.SheetMusicRepository;
import com.d102.crescendo.domain.sheet.repository.UserSheetDocumentRepository;
import com.d102.crescendo.domain.sheet.repository.UserSheetRepository;
import com.d102.crescendo.domain.user.entity.User;
import com.d102.crescendo.domain.user.repository.UserRepository;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSheetService {

    private final UserSheetRepository userSheetRepository;
    private final PerformanceRepository performanceRepository;
    private final SheetMusicRepository sheetMusicRepository;
    private final SheetMusicDocumentRepository sheetMusicDocumentRepository;
    private final UserSheetDocumentRepository userSheetDocumentRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final MusicXmlService musicXmlService;
    private final SheetRecommendationService sheetRecommendationService;
    private final SheetDifficultyMetricRepository sheetDifficultyMetricRepository;
    private final DailyRecommendationRepository dailyRecommendationRepository;
    private final TodaySheetBatchService todaySheetBatchService;

    /**
     * 내 악보 상세 조회
     */
    @Transactional(readOnly = true)
    public UserSheetDetailResponse getUserSheetDetail(Integer userSheetId) {
        log.info("내 악보 상세 조회 - userSheetId: {}", userSheetId);

        // 1. 사용자 악보 조회
        UserSheet userSheet = userSheetRepository.findUserSheetDetailById(userSheetId)
                .orElseThrow(() -> {
                    log.warn("사용자 악보를 찾을 수 없음 - userSheetId: {}", userSheetId);
                    return new BusinessException(BusinessError.USER_SHEET_NOT_FOUND);
                });

        SheetMusic sheet = userSheet.getSheet();

        // 2. 평가 모드 연주 기록 조회 (최신순, practiceMode = false)
        List<Performance> evaluationPerformances = performanceRepository
                .findEvaluationModeByUserSheetIdOrderByEndedAtDesc(userSheetId);

        // 3. 시작 마디 계산 (가장 최근 연주의 끝 마디, 연습/평가 모드 상관없이)
        Short startMeasure = calculateStartMeasure(userSheetId);

        // 4. 평가 모드 연주 기록 DTO 변환 (PerformanceEvaluation에서 score와 comment 가져오기)
        // 평가가 완료된 것만 포함
        List<UserSheetDetailResponse.PerformanceItem> performanceItems = evaluationPerformances.stream()
                .filter(p -> p.getEvaluation() != null)
                .map(p -> {
                    PerformanceEvaluation evaluation = p.getEvaluation();
                    return UserSheetDetailResponse.PerformanceItem.builder()
                            .performanceId(p.getPlayId())
                            .totalScore(evaluation.getScore() != null ? evaluation.getScore() : (short) 0)
                            .comment(evaluation.getComment())
                            .endedAt(p.getEndedAt())
                            .wavXmlUrl(p.getWavXmlUrl())
                            .build();
                })
                .toList();

        // 5. 난이도 지표 조회 (티어가 있는 경우에만)
        UserSheetDetailResponse.DifficultyMetrics metrics = null;
        if (sheet.getTier() != null) {
            Optional<SheetDifficultyMetric> metricOpt =
                    sheetDifficultyMetricRepository.findBySheetSheetId(sheet.getSheetId());

            if (metricOpt.isPresent()) {
                com.d102.crescendo.domain.sheet.entity.SheetDifficultyMetric metric = metricOpt.get();
                metrics = UserSheetDetailResponse.DifficultyMetrics.builder()
                        .tempo(metric.getTempo())
                        .rhythm(metric.getRhythm())
                        .intervals(metric.getIntervals())
                        .harmony(metric.getHarmony())
                        .technique(metric.getTechnique())
                        .length(metric.getLength())
                        .build();

                log.info("난이도 지표 조회 완료 - sheetId: {}", sheet.getSheetId());
            } else {
                log.info("난이도 지표 없음 - sheetId: {} (티어는 있지만 metrics 없음)", sheet.getSheetId());
            }
        }

        log.info("내 악보 상세 조회 성공 - userSheetId: {}, title: {}, 평가기록: {}개, 난이도지표: {}",
                userSheetId, sheet.getTitle(), performanceItems.size(), metrics != null ? "있음" : "없음");

        // 6. 응답 생성
        return UserSheetDetailResponse.builder()
                .userSheetId(userSheet.getUserSheetId())
                .title(sheet.getTitle())
                .composer(sheet.getComposer())
                .genreId(sheet.getGenre().getGenreId())
                .tierCode(sheet.getTier() != null ? sheet.getTier().getTierCode() : null)
                .tierLevel(sheet.getTier() != null ? sheet.getTier().getTierLevel() : null)
                .instrumentId(sheet.getInstrument().getInstrumentId())
                .thumbnailUrl(sheet.getThumbnailUrl())
                .xmlUrl(sheet.getXmlUrl())
                .startMeasure(startMeasure)
                .performances(UserSheetDetailResponse.PerformanceList.builder()
                        .items(performanceItems)
                        .build())
                .metrics(metrics)
                .progress(userSheet.getProgressRate() != null ? userSheet.getProgressRate().intValue() : 0)
                .build();
    }

    /**
     * 서비스 악보를 내 악보로 담기
     * - user_sheet 테이블에 새 레코드 생성
     * - sheet_music의 download_number +1
     */
    @Transactional
    public void addSheetToMyLibrary(Integer userId, Integer sheetId) {

        log.info("내 악보로 담기 요청 - userId: {}, sheetId: {}", userId, sheetId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음 - userId: {}", userId);
                    return new BusinessException(BusinessError.USER_NOT_FOUND);
                });

        // 2. 서비스 악보 조회
        SheetMusic sheet = sheetMusicRepository.findById(sheetId)
                .orElseThrow(() -> {
                    log.warn("악보를 찾을 수 없음 - sheetId: {}", sheetId);
                    return new BusinessException(BusinessError.SHEET_NOT_FOUND);
                });

        // 3. 서비스 악보인지 확인
        if (sheet.getSourceType() != SheetMusic.SourceType.SYSTEM) {
            log.warn("서비스 악보가 아님 - sheetId: {}, sourceType: {}", sheetId, sheet.getSourceType());
            throw new BusinessException(BusinessError.NOT_SERVICE_SHEET);
        }

        // 4. 이미 담았는지 중복 체크
        Optional<UserSheet> existingUserSheet = userSheetRepository.findByUserIdAndSheetId(userId, sheetId);
        if (existingUserSheet.isPresent()) {
            log.warn("이미 담은 악보 - userId: {}, sheetId: {}", userId, sheetId);
            throw new BusinessException(BusinessError.SHEET_ALREADY_ADDED);
        }

        // 5. UserSheet 생성 및 저장
        UserSheet userSheet = UserSheet.builder()
                .user(user)
                .sheet(sheet)
                .practiceTime(0)
                .endMeasure(null)
                .progressRate((short) 0)
                .deletedYes(false)
                .build();

        userSheetRepository.save(userSheet);

        // 6. Elasticsearch에 동기화
        syncToElasticsearch(userSheet);

        // 7. 다운로드 수 증가
        sheetMusicRepository.incrementDownload(sheetId);

        log.info("내 악보로 담기 완료 - userId: {}, sheetId: {}, userSheetId: {}",
                userId, sheetId, userSheet.getUserSheetId());
    }

    /**
     * 내 악보 삭제 (소프트 삭제)
     * - deletedYes를 true로 설정
     * - deletedAt을 현재 시간으로 설정
     */
    @Transactional
    public void deleteUserSheet(Integer userSheetId) {

        log.info("내 악보 삭제 요청 - userSheetId: {}", userSheetId);

        // 1. 사용자 악보 조회 (이미 삭제된 악보도 조회 가능해야 중복 삭제 방지)
        UserSheet userSheet = userSheetRepository.findById(userSheetId)
                .orElseThrow(() -> {
                    log.warn("사용자 악보를 찾을 수 없음 - userSheetId: {}", userSheetId);
                    return new BusinessException(BusinessError.USER_SHEET_NOT_FOUND);
                });

        // 2. 이미 삭제된 경우 체크
        if (userSheet.isDeletedYes()) {
            log.warn("이미 삭제된 악보 - userSheetId: {}", userSheetId);
            return; // 이미 삭제된 경우 그냥 반환
        }

        // 3. 소프트 삭제 처리
        userSheet.softDelete();

        // 4. Elasticsearch에 동기화 (deletedYes = true로 업데이트)
        syncToElasticsearch(userSheet);

        // 5. ADMIN 사용자인 경우 악보를 숨김 처리 및 캐시/추천 목록에서 제거
        User user = userSheet.getUser();
        if (user.getRole() == User.Role.ADMIN) {
            SheetMusic sheetMusic = userSheet.getSheet();

            log.info("ADMIN 권한 확인 - 악보 숨김 처리 시작 - sheetId: {}, userId: {}",
                    sheetMusic.getSheetId(), user.getUserId());

            // 악보를 숨김 처리
            sheetMusic.hideSheet();

            // Elasticsearch에서 문서 제거
            try {
                sheetMusicDocumentRepository.deleteById(String.valueOf(sheetMusic.getSheetId()));
                log.info("Elasticsearch에서 악보 문서 삭제 완료 - sheetId: {}", sheetMusic.getSheetId());
            } catch (Exception e) {
                log.error("Elasticsearch에서 악보 문서 삭제 실패 - sheetId: {}, error: {}",
                        sheetMusic.getSheetId(), e.getMessage(), e);
            }

            // Redis에 캐싱된 인기 악보 목록 무효화
            try {
                sheetRecommendationService.invalidatePopularSheetsCache();
                log.info("Redis 인기 악보 캐시 무효화 완료 - sheetId: {}", sheetMusic.getSheetId());
            } catch (Exception e) {
                log.error("Redis 인기 악보 캐시 무효화 실패 - sheetId: {}, error: {}",
                        sheetMusic.getSheetId(), e.getMessage(), e);
            }

            // 오늘의 추천 악보 목록 재계산
            try {
                // 1. 삭제된 악보를 추천받은 사용자 ID 목록 조회
                List<Integer> affectedUserIds = dailyRecommendationRepository.findUserIdsBySheetId(sheetMusic.getSheetId());
                log.info("삭제된 악보를 추천받은 사용자 {}명 발견 - sheetId: {}", affectedUserIds.size(), sheetMusic.getSheetId());

                // 2. 해당 사용자들의 추천을 재계산
                int successCount = 0;
                int failCount = 0;
                for (Integer affectedUserId : affectedUserIds) {
                    try {
                        todaySheetBatchService.recalculateRecommendationForUser(affectedUserId);
                        successCount++;
                    } catch (Exception e) {
                        log.error("사용자 {} 추천 재계산 실패 - sheetId: {}", affectedUserId, sheetMusic.getSheetId(), e);
                        failCount++;
                    }
                }
                log.info("오늘의 추천 악보 재계산 완료 - 성공: {}명, 실패: {}명", successCount, failCount);
            } catch (Exception e) {
                log.error("오늘의 추천 악보 재계산 실패 - sheetId: {}, error: {}",
                        sheetMusic.getSheetId(), e.getMessage(), e);
            }
        }

        log.info("내 악보 삭제 완료 - userSheetId: {}", userSheetId);
    }

    /**
     * 시작 마디 계산
     * - 가장 최근 연주의 끝 마디를 반환 (연습/평가 모드 구분 없이)
     * - 연주 기록이 없으면 1 반환
     */
    private Short calculateStartMeasure(Integer userSheetId) {
        return performanceRepository.findLatestByUserSheetId(userSheetId)
                .map(performance -> performance.getEndMeasure() != null
                        ? performance.getEndMeasure()
                        : (short) 1)
                .orElse((short) 1);
    }

    /**
     * 내 악보 검색 (Elasticsearch 사용)
     * @param userId 사용자 ID
     * @param searchKeyword 검색어 (제목, 작곡가, 악기, 장르)
     * @param instrumentId 악기 ID
     * @param genreIdsStr 장르 ID 문자열 (콤마로 구분)
     * @param tierCode 티어 코드
     * @param minTierLevel 최소 티어 레벨
     * @param maxTierLevel 최대 티어 레벨
     * @param sortType 정렬 타입 (1: 최신순, 2: 진행률순, 3: 제목순)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (기본 5)
     * @return 내 악보 검색 결과
     */
    @Transactional(readOnly = true)
    public UserSheetSearchResponse searchUserSheets(
            Integer userId,
            String searchKeyword,
            Integer instrumentId,
            String genreIdsStr,
            String tierCode,
            Integer minTierLevel,
            Integer maxTierLevel,
            Integer sortType,
            Integer page,
            Integer size
    ) {
        log.info("내 악보 검색 (Elasticsearch) - userId: {}, keyword: {}, instrumentId: {}, genreIds: {}, tierCode: {}, tierLevel: {}-{}, sortType: {}, page: {}, size: {}",
                userId, searchKeyword, instrumentId, genreIdsStr, tierCode, minTierLevel, maxTierLevel, sortType, page, size);

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

        // Elasticsearch를 통한 검색
        org.springframework.data.elasticsearch.core.SearchHits<UserSheetDocument> searchHits =
                userSheetDocumentRepository.searchUserSheets(
                        userId,
                        searchKeyword,
                        instrumentId,
                        genreIds,
                        tierCode,
                        minTierLevel,
                        maxTierLevel,
                        sortType,
                        page,
                        size
                );

        // DTO 변환 (하이라이팅 포함)
        List<UserSheetSearchResponse.SheetItem> sheetItems = searchHits.getSearchHits().stream()
                .map(hit -> {
                    UserSheetDocument doc = hit.getContent();

                    // 하이라이팅 추출
                    List<String> highlightedTitle = hit.getHighlightField("title");
                    List<String> highlightedComposer = hit.getHighlightField("composer");

                    return UserSheetSearchResponse.SheetItem.builder()
                            .userSheetId(doc.getUserSheetId())
                            .title(doc.getTitle())
                            .composer(doc.getComposer())
                            .thumbnailUrl(doc.getThumbnailUrl())
                            .instrumentId(doc.getInstrumentId())
                            .genreId(doc.getGenreId())
                            .tierCode(doc.getTierCode())
                            .tierLevel(doc.getTierLevel() != null ? doc.getTierLevel().shortValue() : null)
                            .progressRate(doc.getProgressRate() != null ? doc.getProgressRate().intValue() : 0)
                            .highlightedTitle(highlightedTitle)
                            .highlightedComposer(highlightedComposer)
                            .build();
                })
                .collect(Collectors.toList());

        // 페이지네이션 정보 계산
        int totalCount = (int) searchHits.getTotalHits();
        int pageSize = (size != null && size > 0) ? size : 5;
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        log.info("내 악보 검색 완료 - 결과: {}건 (전체: {}건, 페이지: {}/{})", sheetItems.size(), totalCount, page, totalPages);

        return UserSheetSearchResponse.builder()
                .totalCount(totalCount)
                .page(page)
                .size(pageSize)
                .totalPages(totalPages)
                .sheetList(sheetItems)
                .build();
    }

    /**
     * 악보 편곡
     * @param userId 사용자 ID
     * @param userSheetId 원본 사용자 악보 ID
     * @param style 편곡 스타일 (클라이언트가 선택한 옵션)
     * @return 편곡된 악보 정보
     */
    @Transactional
    public void arrangeUserSheet(Integer userId, Integer userSheetId, String style, String xmlUrl) {
        log.info("악보 편곡 요청 - userId: {}, userSheetId: {}, style: {}", userId, userSheetId, style);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음 - userId: {}", userId);
                    return new BusinessException(BusinessError.USER_NOT_FOUND);
                });

        // 2. 원본 UserSheet 조회
        UserSheet originUserSheet = userSheetRepository.findById(userSheetId)
                .orElseThrow(() -> {
                    log.warn("사용자 악보를 찾을 수 없음 - userSheetId: {}", userSheetId);
                    return new BusinessException(BusinessError.USER_SHEET_NOT_FOUND);
                });

        // 3. 권한 확인 (본인 악보인지)
        if (!originUserSheet.getUser().getUserId().equals(userId)) {
            log.warn("권한 없음 - userId: {}, userSheetId owner: {}", userId, originUserSheet.getUser().getUserId());
            throw new BusinessException(BusinessError.FORBIDDEN);
        }

        SheetMusic originSheet = originUserSheet.getSheet();

        // 4. 최초 원본 악보 찾기 (편곡의 편곡이어도 최초 원본 제목 사용)
        SheetMusic rootOriginSheet = findRootOriginSheet(originSheet);

        log.info("원본 악보 - sheetId: {}, title: {}", originSheet.getSheetId(), originSheet.getTitle());
        log.info("최초 원본 악보 - sheetId: {}, title: {}", rootOriginSheet.getSheetId(), rootOriginSheet.getTitle());

        // 5. AI 서버에 편곡 요청
        ArrangementResponse arrangementResponse = aiService.requestArrangement(style, xmlUrl);
        String arrangedXmlUrl = arrangementResponse.getS3Url();

        log.info("AI 서버 편곡 완료 - arrangedXmlUrl: {}", arrangedXmlUrl);

        // 6. 편곡된 XML 파싱
        MusicXmlParseResponse parseResult = musicXmlService.parseAndBuildPreview(arrangedXmlUrl);

        log.info("편곡된 XML 파싱 완료 - 마디수: {}", parseResult.getMaxMeasureCount());

        // 7. 제목 생성: 최초 원본 제목 + (스타일 Ver.) 형식
        String baseTitle = rootOriginSheet.getTitle() + " (" + style + " Ver.)";
        String finalTitle = generateUniqueTitle(baseTitle);

        log.info("생성된 제목 - baseTitle: {}, finalTitle: {}", baseTitle, finalTitle);

        // 8. 편곡된 SheetMusic 생성
        SheetMusic arrangedSheet = SheetMusic.builder()
                .title(finalTitle)
                .composer("크레센도")
                .instrument(originSheet.getInstrument())
                .genre(originSheet.getGenre())
                .tier(originSheet.getTier())  // 임시 설정 (편곡한 악보 난이도 요청 실패함)
                .maxMeasureCnt(parseResult.getMaxMeasureCount() != null
                        ? parseResult.getMaxMeasureCount()
                        : (short) 0)
                .downloadNumber(0)
                .visibleYes(true)
                .sourceType(SheetMusic.SourceType.ARRANGED)
                .originSheet(originSheet)  // 직전 편곡본을 originSheet로 (체인 유지)
                .style(style)
                .xmlUrl(parseResult.getFullUrl())
                .xmlUrlPreview(parseResult.getPreviewUrl())
                .thumbnailUrl(originSheet.getThumbnailUrl())  // 원본과 동일한 썸네일 사용
                .build();

        sheetMusicRepository.save(arrangedSheet);

        log.info("편곡된 악보 저장 완료 - sheetId: {}, title: {}", arrangedSheet.getSheetId(), arrangedSheet.getTitle());

        // 9. UserSheet 생성
        UserSheet arrangedUserSheet = UserSheet.builder()
                .user(user)
                .sheet(arrangedSheet)
                .practiceTime(0)
                .endMeasure(null)
                .progressRate((short) 0)
                .deletedYes(false)
                .build();

        userSheetRepository.save(arrangedUserSheet);

        // 10. Elasticsearch에 동기화
        syncToElasticsearch(arrangedUserSheet);

        log.info("편곡된 사용자 악보 생성 완료 - userSheetId: {}", arrangedUserSheet.getUserSheetId());
    }

    /**
     * 최초 원본 악보를 재귀적으로 찾기
     * 편곡의 편곡이어도 최초 원본을 반환
     */
    private SheetMusic findRootOriginSheet(SheetMusic sheet) {
        SheetMusic current = sheet;
        while (current.getOriginSheet() != null) {
            current = current.getOriginSheet();
        }
        return current;
    }

    /**
     * 내 악보 자동완성 (Elasticsearch 사용)
     * @param userId 사용자 ID
     * @param keyword 검색어
     * @return 자동완성 결과 (제목 문자열 리스트)
     */
    @Transactional(readOnly = true)
    public List<String> autocompleteUserSheets(Integer userId, String keyword) {
        log.info("내 악보 자동완성 (Elasticsearch) - userId: {}, keyword: {}", userId, keyword);

        // Elasticsearch를 통한 자동완성 검색 (제한 없이 모든 결과 반환)
        org.springframework.data.elasticsearch.core.SearchHits<UserSheetDocument> searchHits =
                userSheetDocumentRepository.autocompleteUserSheets(userId, keyword, 1000);

        // 제목 문자열 리스트로 변환
        List<String> suggestions = searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getTitle())
                .collect(Collectors.toList());

        log.info("내 악보 자동완성 완료 - 결과: {}건", suggestions.size());

        return suggestions;
    }

    /**
     * 중복되지 않는 제목 생성
     */
    private String generateUniqueTitle(String baseTitle) {
        String candidateTitle = baseTitle;
        int suffix = 1;

        while (sheetMusicRepository.existsByTitle(candidateTitle)) {
            candidateTitle = baseTitle + "_" + suffix;
            suffix++;
        }

        return candidateTitle;
    }

    /**
     * UserSheet 엔티티를 UserSheetDocument로 변환
     * @param userSheet UserSheet 엔티티
     * @return UserSheetDocument
     */
    private UserSheetDocument convertToDocument(UserSheet userSheet) {
        SheetMusic sheet = userSheet.getSheet();

        return UserSheetDocument.builder()
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
    }

    /**
     * UserSheet를 Elasticsearch에 동기화
     * @param userSheet UserSheet 엔티티
     */
    private void syncToElasticsearch(UserSheet userSheet) {
        try {
            UserSheetDocument document = convertToDocument(userSheet);
            userSheetDocumentRepository.save(document);
            log.info("Elasticsearch 동기화 완료 - userSheetId: {}", userSheet.getUserSheetId());
        } catch (Exception e) {
            log.error("Elasticsearch 동기화 실패 - userSheetId: {}, error: {}",
                    userSheet.getUserSheetId(), e.getMessage(), e);
            // Elasticsearch 동기화 실패가 전체 트랜잭션을 롤백하지 않도록 예외를 삼킴
        }
    }

    /**
     * Elasticsearch에서 UserSheet 삭제
     * @param userSheetId UserSheet ID
     */
    private void deleteFromElasticsearch(Integer userSheetId) {
        try {
            userSheetDocumentRepository.deleteById(String.valueOf(userSheetId));
            log.info("Elasticsearch에서 문서 삭제 완료 - userSheetId: {}", userSheetId);
        } catch (Exception e) {
            log.error("Elasticsearch에서 문서 삭제 실패 - userSheetId: {}, error: {}",
                    userSheetId, e.getMessage(), e);
            // Elasticsearch 삭제 실패가 전체 트랜잭션을 롤백하지 않도록 예외를 삼킴
        }
    }

    /**
     * 기존 PostgreSQL의 모든 UserSheet를 Elasticsearch로 마이그레이션
     * 초기 데이터 동기화 또는 재색인 시 사용
     * @return 마이그레이션 성공 건수
     */
    @Transactional(readOnly = true)
    public int migrateAllToElasticsearch() {
        log.info("===== UserSheet Elasticsearch 마이그레이션 시작 =====");

        // 1. PostgreSQL에서 모든 UserSheet 조회
        List<UserSheet> allUserSheets = userSheetRepository.findAll();
        log.info("총 {}건의 UserSheet 발견", allUserSheets.size());

        int successCount = 0;
        int failCount = 0;

        // 2. 각 UserSheet를 Elasticsearch에 저장
        for (UserSheet userSheet : allUserSheets) {
            try {
                syncToElasticsearch(userSheet);
                successCount++;

                // 100건마다 진행 상황 로그
                if (successCount % 100 == 0) {
                    log.info("진행 상황: {}건 완료 / {}건 중", successCount, allUserSheets.size());
                }
            } catch (Exception e) {
                failCount++;
                log.error("마이그레이션 실패 - userSheetId: {}, error: {}",
                        userSheet.getUserSheetId(), e.getMessage(), e);
            }
        }

        log.info("===== UserSheet Elasticsearch 마이그레이션 완료 =====");
        log.info("성공: {}건, 실패: {}건, 전체: {}건", successCount, failCount, allUserSheets.size());

        return successCount;
    }
}
package com.d102.crescendo.domain.sheet.service;

import com.d102.crescendo.domain.ai.dto.response.DifficultyEvaluationResponse;
import com.d102.crescendo.domain.common.entity.Tier;
import com.d102.crescendo.domain.common.repository.TierRepository;
import com.d102.crescendo.domain.sheet.document.UserSheetDocument;
import com.d102.crescendo.domain.sheet.dto.request.UserSheetCreateRequest;
import com.d102.crescendo.domain.sheet.dto.response.MusicXmlParseResponse;
import com.d102.crescendo.domain.sheet.dto.response.UserSheetCreateResponse;
import com.d102.crescendo.domain.sheet.entity.Genre;
import com.d102.crescendo.domain.sheet.entity.Instrument;
import com.d102.crescendo.domain.sheet.entity.SheetDifficultyMetric;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import com.d102.crescendo.domain.sheet.entity.UserSheet;
import com.d102.crescendo.domain.sheet.event.SheetCreatedEvent;
import com.d102.crescendo.domain.sheet.repository.GenreRepository;
import com.d102.crescendo.domain.sheet.repository.InstrumentRepository;
import com.d102.crescendo.domain.sheet.repository.SheetDifficultyMetricRepository;
import com.d102.crescendo.domain.sheet.repository.SheetMusicRepository;
import com.d102.crescendo.domain.sheet.repository.UserSheetDocumentRepository;
import com.d102.crescendo.domain.sheet.repository.UserSheetRepository;
import com.d102.crescendo.domain.user.entity.User;
import com.d102.crescendo.domain.user.repository.UserRepository;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class SheetRegistrationService {

    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final InstrumentRepository instrumentRepository;
    private final MusicXmlService musicXmlService;
    private final SheetMusicRepository sheetMusicRepository;
    private final UserSheetRepository userSheetRepository;
    private final UserSheetDocumentRepository userSheetDocumentRepository;
    private final SheetDifficultyMetricRepository difficultyMetricRepository;
    private final ApplicationEventPublisher publisher;
    private final TierRepository tierRepository;
    private final com.d102.crescendo.domain.ai.service.AiService aiService;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public UserSheetCreateResponse registerUserSheet(Integer userId, UserSheetCreateRequest req) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));

        // 2. 장르 조회
        Genre genre = genreRepository.findById(req.getGenreId())
                .orElseThrow(() -> new BusinessException(BusinessError.GENRE_NOT_FOUND));

        // 3. 악기 조회
        Instrument instrument = instrumentRepository.findById(req.getInstrumentId())
                .orElseThrow(() -> new BusinessException(BusinessError.INSTRUMENT_NOT_FOUND));

        // 4. 클라이언트가 업로드한 XML을 파싱하고, 전체/미리보기 XML을 S3에 저장
        log.info("MusicXML 파싱 시작 - URL: {}", req.getXmlUrl());
        MusicXmlParseResponse parseResult = musicXmlService.parseAndBuildPreview(req.getXmlUrl());

        log.info("MusicXML 파싱 완료 - 제목: {}, 작곡가: {}, 마디수: {}, 전체URL: {}, 미리보기URL: {}",
                parseResult.getTitle(),
                parseResult.getComposer(),
                parseResult.getMaxMeasureCount(),
                parseResult.getFullUrl(),
                parseResult.getPreviewUrl());

        // 5. SheetMusic 엔티티 생성
        // 파싱된 정보를 우선 사용하고, 없으면 클라이언트가 보낸 정보 사용
        String finalTitle = parseResult.getTitle() != null && !parseResult.getTitle().equals("제목 없음")
                ? parseResult.getTitle()
                : req.getTitle();

        String finalComposer = parseResult.getComposer() != null && !parseResult.getComposer().equals("작곡가 미상")
                ? parseResult.getComposer()
                : req.getComposer();
        SheetMusic sheet = SheetMusic.builder()
                .title(finalTitle)
                .composer(finalComposer)
                .instrument(instrument)
                .genre(genre)
                .tier(null)
                .maxMeasureCnt((short) 0)           // ★ NOT NULL
                .downloadNumber(0)                    // ★ NOT NULL
                .visibleYes(true)
                .sourceType(user.getRole() == User.Role.ADMIN
                        ? SheetMusic.SourceType.SYSTEM
                        : SheetMusic.SourceType.USER)  // ADMIN이면 SYSTEM, 아니면 USER
                .xmlUrl(parseResult.getFullUrl())          // S3에 저장된 전체 XML URL
                .xmlUrlPreview(parseResult.getPreviewUrl()) // S3에 저장된 미리보기 XML URL
                .maxMeasureCnt(parseResult.getMaxMeasureCount() != null
                        ? parseResult.getMaxMeasureCount().shortValue()
                        : 0)  // Integer를 Short로 변환
                .build();

        sheet = sheetMusicRepository.save(sheet);
        log.info("SheetMusic 저장 완료 - sheetId: {}", sheet.getSheetId());

        // 5-1. AI 서버로 난이도 평가 요청
        log.info("난이도 평가 요청 시작: sheetId={}", sheet.getSheetId());
        try {
            DifficultyEvaluationResponse difficultyResponse =
                    aiService.requestDifficultyEvaluation(parseResult.getFullUrl());

            // 5-1-1. Tier 업데이트
            if (difficultyResponse.getLevel() != null) {
                Tier tier = tierRepository.findById(difficultyResponse.getLevel())
                        .orElse(null);
                sheet.updateTier(tier);
                sheet = sheetMusicRepository.save(sheet);
                log.info("SheetMusic Tier 업데이트 완료 - tierId: {}", difficultyResponse.getLevel());
            }

            // 5-1-2. SheetDifficultyMetric 저장
            if (difficultyResponse.getMetrics() != null) {
                String recommendationsJson = null;
                if (difficultyResponse.getRecommendations() != null) {
                    try {
                        recommendationsJson = objectMapper.writeValueAsString(
                                difficultyResponse.getRecommendations());
                    } catch (JsonProcessingException e) {
                        log.error("recommendations JSON 변환 실패", e);
                    }
                }

                SheetDifficultyMetric metric = SheetDifficultyMetric.builder()
                        .sheet(sheet)
                        .tempo(difficultyResponse.getMetrics().getTempo())
                        .rhythm(difficultyResponse.getMetrics().getRhythm())
                        .intervals(difficultyResponse.getMetrics().getIntervals())
                        .harmony(difficultyResponse.getMetrics().getHarmony())
                        .technique(difficultyResponse.getMetrics().getTechnique())
                        .length(difficultyResponse.getMetrics().getLength())
                        .summary(difficultyResponse.getSummary())
                        .recommendations(recommendationsJson)
                        .build();

                difficultyMetricRepository.save(metric);
                log.info("SheetDifficultyMetric 저장 완료 - sheetId: {}", sheet.getSheetId());
            }

        } catch (Exception e) {
            log.error("난이도 평가 처리 중 에러 발생 - sheetId: {}", sheet.getSheetId(), e);
            // 에러가 나도 악보 등록은 계속 진행
        }

        // 5-2. 이벤트 발행 -> SheetMusicDocument 생성
        SheetCreatedEvent event = SheetCreatedEvent.from(sheet);
        publisher.publishEvent(event);
        log.info("TEST: SheetCreatedEvent 발행 완료 - sheetId={}", sheet.getSheetId());

        // 6. UserSheet 엔티티 생성
        UserSheet userSheet = UserSheet.builder()
                .user(user)
                .sheet(sheet)
                .practiceTime(0)
                .progressRate((short) 0)
                .deletedYes(false)
                .endMeasure((short) 1)
                .build();

        userSheet = userSheetRepository.save(userSheet);
        log.info("UserSheet 저장 완료 - userSheetId: {}, userId: {}", userSheet.getUserSheetId(), userId);

        // 6-1. Elasticsearch 동기화 (내 악보 검색용)
        syncToElasticsearch(userSheet);
        log.info("UserSheetDocument Elasticsearch 동기화 완료 - userSheetId: {}", userSheet.getUserSheetId());

        // 7. ADMIN 사용자인 경우에만 임베딩 생성 (동기)
        if (user.getRole() == User.Role.ADMIN) {
            // DB에 즉시 반영하여 임베딩 생성 시 조회 가능하도록 함
            sheetMusicRepository.flush();

            log.info("ADMIN 악보 - 동기 임베딩 생성 시작: sheetId={}", sheet.getSheetId());
            try {
                aiService.requestEmbedding(sheet.getSheetId());
                log.info("ADMIN 악보 임베딩 생성 완료: sheetId={}", sheet.getSheetId());
            } catch (Exception e) {
                log.error("ADMIN 악보 임베딩 생성 실패: sheetId={}", sheet.getSheetId(), e);
                // 에러가 나도 악보 등록은 계속 진행
            }
        } else {
            log.info("일반 사용자 악보 - 임베딩 생성 스킵: sheetId={}", sheet.getSheetId());
        }

        // 8. 응답 생성 (sheetId, userSheetId, createdAt만 반환)
        return UserSheetCreateResponse.builder()
                .sheetId(sheet.getSheetId())
                .userSheetId(userSheet.getUserSheetId())
                .createdAt(sheet.getCreatedAt())
                .build();
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
            log.info("Elasticsearch 동기화 완료 - userSheetId: {}", userSheet.getUserSheetId());
        } catch (Exception e) {
            log.error("Elasticsearch 동기화 실패 - userSheetId: {}, error: {}",
                    userSheet.getUserSheetId(), e.getMessage(), e);
            // Elasticsearch 동기화 실패가 전체 트랜잭션을 롤백하지 않도록 예외를 삼킴
        }
    }
}

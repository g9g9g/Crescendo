package com.d102.crescendo.domain.user.service;

import com.d102.crescendo.domain.auth.security.JwtTokenProvider;
import com.d102.crescendo.domain.common.entity.Tier;
import com.d102.crescendo.domain.common.repository.TierRepository;
import com.d102.crescendo.domain.fcm.service.FcmService;
import com.d102.crescendo.domain.rank.entity.UserInstrumentRankDaily;
import com.d102.crescendo.domain.sheet.entity.Genre;
import com.d102.crescendo.domain.sheet.entity.Instrument;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import com.d102.crescendo.domain.sheet.entity.UserSheet;
import com.d102.crescendo.domain.sheet.repository.GenreRepository;
import com.d102.crescendo.domain.sheet.repository.InstrumentRepository;
import com.d102.crescendo.domain.sheet.repository.SheetMusicRepository;
import com.d102.crescendo.domain.sheet.repository.UserSheetRepository;
import com.d102.crescendo.domain.user.dto.request.UpdateUserInfoRequest;
import com.d102.crescendo.domain.user.dto.response.OnboardingRecommendSheetResponse;
import com.d102.crescendo.domain.user.dto.response.UserInfoResponse;
import com.d102.crescendo.domain.user.entity.*;
import com.d102.crescendo.domain.user.repository.UserGenreRepository;
import com.d102.crescendo.domain.user.repository.UserInstrumentTierRepository;
import com.d102.crescendo.domain.user.repository.UserRepository;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.d102.crescendo.domain.user.dto.request.UserSignUpRequest;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final InstrumentRepository instrumentRepository;
    private final TierRepository tierRepository;
    private final UserGenreRepository userGenreRepository;
    private final UserInstrumentTierRepository userInstrumentTierRepository;
    private final UserSheetRepository userSheetRepository;
    private final SheetMusicRepository sheetMusicRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final FcmService fcmService;

    private static final String[] ADJECTIVES = {
            "촉촉한", " 폭신한", "분홍색", "정신없는", "시끄러운", "울퉁불퉁한", "불안한", "신비로운", "깐깐한", "투명한", "궁금한"
    };
    private static final String[] NOUNS = {
            "건어물", "깡패", "수도꼭지", "파워레인저", "정숙", "솜사탕", " 소화기", "멸치액젓", "말랑카우", "비눗방울", "오징어순대"
    };

    private static final String RARE_ADJECTIVE = "전설의"; // 희귀 접두사
    private static final double RARE_PROBABILITY = 0.01; // 1% 확률

    private static final Random random = new Random();
    private static final int INITIAL_TIER_ID = 1;

    public String generateNickname() {
        String nickname;
        int maxAttempts = 100;
        int attempts = 0;

        do {
            if (attempts >= maxAttempts) {
                throw new BusinessException(BusinessError.NICKNAME_GENERATION_FAILED);
            }

            // 1% 확률로 희귀 접두사, 99% 확률로 일반 접두사
            String adjective = (random.nextDouble() < RARE_PROBABILITY)
                    ? RARE_ADJECTIVE
                    : ADJECTIVES[random.nextInt(ADJECTIVES.length)];

            String noun = NOUNS[random.nextInt(NOUNS.length)];
            int number = random.nextInt(9999);
            nickname = adjective + noun + number;
            attempts++;
        } while (userRepository.existsByNickname(nickname));

        return nickname;
    }

    @Transactional
    public void signUp(User user, UserSignUpRequest request) {
        if (request.getFavoriteGenreIds() != null && !request.getFavoriteGenreIds().isEmpty()) {
            validateAndSaveUserGenres(user, request.getFavoriteGenreIds());
        }

        if (request.getStartInstrumentId() != null) {
            createInitialUserInstrument(user, request.getStartInstrumentId());
        }

        if (request.getSheetIds() != null && !request.getSheetIds().isEmpty()) {
            addPreferredSheets(user, request.getSheetIds());
        }
    }

    private List<UserGenre> validateAndSaveUserGenres(User user, List<Integer> genreIds) {
        Set<Integer> uniqueGenreIds = new HashSet<>(genreIds);
        List<Genre> genres = genreRepository.findAllById(uniqueGenreIds);

        if (genres.size() != uniqueGenreIds.size()) {
            throw new BusinessException(BusinessError.GENRE_NOT_FOUND);
        }

        Map<Integer, Genre> genreMap = genres.stream()
                .collect(Collectors.toMap(Genre::getGenreId, Function.identity()));

        List<UserGenre> userGenres = genreIds.stream()
                .map(genreId -> {
                    Genre genre = genreMap.get(genreId);
                    UserGenreId userGenreId = new UserGenreId(user.getUserId(), genreId);
                    return UserGenre.builder()
                            .id(userGenreId)
                            .user(user)
                            .genre(genre)
                            .build();
                })
                .collect(Collectors.toList());

        return userGenreRepository.saveAll(userGenres);
    }

    private void createInitialUserInstrument(User user, Integer startInstrumentId) {
        Instrument instrument = instrumentRepository.findById(startInstrumentId)
                .orElseThrow(() -> new BusinessException(BusinessError.INSTRUMENT_NOT_FOUND));

        Tier initialTier = tierRepository.findByTierId(INITIAL_TIER_ID)
                .orElseThrow(() -> new BusinessException(BusinessError.TIER_NOT_FOUND));

        UserInstrumentTierId userInstrumentTierId = new UserInstrumentTierId(
                user.getUserId(),
                instrument.getInstrumentId()
        );

        UserInstrumentTier userInstrumentTier = UserInstrumentTier.builder()
                .id(userInstrumentTierId)
                .user(user)
                .instrument(instrument)
                .tier(initialTier)
                .exp(0)
                .practiceTime(0)
                .build();

        userInstrumentTierRepository.save(userInstrumentTier);
    }

    private void addPreferredSheets(User user, List<Integer> sheetIds) {
        Set<Integer> uniqueSheetIds = new HashSet<>(sheetIds);
        List<SheetMusic> sheets = sheetMusicRepository.findAllById(uniqueSheetIds);

        if (sheets.size() != uniqueSheetIds.size()) {
            throw new BusinessException(BusinessError.SHEET_NOT_FOUND);
        }

        Map<Integer, SheetMusic> sheetMap = sheets.stream()
                .collect(Collectors.toMap(SheetMusic::getSheetId, Function.identity()));

        List<UserSheet> userSheets = sheetIds.stream()
                .map(sheetId -> {
                    SheetMusic sheet = sheetMap.get(sheetId);
                    return UserSheet.builder()
                            .user(user)
                            .sheet(sheet)
                            .deletedYes(false)
                            .build();
                })
                .collect(Collectors.toList());

        userSheetRepository.saveAll(userSheets);
    }

    public OnboardingRecommendSheetResponse getRecommendSheets(Integer instrumentId) {
        // 악기 ID 유효성 검증
        instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new BusinessException(BusinessError.INSTRUMENT_NOT_FOUND));

        // 모든 장르 조회
        List<Genre> allGenres = genreRepository.findAll();

        // 각 장르별로 다운로드 수 기준 상위 3개씩 조회
        List<List<SheetMusic>> sheetsByGenre = allGenres.stream()
                .map(genre -> sheetMusicRepository.findTopSheetsByGenreAndInstrument(
                        genre.getGenreId(),
                        instrumentId,
                        PageRequest.of(0, 3)))
                .filter(sheets -> !sheets.isEmpty())
                .collect(Collectors.toList());

        // 장르별 악보를 골고루 섞기 (라운드 로빈 방식)
        List<SheetMusic> interleavedSheets = new ArrayList<>();
        int maxSize = sheetsByGenre.stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        for (int i = 0; i < maxSize; i++) {
            for (List<SheetMusic> genreSheets : sheetsByGenre) {
                if (i < genreSheets.size()) {
                    interleavedSheets.add(genreSheets.get(i));
                }
            }
        }

        return OnboardingRecommendSheetResponse.from(interleavedSheets);
    }

    public UserInfoResponse getUserInfo(Integer userId) {
        // DB에서 영속 상태의 User 조회 (fetch join으로 연관 엔티티 한 번에 조회)
        User user = userRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));

        // favoriteGenres 추출
        List<Integer> favoriteGenres = user.getUserGenres().stream()
                .map(userGenre -> userGenre.getGenre().getGenreId())
                .collect(Collectors.toList());

        // 각 악기별 최신 rank 정보를 Map으로 구성
        Map<Integer, Integer> instrumentRankMap = user.getUserInstrumentRankDailies().stream()
                .collect(Collectors.toMap(
                        rankDaily -> rankDaily.getInstrument().getInstrumentId(),
                        UserInstrumentRankDaily::getRank,
                        (existing, replacement) -> replacement  // 중복 시 최신 것 사용
                ));

        // instrumentTiers 구성
        List<UserInfoResponse.InstrumentTierInfo> instrumentTiers = user.getUserInstrumentTiers().stream()
                .map(userInstrumentTier -> {
                    Integer instrumentId = userInstrumentTier.getInstrument().getInstrumentId();
                    return UserInfoResponse.InstrumentTierInfo.builder()
                            .instrumentId(instrumentId)
                            .tierCode(userInstrumentTier.getTier().getTierCode())
                            .tierLevel(userInstrumentTier.getTier().getTierLevel())
                            .exp(userInstrumentTier.getExp())
                            .expToNext(userInstrumentTier.getTier().getExpToNext())
                            .practiceTime(userInstrumentTier.getPracticeTime())
                            .rank(instrumentRankMap.get(instrumentId))
                            .build();
                })
                .collect(Collectors.toList());

        // 완주한 곡 조회 (progressRate = 100, tierId 높은 순 -> bestScore 높은 순 -> 최신순, TOP 50)
        List<UserSheet> completedSheets = userSheetRepository.findCompletedSheets(userId);
        List<UserInfoResponse.CompletionInfo> completions = completedSheets.stream()
                .map(userSheet -> UserInfoResponse.CompletionInfo.builder()
                        .tierCode(userSheet.getSheet().getTier().getTierCode())
                        .tierLevel(userSheet.getSheet().getTier().getTierLevel())
                        .build())
                .collect(Collectors.toList());

        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileUrl(user.getProfileUrl())
                .favoriteGenreIds(favoriteGenres)
                .totalPracticeTime(user.getTotalPracticeTime())
                .instrumentTiers(instrumentTiers)
                .completedCount(completions.size())
                .completions(completions)
                .build();
    }

    @Transactional
    public void updateUserInfo(Integer userId, UpdateUserInfoRequest request) {
        // DB에서 영속 상태의 User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));

        // nickname 수정
        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            if (userRepository.existsByNickname(request.getNickname()) &&
                !user.getNickname().equals(request.getNickname())) {
                throw new BusinessException(BusinessError.NICKNAME_DUPLICATED);
            }
            user.updateNickname(request.getNickname());
        }

        // profileUrl 수정
        if (request.getProfileUrl() != null) {
            user.updateProfileUrl(request.getProfileUrl());
        }

        // favoriteGenres 수정
        if (request.getFavoriteGenreIds() != null) {
            // 기존 UserGenre 삭제
            userGenreRepository.deleteAll(user.getUserGenres());
            user.getUserGenres().clear();

            // 새로운 UserGenre 추가
            if (!request.getFavoriteGenreIds().isEmpty()) {
                List<UserGenre> newUserGenres = validateAndSaveUserGenres(user, request.getFavoriteGenreIds());
                user.getUserGenres().addAll(newUserGenres);
            }
        }
    }

    @Transactional
    public void deleteUser(User user, String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(BusinessError.INVALID_TOKEN);
        }

        String tokenEmail = jwtTokenProvider.getEmail(refreshToken);
        if (!tokenEmail.equals(user.getEmail())) {
            throw new BusinessException(BusinessError.TOKEN_EMAIL_MISMATCH);
        }

        String redisKey = "RT:" + user.getEmail();
        String savedRefreshToken;
        try {
            savedRefreshToken = redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.error("[WITHDRAW] 레디스 연결 에러 발생: {}", e.getMessage(), e);
            throw new BusinessException(BusinessError.REDIS_CONNECTION_FAILED);
        }

        if (savedRefreshToken == null) {
            log.info("[WITHDRAW] 레디스에 리프레시 토큰이 존재하지 않음");
            throw new BusinessException(BusinessError.TOKEN_NOT_FOUND);
        }

        if (!savedRefreshToken.equals(refreshToken)) {
            log.info("[WITHDRAW] 레디스에 저장된 리프레시 토큰과 일치하지 않음");
            throw new BusinessException(BusinessError.INVALID_TOKEN);
        }

        // UserSheets를 fetch join으로 조회(LazyInitializationException 방지하기 위해 미리 조회)
        User userWithSheets = userRepository.findByIdWithUserSheets(user.getUserId())
                .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));

        // 모든 FCM 토큰 삭제 (모든 기기에서 로그아웃)
        fcmService.deleteAllTokens(user.getUserId());

        redisTemplate.delete(redisKey);
        userWithSheets.softDelete();
        log.info("[WITHDRAW] user={} 회원 탈퇴 완료 (소프트 삭제)", user.getEmail());
    }
    
}

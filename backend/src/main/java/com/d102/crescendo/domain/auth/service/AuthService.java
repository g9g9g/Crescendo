package com.d102.crescendo.domain.auth.service;

import com.d102.crescendo.domain.auth.dto.request.GoogleLoginRequest;
import com.d102.crescendo.domain.auth.dto.response.OAuthUserInfo;
import com.d102.crescendo.domain.auth.dto.response.TokenResponse;
import com.d102.crescendo.domain.auth.security.JwtTokenProvider;
import com.d102.crescendo.domain.fcm.service.FcmService;
import com.d102.crescendo.domain.user.entity.User;
import com.d102.crescendo.domain.user.repository.UserRepository;
import com.d102.crescendo.domain.user.service.UserService;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleOAuthService googleOAuthService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final FcmService fcmService;

    @Transactional
    public TokenResponse googleLogin(GoogleLoginRequest request) {
        OAuthUserInfo userInfo = googleOAuthService.verifyIDToken(request.getIdToken());
        boolean firstLoginYn = false;
        // 삭제되지 않은 사용자만 조회 (탈퇴 후 재가입 시 새로운 계정 생성)
        User user = userRepository.findByEmailAndDeletedYes(userInfo.getEmail(), false)
                .orElseGet(() -> createGoogleUser(userInfo));

        // 만약 userGenre나 userInstrumentTier가 없다면, firstLoginYn=true
        if (user.getUserGenres() == null || user.getUserGenres().isEmpty() ||
            user.getUserInstrumentTiers() == null || user.getUserInstrumentTiers().isEmpty()) {
            firstLoginYn = true;
        }

        String accessToken = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole());

        redisTemplate.opsForValue().set(
                "RT:" + user.getEmail(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        log.info("[AUTH SERVICE] 구글 로그인 처리 완료 - firstLogin={}", firstLoginYn);
        return new TokenResponse(accessToken, refreshToken, firstLoginYn);
    }

    private User createGoogleUser(OAuthUserInfo userInfo) {

        String nickname = userService.generateNickname();

        User user = User.createOAuthUser(
                userInfo.getEmail(),
                nickname,
                User.Provider.GOOGLE,
                userInfo.getProfileUrl()
        );

        User savedUser = userRepository.save(user);

        return savedUser;
    }

    @Transactional
    public void logOut(String refreshToken, String fcmToken) {
        // 1. 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(BusinessError.INVALID_TOKEN);
        }

        if (fcmToken == null || fcmToken.isEmpty()) {
            throw new BusinessException(BusinessError.FCM_TOKEN_REQUIRED);
        }

        // 2. 이메일 추출
        String email = jwtTokenProvider.getEmail(refreshToken);
        String redisKey = "RT:" + email;

        // 3. Redis에 Refresh Token 존재 확인 및 삭제
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        if (!hasKey) {
            log.warn("[LOGOUT] Redis에 Refresh Token 없음: {}", email);
            throw new BusinessException(BusinessError.TOKEN_NOT_FOUND);
        }

        Boolean isDeleted = redisTemplate.delete(redisKey);
        if (!isDeleted) {
            throw new BusinessException(BusinessError.TOKEN_DELETE_FAIL);
        }

        // 4. FCM 토큰 제거
        User user = userRepository.findByEmailAndDeletedYes(email, false)
                .orElseThrow(() -> new BusinessException(BusinessError.USER_EMAIL_NOT_FOUND));

        fcmService.deleteTokenByValue(user.getUserId(), fcmToken);

        log.info("[LOGOUT] user={} Refresh Token & fcm Token 삭제 완료", email);
    }


    public TokenResponse reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(BusinessError.INVALID_TOKEN);
        }

        String email = jwtTokenProvider.getEmail(refreshToken);

        // 삭제되지 않은 사용자만 조회
        Optional<User> userOpt = userRepository.findByEmailAndDeletedYes(email, false);
        if (userOpt.isEmpty()) {
            throw new BusinessException(BusinessError.USER_EMAIL_NOT_FOUND);
        }

        String savedRefreshToken = redisTemplate.opsForValue().get("RT:" + email);

        if (!savedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(BusinessError.INVALID_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.generateToken(userOpt.get().getUserId(), email, userOpt.get().getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email, userOpt.get().getRole());
        redisTemplate.opsForValue().set(
                "RT:" + email,
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
        return new TokenResponse(newAccessToken, newRefreshToken, false);
    }
}

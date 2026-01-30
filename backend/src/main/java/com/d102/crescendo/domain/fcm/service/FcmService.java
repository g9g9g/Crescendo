package com.d102.crescendo.domain.fcm.service;

import com.d102.crescendo.domain.fcm.entity.FcmToken;
import com.d102.crescendo.domain.fcm.entity.NotificationType;
import com.d102.crescendo.domain.fcm.repository.FcmTokenRepository;
import com.d102.crescendo.domain.user.entity.User;
import com.d102.crescendo.domain.user.repository.UserRepository;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.d102.crescendo.global.exception.BusinessError.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Value("${fcm.test-mode:true}")
    private boolean testMode;

    @Transactional
    public void registerOrUpdateToken(Integer userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        // 같은 토큰이 이미 등록되어 있는지 확인
        FcmToken fcmToken = fcmTokenRepository.findByUserAndToken(user, token)
                .orElse(FcmToken.builder()
                        .user(user)
                        .token(token)
                        .build());

        fcmTokenRepository.save(fcmToken);
        log.info("[FCM] userId={} FCM 토큰 등록/갱신 완료", userId);
    }

    /**
     * 사용자의 특정 FCM 토큰 삭제 (로그아웃 시 사용)
     * @param userId 토큰을 삭제할 사용자 ID
     * @param token 삭제할 FCM 토큰 값
     */
    @Transactional
    public void deleteTokenByValue(Integer userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        fcmTokenRepository.findByUserAndToken(user, token).ifPresent(fcmToken -> {
            fcmTokenRepository.delete(fcmToken);
            log.info("[FCM] userId={}, token={} FCM 토큰 삭제 완료", userId, token);
        });
    }

    /**
     * 사용자의 모든 FCM 토큰 삭제 (회원 탈퇴 시 사용)
     * @param userId 토큰을 삭제할 사용자 ID
     */
    @Transactional
    public void deleteAllTokens(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        List<FcmToken> tokens = fcmTokenRepository.findAllByUser(user);
        if (!tokens.isEmpty()) {
            fcmTokenRepository.deleteAll(tokens);
            log.info("[FCM] userId={}, 총 {}개 FCM 토큰 삭제 완료", userId, tokens.size());
        }
    }

    /**
     * 특정 사용자에게 푸시 알림 전송
     * 사용자의 모든 FCM 토큰에 알림을 전송하고, 실패한 토큰은 자동으로 삭제합니다.
     * @param userId 알림을 받을 사용자 ID
     * @param title 알림 제목
     * @param body 알림 내용
     * @param type 알림 타입 (EVALUATION, TIERUP)
     * @throws BusinessException 사용자를 찾을 수 없거나 FCM 토큰이 없는 경우
     */
    public void sendNotification(Integer userId, String title, String body, NotificationType type) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        // 사용자의 모든 FCM 토큰 조회
        List<FcmToken> fcmTokens = fcmTokenRepository.findAllByUser(user);

        if (fcmTokens.isEmpty()) {
            log.warn("[FCM] userId={} FCM 토큰이 없습니다.", userId);
            throw new BusinessException(BusinessError.FCM_TOKEN_NOT_FOUND);
        }

        int successCount = 0;
        int failCount = 0;

        // 모든 토큰에 알림 전송 시도
        for (FcmToken fcmToken : fcmTokens) {
            try {
                // FCM 메시지 생성 (data 메시지 사용)
                Message message = Message.builder()
                        .setToken(fcmToken.getToken())
                        .putData("title", title)
                        .putData("body", body)
                        .putData("type", type.name())
                        .build();

                // FCM 전송
                String response = FirebaseMessaging.getInstance().send(message);
                successCount++;
                log.info("[FCM] userId={}, tokenId={} 알림 전송 성공: {}", userId, fcmToken.getId(), response);

            } catch (FirebaseMessagingException e) {
                failCount++;
                log.error("[FCM] userId={}, tokenId={} 알림 전송 실패: {}", userId, fcmToken.getId(), e.getMessage());

                // 무효한 토큰인 경우 DB에서 삭제
                if (isInvalidToken(e)) {
                    fcmTokenRepository.delete(fcmToken);
                    log.info("[FCM] userId={}, tokenId={} 무효한 토큰 삭제 완료", userId, fcmToken.getId());
                }
            }
        }

        log.info("[FCM] userId={} 알림 전송 완료 - 성공: {}, 실패: {}, 전체: {}",
                userId, successCount, failCount, fcmTokens.size());

        // 모든 토큰 전송 실패 시 예외 발생
        if (successCount == 0) {
            throw new BusinessException(BusinessError.FCM_SEND_FAILED);
        }
    }

    /**
     * Firebase 예외가 무효한 토큰으로 인한 것인지 확인
     * @param e FirebaseMessagingException
     * @return 무효한 토큰이면 true
     */
    private boolean isInvalidToken(FirebaseMessagingException e) {
        String errorMessage = e.getMessage();
        if (errorMessage == null) {
            return false;
        }

        // UNREGISTERED: 토큰이 등록되지 않음 (앱 삭제, 토큰 만료 등)
        // INVALID_ARGUMENT: 잘못된 토큰 형식
        // NOT_FOUND: 등록되지 않은 토큰
        return errorMessage.contains("UNREGISTERED") ||
               errorMessage.contains("INVALID_ARGUMENT") ||
               errorMessage.contains("Requested entity was not found") ||
               errorMessage.contains("registration-token-not-registered");
    }
}

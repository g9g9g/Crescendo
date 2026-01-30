package com.d102.crescendo.domain.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.d102.crescendo.domain.auth.dto.response.OAuthUserInfo;
import com.d102.crescendo.global.exception.BusinessError;
import com.d102.crescendo.global.exception.BusinessException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    public OAuthUserInfo verifyIDToken(String idToken) {
        log.info("[GOOGLE OAUTH] ID Token 검증 시작");
        log.debug("[GOOGLE OAUTH] clientId={}", clientId);

        if (idToken == null || idToken.isBlank() || idToken.chars().filter(ch -> ch == '.').count() != 2) {
            log.error("[GOOGLE OAUTH] 유효하지 않은 ID Token 형식");
            throw new BusinessException(BusinessError.INVALID_GOOGLE_TOKEN);
        }

        try {
            log.debug("[GOOGLE OAUTH] GoogleIdTokenVerifier 생성 중...");
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            log.debug("[GOOGLE OAUTH] ID Token 검증 요청 중...");
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                log.error("[GOOGLE OAUTH] ID Token 검증 실패 - googleIdToken is null");
                throw new BusinessException(BusinessError.INVALID_TOKEN);
            }

            Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            String profileUrl = (String) payload.get("picture");

            log.info("[GOOGLE OAUTH] ID Token 검증 성공 - email={}", email);

            return OAuthUserInfo.builder()
                    .email(email)
                    .profileUrl(profileUrl)
                    .build();

        } catch (GeneralSecurityException | IOException e) {
            log.error("[GOOGLE OAUTH] ID Token 검증 중 예외 발생", e);
            throw new BusinessException(BusinessError.INVALID_GOOGLE_TOKEN);
        }
    }
}
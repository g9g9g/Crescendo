package com.d102.crescendo.domain.auth.controller;

import com.d102.crescendo.domain.auth.dto.request.GoogleLoginRequest;
import com.d102.crescendo.domain.auth.dto.request.LogoutRequest;
import com.d102.crescendo.domain.auth.dto.request.TokenRequest;
import com.d102.crescendo.domain.auth.dto.response.TokenResponse;
import com.d102.crescendo.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "01. Auth", description = "사용자 인증 관련 API")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    @Operation(summary = "구글 로그인", description = "Google ID token을 통한 로그인/회원가입 처리")
    public ResponseEntity<TokenResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
        TokenResponse token = authService.googleLogin(request);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자의 refresh Token 및 FCM 토큰을 삭제")
    public ResponseEntity<Void> logOut(@RequestBody LogoutRequest request) {
        authService.logOut(request.getRefreshToken(), request.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "refresh token을 통해 access token 재발급")
    public ResponseEntity<TokenResponse> reissue(@RequestBody TokenRequest request) {
        TokenResponse token = authService.reissue(request.getRefreshToken());
        return ResponseEntity.ok(token);
    }

}

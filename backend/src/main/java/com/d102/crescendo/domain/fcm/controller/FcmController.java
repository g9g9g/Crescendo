package com.d102.crescendo.domain.fcm.controller;

import com.d102.crescendo.domain.auth.security.UserDetailsImpl;
import com.d102.crescendo.domain.fcm.dto.request.FcmSendRequest;
import com.d102.crescendo.domain.fcm.dto.request.FcmTokenRequest;
import com.d102.crescendo.domain.fcm.service.FcmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FCM", description = "푸시 알림 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm")
public class FcmController {

    private final FcmService fcmService;

    @PostMapping
    @Operation(summary = "FCM 토큰 등록/갱신", description = "사용자의 FCM 토큰을 등록하거나 갱신합니다.")
    public ResponseEntity<Void> registerToken(@RequestBody FcmTokenRequest request,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        fcmService.registerOrUpdateToken(userDetails.getUser().getUserId(), request.getToken());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "푸시 알림 발송", description = "특정 사용자에게 푸시 알림을 발송합니다.")
    @PostMapping("/send")
    public ResponseEntity<Void> sendNotification(@RequestBody FcmSendRequest request) {
        fcmService.sendNotification(request.getUserId(), request.getTitle(), request.getBody(), request.getType());
        return ResponseEntity.ok().build();
    }
}

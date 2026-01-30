package com.d102.crescendo.domain.user.controller;

import com.d102.crescendo.domain.auth.security.UserDetailsImpl;
import com.d102.crescendo.domain.user.dto.request.UpdateUserInfoRequest;
import com.d102.crescendo.domain.user.dto.request.UserSignUpRequest;
import com.d102.crescendo.domain.user.dto.request.TokenRequest;
import com.d102.crescendo.domain.user.dto.response.OnboardingRecommendSheetResponse;
import com.d102.crescendo.domain.user.dto.response.UserInfoResponse;
import com.d102.crescendo.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "02. User", description = "사용자 관리 관련 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/onboarding/recommend-sheets")
    @Operation(summary = "온보딩 추천 악보 리스트 조회", description = "온보딩 과정에서 사용자가 선택할 수 있는 추천 악보 리스트를 조회합니다.")
    public ResponseEntity<OnboardingRecommendSheetResponse> recommendSheets(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam Integer instrumentId) {
        OnboardingRecommendSheetResponse response = userService.getRecommendSheets(instrumentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sign-up")
    @Operation(summary = "온보딩 입력 정보 저장", description = "온보딩 단계에서 사용자가 선택한 선호 장르, 선호 악기, 추천 악보 목록을 저장합니다")
    public ResponseEntity<Void> signUp(
            @RequestBody UserSignUpRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.signUp(userDetails.getUser(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/profile")
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필을 조회합니다.")
    public ResponseEntity<UserInfoResponse> getMyInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserInfoResponse response = userService.getUserInfo(userDetails.getUser().getUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/profile")
    @Operation(summary = "내 프로필 수정", description = "선호 장르, 닉네임, 프로필 이미지를 수정합니다.")
    public ResponseEntity<Void> updateMyInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid UpdateUserInfoRequest request) {
        userService.updateUserInfo(userDetails.getUser().getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자의 계정을 삭제합니다.")
    public ResponseEntity<Void> deleteMyAccount(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody TokenRequest request) {
        userService.deleteUser(userDetails.getUser(), request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

}

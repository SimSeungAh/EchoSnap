package com.smartrecycle.backend.domain.user.controller;

import com.smartrecycle.backend.domain.user.dto.request.UpdateOnboardingRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserSettingsRequest;
import com.smartrecycle.backend.domain.user.dto.response.UserResponse;
import com.smartrecycle.backend.domain.user.service.UserService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "회원 API")
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  @Operation(
          summary = "내 정보 조회",
          description = "현재 로그인한 사용자의 정보를 조회합니다."
  )
  public ApiResponse<UserResponse> getMyInfo(
          @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    UserResponse response = userService.getMyInfo(
            userDetails.getUserId()
    );

    return ApiResponse.success(
            "내 정보 조회 성공",
            response
    );
  }

  @PatchMapping("/me")
  @Operation(
          summary = "내 정보 수정",
          description = "현재 로그인한 사용자의 닉네임을 수정합니다."
  )
  public ApiResponse<UserResponse> updateMyInfo(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody UpdateUserRequest request
  ) {
    UserResponse response = userService.updateMyInfo(
            userDetails.getUserId(),
            request
    );

    return ApiResponse.success(
            "내 정보가 수정되었습니다.",
            response
    );
  }

  @PatchMapping("/me/settings")
  @Operation(
          summary = "사용자 설정 변경",
          description = "알림 동의 여부와 위치 이용 동의 여부를 변경합니다."
  )
  public ApiResponse<UserResponse> updateSettings(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody UpdateUserSettingsRequest request
  ) {
    UserResponse response = userService.updateSettings(
            userDetails.getUserId(),
            request
    );

    return ApiResponse.success(
            "사용자 설정이 변경되었습니다.",
            response
    );
  }

  @PatchMapping("/me/onboarding")
  @Operation(
          summary = "초기 설정 완료 상태 변경",
          description = "모바일 앱의 초기 설정 완료 여부를 변경합니다."
  )
  public ApiResponse<UserResponse> updateOnboarding(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody UpdateOnboardingRequest request
  ) {
    UserResponse response = userService.updateOnboarding(
            userDetails.getUserId(),
            request
    );

    return ApiResponse.success(
            "초기 설정 상태가 변경되었습니다.",
            response
    );
  }
}

package com.smartrecycle.backend.domain.user.controller;

import com.smartrecycle.backend.domain.user.dto.request.UpdateOnboardingRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserApartmentRequest;
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
@Tag(
    name = "User",
    description = "회원 및 사용자 설정 API"
)
public class UserController {

  private final UserService userService;

  /**
   * 현재 로그인한 사용자의 정보를 조회합니다.
   */
  @GetMapping("/me")
  @Operation(
      summary = "내 정보 조회",
      description = """
                    현재 로그인한 사용자의 정보를 조회합니다.
                    
                    거주지 초기 설정이 완료된 경우
                    residenceType과 해당 거주지 정보가 함께 반환됩니다.
                    """
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

  /**
   * 현재 로그인한 사용자의 닉네임을 변경합니다.
   */
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

  /**
   * 알림 수신 및 위치 정보 이용 설정을 변경합니다.
   */
  @PatchMapping("/me/settings")
  @Operation(
      summary = "사용자 설정 변경",
      description = """
                    알림 수신 동의 여부와
                    위치 정보 이용 동의 여부를 변경합니다.
                    """
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

  /**
   * Flutter 앱 초기 설정 완료 여부를 변경합니다.
   */
  @PatchMapping("/me/onboarding")
  @Operation(
      summary = "초기 설정 완료 상태 변경",
      description = """
                    Flutter 사용자 앱의
                    초기 설정 완료 여부를 변경합니다.
                    """
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

  /**
   * 관리주체의 자체 배출 일정을 사용하는 거주 단지를
   * 설정하거나 변경합니다.
   */
  @PatchMapping("/me/apartment")
  @Operation(
      summary = "거주 단지 설정",
      description = """
                    현재 로그인한 사용자의 거주 단지를 설정하거나 변경합니다.
                    
                    관리자가 승인한 아파트 등
                    관리주체의 자체 배출 일정을 사용하는 거주지를 대상으로 합니다.
                    
                    거주 단지를 설정하면 사용자의 residenceType은
                    MANAGED_COMPLEX로 변경됩니다.
                    
                    일반주택의 주소 기반 거주지 설정은
                    주소 및 수거구역 기능에서 별도로 제공합니다.
                    """
  )
  public ApiResponse<UserResponse> updateApartment(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UpdateUserApartmentRequest request
  ) {
    UserResponse response = userService.updateApartment(
        userDetails.getUserId(),
        request
    );

    return ApiResponse.success(
        "거주 단지가 변경되었습니다.",
        response
    );
  }
}
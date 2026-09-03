package com.echosnap.backend.domain.user.controller;

import com.echosnap.backend.domain.user.dto.request.UpdateOnboardingRequest;
import com.echosnap.backend.domain.user.dto.request.UpdateUserApartmentRequest;
import com.echosnap.backend.domain.user.dto.request.UpdateUserRequest;
import com.echosnap.backend.domain.user.dto.request.UpdateUserResidenceRequest;
import com.echosnap.backend.domain.user.dto.request.UpdateUserSettingsRequest;
import com.echosnap.backend.domain.user.dto.response.UserResponse;
import com.echosnap.backend.domain.user.service.UserService;
import com.echosnap.backend.global.response.ApiResponse;
import com.echosnap.backend.global.security.service.CustomUserDetails;
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

                    MANAGED_COMPLEX 사용자는 apartment,
                    GENERAL_HOUSING 사용자는 residence 정보를 사용합니다.
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

                    관리자가 승인한 아파트, 오피스텔 등
                    관리주체의 자체 배출 일정을 사용하는 거주지를 대상으로 합니다.

                    거주 단지를 설정하면 사용자의 residenceType은
                    MANAGED_COMPLEX로 변경됩니다.

                    기존 일반주택 Residence가 존재하는 경우
                    해당 연결은 제거됩니다.
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

  /**
   * 주소 기반 지역 수거 일정을 사용하는
   * 일반주택 거주지를 설정하거나 변경합니다.
   */
  @PatchMapping("/me/residence")
  @Operation(
      summary = "일반주택 거주지 설정",
      description = """
                    현재 로그인한 사용자의
                    주소 기반 일반주택 거주지를 설정하거나 변경합니다.

                    주소 검색 API에서 사용자가 선택한
                    도로명/지번 주소, 행정구역 정보와 좌표를 저장합니다.

                    주소를 설정하면 사용자의 residenceType은
                    GENERAL_HOUSING으로 변경되고,
                    기존 Apartment 연결은 제거됩니다.

                    저장된 행정구역 정보는 이후
                    CollectionArea 수거구역 연결에 사용됩니다.
                    """
  )
  public ApiResponse<UserResponse> updateResidence(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UpdateUserResidenceRequest request
  ) {
    UserResponse response = userService.updateResidence(
        userDetails.getUserId(),
        request
    );

    return ApiResponse.success(
        "일반주택 거주지가 변경되었습니다.",
        response
    );
  }
}
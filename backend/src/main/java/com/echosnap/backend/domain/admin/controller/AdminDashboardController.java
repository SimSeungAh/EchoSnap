package com.echosnap.backend.domain.admin.controller;

import com.echosnap.backend.domain.admin.dto.AdminDashboardDtos;
import com.echosnap.backend.domain.admin.service.AdminDashboardService;
import com.echosnap.backend.global.response.ApiResponse;
import com.echosnap.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(
    name = "Admin Dashboard",
    description = "EchoSnap 관리자 대시보드 API"
)
public class AdminDashboardController {

  private final AdminDashboardService
      adminDashboardService;

  @GetMapping
  @Operation(
      summary = "관리자 대시보드 조회",
      description = """
                    관리자 메인 화면에 필요한
                    서비스 운영 현황을 조회합니다.

                    포함 정보:
                    - 전체 사용자 수
                    - 활성 사용자 수
                    - 승인 대기 거주지 수
                    - 등록 폐기물 품목 수
                    - AI 사용자 정정 검수 대기 수
                    - 최근 AI 정정 데이터
                    """
  )
  public ApiResponse<
      AdminDashboardDtos.DashboardResponse
      >
  getDashboard(
      @AuthenticationPrincipal
      CustomUserDetails userDetails
  ) {
    AdminDashboardDtos.DashboardResponse response =
        adminDashboardService.getDashboard(
            userDetails.getUserId()
        );

    return ApiResponse.success(
        "관리자 대시보드 조회 성공",
        response
    );
  }
}
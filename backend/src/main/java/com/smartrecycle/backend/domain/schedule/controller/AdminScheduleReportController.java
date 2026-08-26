package com.smartrecycle.backend.domain.schedule.controller;

import com.smartrecycle.backend.domain.schedule.dto.request.ApproveScheduleReportRequest;
import com.smartrecycle.backend.domain.schedule.dto.request.RejectScheduleReportRequest;
import com.smartrecycle.backend.domain.schedule.dto.response.AdminScheduleReportResponse;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReportStatus;
import com.smartrecycle.backend.domain.schedule.service.ScheduleReportAdminService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/schedule-reports")
@RequiredArgsConstructor
@Tag(
    name = "Admin Schedule Report",
    description = "관리자 주민 배출 일정 제보 관리 API"
)
public class AdminScheduleReportController {

  private final ScheduleReportAdminService
      scheduleReportAdminService;

  /**
   * 관리자 주민 일정 제보 목록 조회
   *
   * status를 전달하지 않으면
   * 기본적으로 PENDING 제보를 조회합니다.
   */
  @GetMapping
  @Operation(
      summary = "관리자 주민 일정 제보 목록 조회",
      description = """
                    관리자가 주민이 등록한
                    배출 일정 제보를 상태별로 조회합니다.

                    status를 생략하면
                    PENDING 상태를 기본값으로 사용합니다.

                    지원 상태:

                    PENDING
                    - 관리자 검토 대기
                    - 오래 접수된 제보부터 조회

                    APPROVED
                    - 관리자 승인 완료

                    REJECTED
                    - 관리자 거절 완료

                    각 응답에는 주민의
                    CONFIRMED / DIFFERENT 확인 수도
                    함께 포함됩니다.
                    """
  )
  public ApiResponse<List<AdminScheduleReportResponse>>
  getReports(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @RequestParam(
          required = false
      )
      ScheduleReportStatus status
  ) {
    List<AdminScheduleReportResponse> responses =
        scheduleReportAdminService
            .getReports(
                userDetails.getUserId(),
                status
            );

    return ApiResponse.success(
        "주민 일정 제보 목록을 조회했습니다.",
        responses
    );
  }

  /**
   * 관리자 주민 일정 제보 상세 조회
   */
  @GetMapping("/{reportId}")
  @Operation(
      summary = "관리자 주민 일정 제보 상세 조회",
      description = """
                    관리자가 주민 일정 제보 한 건의
                    상세 내용을 조회합니다.

                    관리자이므로 일반 사용자와 달리
                    거주 범위 제한 없이 조회할 수 있습니다.

                    다음 정보를 확인할 수 있습니다.

                    - 제보자
                    - 제보 유형
                    - 현재 상태
                    - 대상 공동주택 또는 수거구역
                    - 기존 공식 일정 ID
                    - 주민이 제보한 일정
                    - 특정 날짜 변경 여부
                    - 주민 제보 설명
                    - CONFIRMED 수
                    - DIFFERENT 수
                    - 관리자 검토 정보
                    """
  )
  public ApiResponse<AdminScheduleReportResponse>
  getReportDetail(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long reportId
  ) {
    AdminScheduleReportResponse response =
        scheduleReportAdminService
            .getReportDetail(
                userDetails.getUserId(),
                reportId
            );

    return ApiResponse.success(
        "주민 일정 제보 상세 정보를 조회했습니다.",
        response
    );
  }

  /**
   * 관리자 주민 일정 제보 승인
   */
  @PatchMapping("/{reportId}/approve")
  @Operation(
      summary = "관리자 주민 일정 제보 승인",
      description = """
                    관리자가 PENDING 상태의
                    주민 일정 제보를 승인합니다.

                    제보 유형에 따라
                    실제 공식 일정 반영 방식이 달라집니다.

                    INITIAL_SCHEDULE
                    - 기존 공식 일정이 없던 경우
                    - 새로운 공식 정기 일정을 생성합니다.

                    SCHEDULE_CORRECTION
                    - 기존 공식 일정이 실제와 다른 경우
                    - 참조된 공식 정기 일정을 수정합니다.

                    TEMPORARY_CHANGE
                    - 특정 날짜에만 일정이 변경되는 경우
                    - ScheduleException을 생성합니다.
                    - 사용자에게 표시할 publicReason이 필수입니다.

                    공식 일정 반영과
                    ScheduleReport APPROVED 처리는
                    하나의 Transaction에서 수행됩니다.

                    모든 공식 데이터 반영이 성공한 경우에만
                    제보 상태가 APPROVED로 변경됩니다.
                    """
  )
  public ApiResponse<AdminScheduleReportResponse>
  approveReport(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long reportId,

      @Valid
      @RequestBody
      ApproveScheduleReportRequest request
  ) {
    AdminScheduleReportResponse response =
        scheduleReportAdminService
            .approveReport(
                userDetails.getUserId(),
                reportId,
                request
            );

    return ApiResponse.success(
        "주민 일정 제보를 승인하고 공식 일정에 반영했습니다.",
        response
    );
  }

  /**
   * 관리자 주민 일정 제보 거절
   */
  @PatchMapping("/{reportId}/reject")
  @Operation(
      summary = "관리자 주민 일정 제보 거절",
      description = """
                    관리자가 PENDING 상태의
                    주민 일정 제보를 거절합니다.

                    거절 사유는 필수입니다.

                    거절된 제보는
                    REJECTED 상태로 변경되고
                    검토 관리자와 검토 시각이 기록됩니다.

                    공식 일정 데이터에는
                    영향을 주지 않습니다.

                    이미 APPROVED 또는 REJECTED 상태인
                    제보는 다시 처리할 수 없습니다.
                    """
  )
  public ApiResponse<AdminScheduleReportResponse>
  rejectReport(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long reportId,

      @Valid
      @RequestBody
      RejectScheduleReportRequest request
  ) {
    AdminScheduleReportResponse response =
        scheduleReportAdminService
            .rejectReport(
                userDetails.getUserId(),
                reportId,
                request
            );

    return ApiResponse.success(
        "주민 일정 제보를 거절했습니다.",
        response
    );
  }
}
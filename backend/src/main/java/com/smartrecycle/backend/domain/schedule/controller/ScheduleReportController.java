package com.smartrecycle.backend.domain.schedule.controller;

import com.smartrecycle.backend.domain.schedule.dto.request.CreateApartmentScheduleReportRequest;
import com.smartrecycle.backend.domain.schedule.dto.request.CreateGeneralHousingScheduleReportRequest;
import com.smartrecycle.backend.domain.schedule.dto.request.UpdateScheduleConfirmationRequest;
import com.smartrecycle.backend.domain.schedule.dto.response.ScheduleConfirmationResponse;
import com.smartrecycle.backend.domain.schedule.dto.response.ScheduleReportResponse;
import com.smartrecycle.backend.domain.schedule.dto.response.ScheduleReportViewResponse;
import com.smartrecycle.backend.domain.schedule.service.ScheduleConfirmationService;
import com.smartrecycle.backend.domain.schedule.service.ScheduleReportQueryService;
import com.smartrecycle.backend.domain.schedule.service.ScheduleReportService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedule-reports")
@RequiredArgsConstructor
@Tag(
    name = "Schedule Report",
    description = "주민 배출 일정 제보 및 확인 API"
)
public class ScheduleReportController {

  private final ScheduleReportService
      scheduleReportService;

  private final ScheduleConfirmationService
      scheduleConfirmationService;

  private final ScheduleReportQueryService
      scheduleReportQueryService;

  /**
   * 공동주택 주민 일정 제보 등록
   */
  @PostMapping("/apartment")
  @Operation(
      summary = "공동주택 배출 일정 제보 등록",
      description = """
                    로그인한 공동주택 사용자가
                    자신의 거주 단지에 적용되는
                    배출 일정을 제보합니다.

                    지원 제보 유형:

                    INITIAL_SCHEDULE
                    - 공식 일정이 아직 없는 경우
                      새로운 정기 일정을 제보합니다.

                    SCHEDULE_CORRECTION
                    - 기존 공식 일정이 실제와 다른 경우
                      수정 내용을 제보합니다.
                    - referenceScheduleId가 필요합니다.

                    TEMPORARY_CHANGE
                    - 특정 날짜에만 일정이 변경되거나
                      배출이 중단되는 경우 제보합니다.
                    - effectiveDate가 필요합니다.

                    apartmentId는 Request에서 받지 않고
                    로그인 사용자의 실제 거주 Apartment를
                    서버에서 직접 확인합니다.

                    등록된 제보는 즉시 공식 일정으로
                    반영되지 않고 PENDING 상태로 저장됩니다.
                    """
  )
  public ApiResponse<ScheduleReportResponse>
  createApartmentReport(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @Valid
      @RequestBody
      CreateApartmentScheduleReportRequest request
  ) {
    ScheduleReportResponse response =
        scheduleReportService
            .createApartmentReport(
                userDetails.getUserId(),
                request
            );

    return ApiResponse.success(
        "공동주택 배출 일정 제보가 등록되었습니다.",
        response
    );
  }

  /**
   * 일반주택 주민 일정 제보 등록
   */
  @PostMapping("/general-housing")
  @Operation(
      summary = "일반주택 지역 배출 일정 제보 등록",
      description = """
                    로그인한 일반주택 사용자가
                    자신의 주소에 적용되는
                    지역 배출 일정을 제보합니다.

                    지원 폐기물 종류:

                    LIFE_WASTE
                    FOOD_WASTE
                    RECYCLABLE

                    지원 제보 유형:

                    INITIAL_SCHEDULE
                    SCHEDULE_CORRECTION
                    TEMPORARY_CHANGE

                    collectionAreaId는 Request에서 받지 않고
                    사용자의 Residence에 실제 연결된
                    CollectionArea를 서버에서 사용합니다.

                    일반주택에서는
                    20:00~02:00처럼 자정을 넘기는
                    일정도 제보할 수 있습니다.

                    등록된 제보는 PENDING 상태로 저장됩니다.
                    """
  )
  public ApiResponse<ScheduleReportResponse>
  createGeneralHousingReport(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @Valid
      @RequestBody
      CreateGeneralHousingScheduleReportRequest request
  ) {
    ScheduleReportResponse response =
        scheduleReportService
            .createGeneralHousingReport(
                userDetails.getUserId(),
                request
            );

    return ApiResponse.success(
        "일반주택 배출 일정 제보가 등록되었습니다.",
        response
    );
  }

  /**
   * 주민 일정 제보 확인
   */
  @PutMapping("/{reportId}/confirmation")
  @Operation(
      summary = "주민 일정 제보 확인",
      description = """
                    동일한 일정 적용 범위의 주민이
                    제보된 일정 정보가 실제와 맞는지 확인합니다.

                    CONFIRMED
                    - 제보된 일정 정보가 실제와 맞습니다.

                    DIFFERENT
                    - 제보된 일정 정보와 실제 일정이 다릅니다.

                    공동주택은 같은 Apartment 주민,
                    일반주택은 같은 CollectionArea와
                    CollectionWasteType이 적용되는 주민만
                    확인할 수 있습니다.

                    제보자는 자신의 제보에
                    직접 확인할 수 없습니다.

                    PENDING 상태의 제보만
                    확인 값을 등록하거나 변경할 수 있습니다.
                    """
  )
  public ApiResponse<ScheduleConfirmationResponse>
  confirmReport(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long reportId,

      @Valid
      @RequestBody
      UpdateScheduleConfirmationRequest request
  ) {
    ScheduleConfirmationResponse response =
        scheduleConfirmationService
            .confirmReport(
                userDetails.getUserId(),
                reportId,
                request
            );

    return ApiResponse.success(
        "일정 제보 확인이 저장되었습니다.",
        response
    );
  }

  /**
   * 내가 작성한 제보 목록 조회
   */
  @GetMapping("/me")
  @Operation(
      summary = "내 일정 제보 목록 조회",
      description = """
                    로그인 사용자가 직접 작성한
                    주민 일정 제보를 최신순으로 조회합니다.

                    PENDING
                    APPROVED
                    REJECTED

                    상태의 제보가 모두 포함됩니다.

                    현재 거주지를 변경했더라도
                    과거에 자신이 작성한 제보는
                    계속 조회할 수 있습니다.
                    """
  )
  public ApiResponse<List<ScheduleReportViewResponse>>
  getMyReports(

      @AuthenticationPrincipal
      CustomUserDetails userDetails
  ) {
    List<ScheduleReportViewResponse> responses =
        scheduleReportQueryService
            .getMyReports(
                userDetails.getUserId()
            );

    return ApiResponse.success(
        "내 일정 제보 목록을 조회했습니다.",
        responses
    );
  }

  /**
   * 내가 확인할 수 있는 제보 목록 조회
   */
  @GetMapping("/confirmable")
  @Operation(
      summary = "확인 가능한 주민 일정 제보 조회",
      description = """
                    현재 로그인 사용자의
                    일정 적용 범위와 동일한
                    PENDING 상태의 주민 제보를 조회합니다.

                    자신이 직접 작성한 제보는 제외됩니다.

                    공동주택:
                    - 같은 Apartment

                    일반주택:
                    - 같은 CollectionArea
                    - 같은 CollectionWasteType

                    조건을 모두 만족해야 합니다.

                    응답에는 주민 확인 집계,
                    현재 사용자가 선택한 확인 값,
                    확인 가능 여부가 포함됩니다.
                    """
  )
  public ApiResponse<List<ScheduleReportViewResponse>>
  getConfirmableReports(

      @AuthenticationPrincipal
      CustomUserDetails userDetails
  ) {
    List<ScheduleReportViewResponse> responses =
        scheduleReportQueryService
            .getConfirmableReports(
                userDetails.getUserId()
            );

    return ApiResponse.success(
        "확인 가능한 일정 제보 목록을 조회했습니다.",
        responses
    );
  }

  /**
   * 일정 제보 상세 조회
   */
  @GetMapping("/{reportId}")
  @Operation(
      summary = "주민 일정 제보 상세 조회",
      description = """
                    주민 일정 제보 한 건의
                    상세 내용을 조회합니다.

                    자신의 제보는
                    현재 거주지가 변경되었더라도
                    계속 조회할 수 있습니다.

                    다른 사용자의 제보는
                    현재 자신과 동일한 일정 적용 범위에
                    속하는 경우에만 조회할 수 있습니다.

                    응답에는 다음 정보가 포함됩니다.

                    - 제보 내용
                    - 제보 상태
                    - 주민 CONFIRMED 수
                    - 주민 DIFFERENT 수
                    - 현재 사용자의 확인 값
                    - 현재 확인 가능 여부
                    """
  )
  public ApiResponse<ScheduleReportViewResponse>
  getReportDetail(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long reportId
  ) {
    ScheduleReportViewResponse response =
        scheduleReportQueryService
            .getReportDetail(
                userDetails.getUserId(),
                reportId
            );

    return ApiResponse.success(
        "일정 제보 상세 정보를 조회했습니다.",
        response
    );
  }
}
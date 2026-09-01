package com.smartrecycle.backend.domain.publicdata.controller;

import com.smartrecycle.backend.domain.publicdata.service.AdminPublicDataService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.response.PageResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/admin/public-data"
)
@RequiredArgsConstructor
@Tag(
    name = "Admin Public Data",
    description = "관리자 공공데이터 동기화 및 이력 관리 API"
)
public class AdminPublicDataController {

  private final AdminPublicDataService
      adminPublicDataService;

  /**
   * 공공데이터 수동 동기화
   */
  @PostMapping("/sync")
  @Operation(
      summary = "공공데이터 수동 동기화",
      description = """
                    행정안전부 생활쓰레기배출정보를
                    SmartRecycle DB와 동기화합니다.

                    동기화 실행 결과는
                    public_data_sync_logs 테이블에 기록됩니다.
                    """
  )
  public ApiResponse<
      AdminPublicDataService.SyncExecutionResponse
      >
  sync(
      @AuthenticationPrincipal
      CustomUserDetails userDetails
  ) {
    AdminPublicDataService.SyncExecutionResponse response =
        adminPublicDataService.sync(
            userDetails.getUserId()
        );

    return ApiResponse.success(
        "공공데이터 동기화가 완료되었습니다.",
        response
    );
  }

  /**
   * 동기화 이력 조회
   */
  @GetMapping("/sync-logs")
  @Operation(
      summary = "공공데이터 동기화 이력 조회",
      description = """
                    최근 공공데이터 동기화 결과를 조회합니다.

                    SUCCESS:
                    정상 완료

                    FAILED:
                    실패

                    RUNNING:
                    현재 실행 중
                    """
  )
  public ApiResponse<
      PageResponse<
          AdminPublicDataService.SyncLogResponse
          >
      >
  getSyncLogs(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @ParameterObject
      @PageableDefault(
          size = 20,
          sort = "startedAt",
          direction = Sort.Direction.DESC
      )
      Pageable pageable
  ) {
    PageResponse<
        AdminPublicDataService.SyncLogResponse
        > response =
        adminPublicDataService.getLogs(
            userDetails.getUserId(),
            pageable
        );

    return ApiResponse.success(
        "공공데이터 동기화 이력 조회 성공",
        response
    );
  }
}
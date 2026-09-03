package com.echosnap.backend.domain.schedule.controller;

import com.echosnap.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
import com.echosnap.backend.domain.schedule.dto.admin.AdminCollectionAreaScheduleDtos;
import com.echosnap.backend.domain.schedule.entity.CollectionAreaScheduleSourceType;
import com.echosnap.backend.domain.schedule.service.CollectionAreaScheduleAdminService;
import com.echosnap.backend.global.response.ApiResponse;
import com.echosnap.backend.global.response.PageResponse;
import com.echosnap.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/admin/collection-area-schedules"
)
@RequiredArgsConstructor
@Tag(
    name = "Admin Collection Area Schedule",
    description = "관리자 일반주택 수거구역 공식 배출 일정 API"
)
public class AdminCollectionAreaScheduleController {

  private final CollectionAreaScheduleAdminService
      collectionAreaScheduleAdminService;

  @GetMapping
  @Operation(
      summary = "일반주택 수거구역 배출 일정 목록 조회"
  )
  public ApiResponse<
      PageResponse<
          AdminCollectionAreaScheduleDtos.ScheduleResponse
          >
      >
  search(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @RequestParam(defaultValue = "")
      String keyword,

      @RequestParam(required = false)
      Long collectionAreaId,

      @RequestParam(required = false)
      CollectionWasteType wasteType,

      @RequestParam(required = false)
      CollectionAreaScheduleSourceType sourceType,

      @ParameterObject
      @PageableDefault(
          size = 20,
          sort = "updatedAt",
          direction = Sort.Direction.DESC
      )
      Pageable pageable
  ) {
    return ApiResponse.success(
        "일반주택 배출 일정 목록 조회 성공",
        collectionAreaScheduleAdminService
            .search(
                userDetails.getUserId(),
                keyword,
                collectionAreaId,
                wasteType,
                sourceType,
                pageable
            )
    );
  }

  /**
   * 관리자 배출 일정 관리 메인 목록.
   *
   * CollectionArea 원본 단위가 아니라
   * 실제 화면에 보이는 지역 그룹 단위로 페이지네이션합니다.
   */
  @GetMapping("/coverage")
  @Operation(
      summary = "지역별 일반주택 일정 등록 현황 조회",
      description = """
                    시도, 시군구, 수거구역명, 대상지역,
                    데이터 출처와 활성 상태가 같은 CollectionArea를
                    하나의 지역 그룹으로 묶어서 조회합니다.

                    페이지네이션 기준도 CollectionArea 원본 개수가 아니라
                    실제 관리자 화면에 표시되는 지역 그룹 개수입니다.

                    지역 그룹 내부의 원본 CollectionArea와 세부 일정도
                    상세 데이터로 함께 반환합니다.
                    """
  )
  public ApiResponse<
      PageResponse<
          AdminCollectionAreaScheduleDtos
              .AreaScheduleGroupResponse
          >
      >
  searchCoverage(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @RequestParam(defaultValue = "")
      String keyword,

      @RequestParam(required = false)
      CollectionAreaSourceType sourceType,

      @RequestParam(required = false)
      Boolean active,

      @ParameterObject
      @PageableDefault(
          size = 20
      )
      Pageable pageable
  ) {
    return ApiResponse.success(
        "지역별 일반주택 일정 등록 현황 조회 성공",
        collectionAreaScheduleAdminService
            .searchCoverage(
                userDetails.getUserId(),
                keyword,
                sourceType,
                active,
                pageable
            )
    );
  }

  @GetMapping("/{scheduleId}")
  @Operation(
      summary = "일반주택 배출 일정 상세 조회"
  )
  public ApiResponse<
      AdminCollectionAreaScheduleDtos.ScheduleResponse
      >
  get(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long scheduleId
  ) {
    return ApiResponse.success(
        "일반주택 배출 일정 상세 조회 성공",
        collectionAreaScheduleAdminService
            .get(
                userDetails.getUserId(),
                scheduleId
            )
    );
  }

  @PostMapping
  @Operation(
      summary = "일반주택 공식 배출 일정 등록"
  )
  public ApiResponse<
      AdminCollectionAreaScheduleDtos.ScheduleResponse
      >
  create(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @Valid
      @RequestBody
      AdminCollectionAreaScheduleDtos.CreateRequest request
  ) {
    return ApiResponse.success(
        "일반주택 배출 일정이 등록되었습니다.",
        collectionAreaScheduleAdminService
            .create(
                userDetails.getUserId(),
                request
            )
    );
  }

  @PatchMapping("/{scheduleId}")
  @Operation(
      summary = "일반주택 공식 배출 일정 수정"
  )
  public ApiResponse<
      AdminCollectionAreaScheduleDtos.ScheduleResponse
      >
  update(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long scheduleId,

      @Valid
      @RequestBody
      AdminCollectionAreaScheduleDtos.UpdateRequest request
  ) {
    return ApiResponse.success(
        "일반주택 배출 일정이 수정되었습니다.",
        collectionAreaScheduleAdminService
            .update(
                userDetails.getUserId(),
                scheduleId,
                request
            )
    );
  }

  @DeleteMapping("/{scheduleId}")
  @Operation(
      summary = "일반주택 공식 배출 일정 삭제"
  )
  public ApiResponse<Void>
  delete(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long scheduleId
  ) {
    collectionAreaScheduleAdminService
        .delete(
            userDetails.getUserId(),
            scheduleId
        );

    return ApiResponse.success(
        "일반주택 배출 일정이 삭제되었습니다."
    );
  }
}
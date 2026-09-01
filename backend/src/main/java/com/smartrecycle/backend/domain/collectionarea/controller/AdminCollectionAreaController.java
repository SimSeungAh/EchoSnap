package com.smartrecycle.backend.domain.collectionarea.controller;

import com.smartrecycle.backend.domain.collectionarea.dto.admin.AdminCollectionAreaDtos;
import com.smartrecycle.backend.domain.collectionarea.dto.response.CollectionAreaSyncResultResponse;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import com.smartrecycle.backend.domain.collectionarea.service.CollectionAreaAdminService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.response.PageResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/admin/collection-areas"
)
@RequiredArgsConstructor
@Tag(
    name = "Admin Collection Area",
    description = "관리자용 지역 수거구역·지자체 공공데이터 API"
)
public class AdminCollectionAreaController {

  private final CollectionAreaAdminService
      collectionAreaAdminService;

  /**
   * 수거구역 지역 그룹 목록 조회.
   */
  @GetMapping
  @Operation(
      summary = "관리자 수거구역 지역 그룹 목록 조회",
      description = """
                    공공데이터 수거구역과
                    관리자 직접 등록 수거구역을
                    실제 화면 표시 지역 기준으로 그룹화하여 조회합니다.

                    CollectionArea 원본 레코드가 아니라
                    지역 그룹 단위로 페이지네이션합니다.

                    sourceType:
                    MOIS_HOUSEHOLD_WASTE / MANUAL

                    active:
                    true / false

                    필터를 생략하면 전체 조회합니다.
                    """
  )
  public ApiResponse<
      PageResponse<
          AdminCollectionAreaDtos.CollectionAreaGroupResponse
          >
      >
  search(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @RequestParam(
          defaultValue = ""
      )
      String keyword,

      @RequestParam(
          required = false
      )
      CollectionAreaSourceType sourceType,

      @RequestParam(
          required = false
      )
      Boolean active,

      @ParameterObject
      @PageableDefault(
          size = 20
      )
      Pageable pageable
  ) {
    PageResponse<
        AdminCollectionAreaDtos.CollectionAreaGroupResponse
        > response =
        collectionAreaAdminService.search(
            userDetails.getUserId(),
            keyword,
            sourceType,
            active,
            pageable
        );

    return ApiResponse.success(
        "관리자 수거구역 지역 그룹 목록 조회 성공",
        response
    );
  }

  /**
   * 수거구역 지역 그룹 상세 조회.
   *
   * 목록에서 한 줄로 묶인 지역의
   * 실제 CollectionArea 원본들을 모두 반환합니다.
   */
  @GetMapping("/groups/detail")
  @Operation(
      summary = "관리자 수거구역 지역 그룹 상세 조회",
      description = """
                    수거구역 목록에서 한 줄로 표시된
                    지역 그룹에 포함된 실제 CollectionArea 원본을
                    모두 조회합니다.

                    상세 화면에서는 각 원본의
                    CollectionArea ID,
                    관리번호,
                    출처,
                    지원 폐기물,
                    기준일자,
                    활성 상태 등을 확인할 수 있습니다.
                    """
  )
  public ApiResponse<
      AdminCollectionAreaDtos.CollectionAreaGroupDetailResponse
      >
  getGroup(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @RequestParam
      String sido,

      @RequestParam
      String sigungu,

      @RequestParam
      String targetAreaName,

      @RequestParam
      CollectionAreaSourceType sourceType,

      @RequestParam
      boolean active
  ) {
    return ApiResponse.success(
        "관리자 수거구역 지역 그룹 상세 조회 성공",
        collectionAreaAdminService.getGroup(
            userDetails.getUserId(),
            sido,
            sigungu,
            targetAreaName,
            sourceType,
            active
        )
    );
  }

  /**
   * 실제 CollectionArea 원본 하나 상세 조회.
   */
  @GetMapping("/{collectionAreaId}")
  @Operation(
      summary = "관리자 수거구역 원본 상세 조회"
  )
  public ApiResponse<
      AdminCollectionAreaDtos.CollectionAreaResponse
      >
  get(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long collectionAreaId
  ) {
    return ApiResponse.success(
        "관리자 수거구역 원본 상세 조회 성공",
        collectionAreaAdminService.get(
            userDetails.getUserId(),
            collectionAreaId
        )
    );
  }

  /**
   * 수거구역 직접 추가.
   */
  @PostMapping
  @Operation(
      summary = "관리자 수거구역 직접 등록",
      description = """
                    관리자가 공공데이터에 없는
                    수거구역을 직접 등록합니다.

                    sourceType은 MANUAL로 저장됩니다.
                    """
  )
  public ApiResponse<
      AdminCollectionAreaDtos.CollectionAreaResponse
      >
  create(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @Valid
      @RequestBody
      AdminCollectionAreaDtos.CreateRequest request
  ) {
    return ApiResponse.success(
        "수거구역이 등록되었습니다.",
        collectionAreaAdminService.create(
            userDetails.getUserId(),
            request
        )
    );
  }

  /**
   * MANUAL 수거구역 수정.
   */
  @PutMapping("/{collectionAreaId}")
  @Operation(
      summary = "관리자 수거구역 수정",
      description = """
                    관리자가 직접 등록한 MANUAL 수거구역만
                    이 API로 수정할 수 있습니다.

                    공공데이터 수거구역은
                    공공데이터 동기화로 관리합니다.
                    """
  )
  public ApiResponse<
      AdminCollectionAreaDtos.CollectionAreaResponse
      >
  update(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long collectionAreaId,

      @Valid
      @RequestBody
      AdminCollectionAreaDtos.UpdateRequest request
  ) {
    return ApiResponse.success(
        "수거구역이 수정되었습니다.",
        collectionAreaAdminService.update(
            userDetails.getUserId(),
            collectionAreaId,
            request
        )
    );
  }

  /**
   * 비활성화.
   */
  @PatchMapping(
      "/{collectionAreaId}/deactivate"
  )
  @Operation(
      summary = "수거구역 비활성화"
  )
  public ApiResponse<
      AdminCollectionAreaDtos.CollectionAreaResponse
      >
  deactivate(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long collectionAreaId
  ) {
    return ApiResponse.success(
        "수거구역이 비활성화되었습니다.",
        collectionAreaAdminService.deactivate(
            userDetails.getUserId(),
            collectionAreaId
        )
    );
  }

  /**
   * 다시 활성화.
   */
  @PatchMapping(
      "/{collectionAreaId}/activate"
  )
  @Operation(
      summary = "수거구역 활성화"
  )
  public ApiResponse<
      AdminCollectionAreaDtos.CollectionAreaResponse
      >
  activate(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long collectionAreaId
  ) {
    return ApiResponse.success(
        "수거구역이 활성화되었습니다.",
        collectionAreaAdminService.activate(
            userDetails.getUserId(),
            collectionAreaId
        )
    );
  }

  /**
   * 기존 공공데이터 동기화.
   */
  @PostMapping(
      "/public-data/sync"
  )
  @Operation(
      summary = "지자체 생활쓰레기 공공데이터 동기화",
      description = """
                    행정안전부 생활쓰레기배출정보 최신 데이터를
                    SmartRecycle DB와 동기화합니다.

                    CollectionArea와
                    CollectionAreaSchedule이 함께 갱신됩니다.
                    """
  )
  public ApiResponse<
      CollectionAreaSyncResultResponse
      >
  syncPublicData(
      @AuthenticationPrincipal
      CustomUserDetails userDetails
  ) {
    CollectionAreaSyncResultResponse response =
        collectionAreaAdminService
            .syncPublicData(
                userDetails.getUserId()
            );

    return ApiResponse.success(
        "지자체 공공데이터 동기화가 완료되었습니다.",
        response
    );
  }
}
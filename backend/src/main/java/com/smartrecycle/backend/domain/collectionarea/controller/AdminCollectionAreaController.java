package com.smartrecycle.backend.domain.collectionarea.controller;

import com.smartrecycle.backend.domain.collectionarea.dto.response.CollectionAreaSyncResultResponse;
import com.smartrecycle.backend.domain.collectionarea.service.CollectionAreaAdminService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
   * 행정안전부 생활쓰레기배출정보 전체 데이터를
   * SmartRecycle DB와 동기화합니다.
   */
  @PostMapping(
      "/public-data/sync"
  )
  @Operation(
      summary = "지자체 생활쓰레기 공공데이터 동기화",
      description = """
                    관리자가 행정안전부 생활쓰레기배출정보 조회서비스의
                    최신 데이터를 SmartRecycle DB와 동기화합니다.

                    MNG_NO를 기준으로 CollectionArea를
                    신규 생성하거나 기존 데이터를 갱신합니다.

                    생활쓰레기, 음식물쓰레기, 재활용품별
                    CollectionAreaSchedule도 함께 생성 또는 갱신합니다.

                    공공데이터에서 더 이상 지원하지 않는
                    폐기물 종류의 기존 일정은 제거합니다.

                    데이터 양이 많기 때문에 요청 처리에
                    시간이 걸릴 수 있습니다.
                    """
  )
  public ApiResponse<CollectionAreaSyncResultResponse>
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
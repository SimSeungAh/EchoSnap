package com.echosnap.backend.domain.waste.controller;

import com.echosnap.backend.domain.waste.dto.response.WasteCategoryResponse;
import com.echosnap.backend.domain.waste.dto.response.WasteItemDetailResponse;
import com.echosnap.backend.domain.waste.dto.response.WasteItemSummaryResponse;
import com.echosnap.backend.domain.waste.service.WasteQueryService;
import com.echosnap.backend.global.response.ApiResponse;
import com.echosnap.backend.global.response.PageResponse;
import com.echosnap.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/waste")
@RequiredArgsConstructor
@Tag(
    name = "Waste",
    description = "사용자용 폐기물 품목 및 분리배출 가이드 API"
)
public class WasteController {

  private final WasteQueryService wasteQueryService;

  /**
   * 일반 사용자에게 노출되는 활성 폐기물 카테고리 목록을 조회합니다.
   */
  @GetMapping("/categories")
  @Operation(
      summary = "폐기물 카테고리 목록 조회",
      description = """
                    활성화된 폐기물 카테고리를
                    표시 순서대로 조회합니다.
                    """
  )
  public ApiResponse<List<WasteCategoryResponse>>
  getCategories() {

    List<WasteCategoryResponse> response =
        wasteQueryService.getCategories();

    return ApiResponse.success(
        "폐기물 카테고리 목록 조회 성공",
        response
    );
  }

  /**
   * 폐기물 품목을 이름 또는 추가 키워드로 검색합니다.
   *
   * categoryId가 있으면 해당 카테고리의 품목만 조회합니다.
   */
  @GetMapping("/items")
  @Operation(
      summary = "폐기물 품목 목록 및 검색",
      description = """
                    품목명 또는 추가 검색 키워드로
                    활성화된 폐기물 품목을 검색합니다.

                    categoryId를 전달하면 해당 카테고리의
                    품목만 조회합니다.
                    """
  )
  public ApiResponse<PageResponse<WasteItemSummaryResponse>>
  searchItems(

      @RequestParam(defaultValue = "")
      String keyword,

      @RequestParam(required = false)
      Long categoryId,

      @ParameterObject
      @PageableDefault(
          size = 20,
          sort = "name",
          direction = Sort.Direction.ASC
      )
      Pageable pageable
  ) {
    PageResponse<WasteItemSummaryResponse> response =
        wasteQueryService.searchItems(
            keyword,
            categoryId,
            pageable
        );

    return ApiResponse.success(
        "폐기물 품목 목록 조회 성공",
        response
    );
  }

  /**
   * 폐기물 품목의 기본 정보와 분리배출 가이드를 조회합니다.
   *
   * 관리형 공동주택 사용자는 해당 품목의
   * Apartment 기반 배출 일정도 함께 반환합니다.
   *
   * 일반주택 사용자는 지역 CollectionArea 일정 API를
   * 별도로 사용합니다.
   */
  @GetMapping("/items/{wasteItemId}")
  @Operation(
      summary = "폐기물 품목 상세·가이드 조회",
      description = """
                    폐기물 품목의 기본 정보와 카테고리,
                    분리배출 방법, 주의 사항,
                    체크리스트를 조회합니다.

                    MANAGED_COMPLEX 사용자는 현재 선택한
                    공동주택의 품목별 공식 일정도 함께 반환합니다.

                    GENERAL_HOUSING 사용자는
                    /api/schedules/me/general-housing API에서
                    주소·수거구역 기반 일정을 조회합니다.

                    가이드가 등록되지 않은 품목은
                    guide가 null로 반환됩니다.
                    """
  )
  public ApiResponse<WasteItemDetailResponse>
  getItemDetail(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long wasteItemId
  ) {
    WasteItemDetailResponse response =
        wasteQueryService.getItemDetail(
            userDetails.getUserId(),
            wasteItemId
        );

    return ApiResponse.success(
        "폐기물 품목 상세 조회 성공",
        response
    );
  }
}
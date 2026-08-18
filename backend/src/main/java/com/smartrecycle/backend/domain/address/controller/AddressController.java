package com.smartrecycle.backend.domain.address.controller;

import com.smartrecycle.backend.domain.address.dto.response.AddressSearchResultResponse;
import com.smartrecycle.backend.domain.address.service.AddressSearchService;
import com.smartrecycle.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(
    name = "Address",
    description = "사용자용 주소 검색 API"
)
public class AddressController {

  private final AddressSearchService addressSearchService;

  /**
   * 도로명 또는 지번 주소를 검색합니다.
   */
  @GetMapping("/search")
  @Operation(
      summary = "주소 검색",
      description = """
                    도로명 또는 지번 주소를 검색합니다.
                    카카오 주소 검색 API 결과를 SmartRecycle 내부 형식으로 변환해 반환합니다.

                    반환된 법정동/행정동 코드와 좌표는
                    이후 일반주택의 행정구역 및 수거구역 연결에 사용됩니다.
                    """
  )
  public ApiResponse<List<AddressSearchResultResponse>> searchAddresses(

      @RequestParam
      String query,

      @RequestParam(defaultValue = "1")
      int page,

      @RequestParam(defaultValue = "10")
      int size
  ) {
    List<AddressSearchResultResponse> response =
        addressSearchService.search(
            query,
            page,
            size
        );

    return ApiResponse.success(
        "주소 검색 성공",
        response
    );
  }
}
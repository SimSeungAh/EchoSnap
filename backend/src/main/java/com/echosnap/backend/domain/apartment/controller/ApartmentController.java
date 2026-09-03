package com.echosnap.backend.domain.apartment.controller;

import com.echosnap.backend.domain.apartment.dto.request.TemporaryApartmentRequest;
import com.echosnap.backend.domain.apartment.dto.response.ApartmentResponse;
import com.echosnap.backend.domain.apartment.service.ApartmentService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/apartments")
@RequiredArgsConstructor
@Tag(
        name = "Apartment",
        description = "사용자용 아파트 API"
)
public class ApartmentController {

    private final ApartmentService apartmentService;

    /**
     * 일반 사용자가 검색되지 않는 아파트를 승인 대기 상태로 임시 등록
     */
    @PostMapping("/temporary")
    @Operation(
            summary = "아파트 임시 등록",
            description = """
          검색 결과에 없는 신축 아파트를 임시 등록합니다.
          등록된 아파트는 PENDING 상태로 저장되며
          관리자가 승인한 뒤 일반 검색 결과에 표시됩니다.
          """
    )
    public ApiResponse<ApartmentResponse> registerTemporary(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @Valid
            @RequestBody
            TemporaryApartmentRequest request
    ) {
        ApartmentResponse response =
                apartmentService.registerTemporary(
                        userDetails.getUserId(),
                        request
                );

        return ApiResponse.success(
                "아파트 임시 등록이 완료되었습니다.",
                response
        );
    }

    /**
     * 승인 완료된 아파트만 검색
     */
    @GetMapping
    @Operation(
            summary = "승인된 아파트 검색",
            description = """
          아파트 이름, 도로명 주소, 지번 주소로 검색합니다.
          APPROVED 상태의 아파트만 반환됩니다.
          """
    )
    public ApiResponse<PageResponse<ApartmentResponse>>
    searchApprovedApartments(

            @RequestParam(defaultValue = "")
            String keyword,

            @ParameterObject
            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        PageResponse<ApartmentResponse> response =
                apartmentService.searchApprovedApartments(
                        keyword,
                        pageable
                );

        return ApiResponse.success(
                "아파트 목록 조회 성공",
                response
        );
    }

    /**
     * 승인 완료된 아파트 상세 정보를 조회
     */
    @GetMapping("/{apartmentId}")
    @Operation(
            summary = "승인된 아파트 상세 조회",
            description = """
          승인 완료된 아파트의 상세 정보를 조회합니다.
          승인 대기 또는 거절 상태의 아파트는 조회할 수 없습니다.
          """
    )
    public ApiResponse<ApartmentResponse>
    getApprovedApartment(

            @PathVariable
            Long apartmentId
    ) {
        ApartmentResponse response =
                apartmentService.getApprovedApartment(
                        apartmentId
                );

        return ApiResponse.success(
                "아파트 상세 조회 성공",
                response
        );
    }
}

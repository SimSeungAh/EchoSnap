package com.smartrecycle.backend.domain.apartment.controller;

import com.smartrecycle.backend.domain.apartment.dto.request.CreateApartmentRequest;
import com.smartrecycle.backend.domain.apartment.dto.request.RejectApartmentRequest;
import com.smartrecycle.backend.domain.apartment.dto.request.UpdateApartmentRequest;
import com.smartrecycle.backend.domain.apartment.dto.response.ApartmentResponse;
import com.smartrecycle.backend.domain.apartment.entity.ApartmentStatus;
import com.smartrecycle.backend.domain.apartment.service.ApartmentService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.response.PageResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/apartments")
@RequiredArgsConstructor
@Tag(
        name = "Admin Apartment",
        description = "관리자용 아파트 관리 API"
)
public class AdminApartmentController {

    private final ApartmentService apartmentService;

    /**
     * 관리자가 승인 완료 상태의 아파트를 직접 등록
     */
    @PostMapping
    @Operation(
            summary = "관리자 아파트 직접 등록",
            description = """
          관리자가 검증된 아파트를 직접 등록합니다.
          별도의 승인 과정 없이 APPROVED 상태로 생성됩니다.
          """
    )
    public ApiResponse<ApartmentResponse>
    registerApproved(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @Valid
            @RequestBody
            CreateApartmentRequest request
    ) {
        ApartmentResponse response =
                apartmentService.registerApproved(
                        userDetails.getUserId(),
                        request
                );

        return ApiResponse.success(
                "아파트 등록이 완료되었습니다.",
                response
        );
    }

    /**
     * 관리자가 승인 상태별로 아파트 목록을 조회
     */
    @GetMapping
    @Operation(
            summary = "관리자 아파트 목록 조회",
            description = """
          PENDING, APPROVED, REJECTED 상태별로
          아파트 목록을 검색합니다.

          status를 생략하면 PENDING 상태를 조회합니다.
          """
    )
    public ApiResponse<PageResponse<ApartmentResponse>>
    searchApartments(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @RequestParam(defaultValue = "PENDING")
            ApartmentStatus status,

            @RequestParam(defaultValue = "")
            String keyword,

            @ParameterObject
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponse<ApartmentResponse> response =
                apartmentService.searchApartmentsForAdmin(
                        userDetails.getUserId(),
                        status,
                        keyword,
                        pageable
                );

        return ApiResponse.success(
                "관리자 아파트 목록 조회 성공",
                response
        );
    }

    /**
     * 관리자가 승인 상태와 관계없이 아파트 상세 정보를 조회
     */
    @GetMapping("/{apartmentId}")
    @Operation(
            summary = "관리자 아파트 상세 조회",
            description = """
          승인 대기, 승인 완료, 거절 상태와 관계없이
          아파트 상세 정보를 조회합니다.
          """
    )
    public ApiResponse<ApartmentResponse>
    getApartment(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long apartmentId
    ) {
        ApartmentResponse response =
                apartmentService.getApartmentForAdmin(
                        userDetails.getUserId(),
                        apartmentId
                );

        return ApiResponse.success(
                "관리자 아파트 상세 조회 성공",
                response
        );
    }

    /**
     * 관리자가 아파트 정보를 수정
     */
    @PutMapping("/{apartmentId}")
    @Operation(
            summary = "아파트 정보 수정",
            description = """
          관리자가 아파트 이름, 주소,
          건물관리번호와 좌표를 수정합니다.
          """
    )
    public ApiResponse<ApartmentResponse>
    updateApartment(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long apartmentId,

            @Valid
            @RequestBody
            UpdateApartmentRequest request
    ) {
        ApartmentResponse response =
                apartmentService.updateApartment(
                        userDetails.getUserId(),
                        apartmentId,
                        request
                );

        return ApiResponse.success(
                "아파트 정보가 수정되었습니다.",
                response
        );
    }

    /**
     * 승인 대기 중인 아파트를 승인
     */
    @PatchMapping("/{apartmentId}/approve")
    @Operation(
            summary = "아파트 승인",
            description = """
          PENDING 상태의 아파트를 승인합니다.
          승인된 아파트는 일반 사용자 검색 결과에 표시됩니다.
          """
    )
    public ApiResponse<ApartmentResponse>
    approveApartment(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long apartmentId
    ) {
        ApartmentResponse response =
                apartmentService.approveApartment(
                        userDetails.getUserId(),
                        apartmentId
                );

        return ApiResponse.success(
                "아파트가 승인되었습니다.",
                response
        );
    }

    /**
     * 승인 대기 중인 아파트를 거절
     */
    @PatchMapping("/{apartmentId}/reject")
    @Operation(
            summary = "아파트 등록 거절",
            description = """
          PENDING 상태의 아파트 등록 요청을
          거절 사유와 함께 거절합니다.
          """
    )
    public ApiResponse<ApartmentResponse>
    rejectApartment(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long apartmentId,

            @Valid
            @RequestBody
            RejectApartmentRequest request
    ) {
        ApartmentResponse response =
                apartmentService.rejectApartment(
                        userDetails.getUserId(),
                        apartmentId,
                        request
                );

        return ApiResponse.success(
                "아파트 등록 요청이 거절되었습니다.",
                response
        );
    }
}
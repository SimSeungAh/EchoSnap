package com.smartrecycle.backend.domain.waste.controller;

import com.smartrecycle.backend.domain.waste.dto.request.CreateWasteCategoryRequest;
import com.smartrecycle.backend.domain.waste.dto.request.CreateWasteItemRequest;
import com.smartrecycle.backend.domain.waste.dto.request.SaveRecycleGuideRequest;
import com.smartrecycle.backend.domain.waste.dto.request.UpdateWasteCategoryRequest;
import com.smartrecycle.backend.domain.waste.dto.request.UpdateWasteItemRequest;
import com.smartrecycle.backend.domain.waste.dto.response.AdminWasteCategoryResponse;
import com.smartrecycle.backend.domain.waste.dto.response.AdminWasteItemResponse;
import com.smartrecycle.backend.domain.waste.dto.response.RecycleGuideResponse;
import com.smartrecycle.backend.domain.waste.service.WasteAdminService;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/waste")
@RequiredArgsConstructor
@Tag(
        name = "Admin Waste",
        description = "관리자용 폐기물 카테고리, 품목 및 분리배출 가이드 API"
)
public class AdminWasteController {

    private final WasteAdminService wasteAdminService;

    /**
     * 활성·비활성 상태를 포함한 전체 폐기물 카테고리 목록을 조회
     */
    @GetMapping("/categories")
    @Operation(
            summary = "관리자 폐기물 카테고리 목록 조회",
            description = """
                    관리자가 활성·비활성 상태를 포함한
                    전체 폐기물 카테고리를 조회합니다.
                    """
    )
    public ApiResponse<List<AdminWasteCategoryResponse>>
    getCategories(

            @AuthenticationPrincipal
            CustomUserDetails userDetails
    ) {
        List<AdminWasteCategoryResponse> response =
                wasteAdminService.getCategories(
                        userDetails.getUserId()
                );

        return ApiResponse.success(
                "관리자 폐기물 카테고리 목록 조회 성공",
                response
        );
    }

    /**
     * 새로운 폐기물 카테고리를 등록
     */
    @PostMapping("/categories")
    @Operation(
            summary = "폐기물 카테고리 등록",
            description = """
                    관리자가 새로운 폐기물 카테고리를 등록합니다.
                    신규 카테고리는 활성 상태로 생성됩니다.
                    """
    )
    public ApiResponse<AdminWasteCategoryResponse>
    createCategory(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @Valid
            @RequestBody
            CreateWasteCategoryRequest request
    ) {
        AdminWasteCategoryResponse response =
                wasteAdminService.createCategory(
                        userDetails.getUserId(),
                        request
                );

        return ApiResponse.success(
                "폐기물 카테고리가 등록되었습니다.",
                response
        );
    }

    /**
     * 폐기물 카테고리 정보를 수정
     */
    @PatchMapping("/categories/{categoryId}")
    @Operation(
            summary = "폐기물 카테고리 수정",
            description = """
                    카테고리 코드, 이름, 설명,
                    표시 순서와 활성 상태를 수정합니다.
                    """
    )
    public ApiResponse<AdminWasteCategoryResponse>
    updateCategory(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long categoryId,

            @Valid
            @RequestBody
            UpdateWasteCategoryRequest request
    ) {
        AdminWasteCategoryResponse response =
                wasteAdminService.updateCategory(
                        userDetails.getUserId(),
                        categoryId,
                        request
                );

        return ApiResponse.success(
                "폐기물 카테고리가 수정되었습니다.",
                response
        );
    }

    /**
     * 폐기물 카테고리를 비활성화
     */
    @PatchMapping("/categories/{categoryId}/deactivate")
    @Operation(
            summary = "폐기물 카테고리 비활성화",
            description = """
                    카테고리를 실제 삭제하지 않고 비활성화합니다.
                    비활성 카테고리와 그 품목은
                    일반 사용자 검색 결과에 노출되지 않습니다.
                    """
    )
    public ApiResponse<AdminWasteCategoryResponse>
    deactivateCategory(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long categoryId
    ) {
        AdminWasteCategoryResponse response =
                wasteAdminService.deactivateCategory(
                        userDetails.getUserId(),
                        categoryId
                );

        return ApiResponse.success(
                "폐기물 카테고리가 비활성화되었습니다.",
                response
        );
    }

    /**
     * 관리자가 폐기물 품목 목록을 검색
     */
    @GetMapping("/items")
    @Operation(
            summary = "관리자 폐기물 품목 목록 조회",
            description = """
                    품목명 또는 추가 검색 키워드로
                    폐기물 품목을 검색합니다.

                    categoryId로 카테고리를 필터링하고,
                    active로 활성 상태를 필터링할 수 있습니다.

                    active를 생략하면 활성·비활성 품목을
                    모두 조회합니다.
                    """
    )
    public ApiResponse<PageResponse<AdminWasteItemResponse>>
    searchItems(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @RequestParam(defaultValue = "")
            String keyword,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            Boolean active,

            @ParameterObject
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponse<AdminWasteItemResponse> response =
                wasteAdminService.searchItems(
                        userDetails.getUserId(),
                        keyword,
                        categoryId,
                        active,
                        pageable
                );

        return ApiResponse.success(
                "관리자 폐기물 품목 목록 조회 성공",
                response
        );
    }

    /**
     * 활성 상태와 관계없이 폐기물 품목 한 건을 조회
     */
    @GetMapping("/items/{wasteItemId}")
    @Operation(
            summary = "관리자 폐기물 품목 상세 조회",
            description = """
                    활성·비활성 상태와 관계없이
                    관리자가 폐기물 품목 정보를 조회합니다.
                    """
    )
    public ApiResponse<AdminWasteItemResponse>
    getItem(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long wasteItemId
    ) {
        AdminWasteItemResponse response =
                wasteAdminService.getItem(
                        userDetails.getUserId(),
                        wasteItemId
                );

        return ApiResponse.success(
                "관리자 폐기물 품목 상세 조회 성공",
                response
        );
    }

    /**
     * 새로운 폐기물 품목을 등록
     */
    @PostMapping("/items")
    @Operation(
            summary = "폐기물 품목 등록",
            description = """
                    관리자가 카테고리, 품목명,
                    검색 키워드와 대표 이미지를 등록합니다.
                    """
    )
    public ApiResponse<AdminWasteItemResponse>
    createItem(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @Valid
            @RequestBody
            CreateWasteItemRequest request
    ) {
        AdminWasteItemResponse response =
                wasteAdminService.createItem(
                        userDetails.getUserId(),
                        request
                );

        return ApiResponse.success(
                "폐기물 품목이 등록되었습니다.",
                response
        );
    }

    /**
     * 폐기물 품목 정보를 수정
     */
    @PatchMapping("/items/{wasteItemId}")
    @Operation(
            summary = "폐기물 품목 수정",
            description = """
                    폐기물 품목의 카테고리, 이름,
                    검색 키워드와 대표 이미지를 수정합니다.
                    """
    )
    public ApiResponse<AdminWasteItemResponse>
    updateItem(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long wasteItemId,

            @Valid
            @RequestBody
            UpdateWasteItemRequest request
    ) {
        AdminWasteItemResponse response =
                wasteAdminService.updateItem(
                        userDetails.getUserId(),
                        wasteItemId,
                        request
                );

        return ApiResponse.success(
                "폐기물 품목이 수정되었습니다.",
                response
        );
    }

    /**
     * 폐기물 품목을 비활성화
     */
    @PatchMapping("/items/{wasteItemId}/deactivate")
    @Operation(
            summary = "폐기물 품목 비활성화",
            description = """
                    폐기물 품목을 실제 삭제하지 않고
                    일반 사용자 검색 결과에서 숨깁니다.
                    """
    )
    public ApiResponse<AdminWasteItemResponse>
    deactivateItem(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long wasteItemId
    ) {
        AdminWasteItemResponse response =
                wasteAdminService.deactivateItem(
                        userDetails.getUserId(),
                        wasteItemId
                );

        return ApiResponse.success(
                "폐기물 품목이 비활성화되었습니다.",
                response
        );
    }

    /**
     * 품목의 분리배출 가이드와 체크리스트를 등록하거나 수정
     */
    @PutMapping("/items/{wasteItemId}/guide")
    @Operation(
            summary = "분리배출 가이드 등록 또는 수정",
            description = """
                    품목의 분리배출 요약, 배출 방법,
                    주의 사항과 체크리스트를 저장합니다.

                    기존 가이드가 없으면 새로 등록하고,
                    이미 존재하면 기존 내용을 수정합니다.
                    체크리스트는 요청한 목록으로 전체 교체됩니다.
                    """
    )
    public ApiResponse<RecycleGuideResponse>
    saveGuide(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long wasteItemId,

            @Valid
            @RequestBody
            SaveRecycleGuideRequest request
    ) {
        RecycleGuideResponse response =
                wasteAdminService.saveGuide(
                        userDetails.getUserId(),
                        wasteItemId,
                        request
                );

        return ApiResponse.success(
                "분리배출 가이드가 저장되었습니다.",
                response
        );
    }
}
package com.smartrecycle.backend.domain.image.controller;

import com.smartrecycle.backend.domain.image.dto.admin.AdminAiCorrectionDtos;
import com.smartrecycle.backend.domain.image.dto.response.ImageFileResponse;
import com.smartrecycle.backend.domain.image.entity.ImageReviewStatus;
import com.smartrecycle.backend.domain.image.service.AdminAiCorrectionService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.response.PageResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/admin/ai-corrections"
)
@RequiredArgsConstructor
@Tag(
    name = "Admin AI Correction",
    description = "관리자 AI 사용자 정정 검수 API"
)
public class AdminAiCorrectionController {

  private final AdminAiCorrectionService
      adminAiCorrectionService;

  /**
   * AI 정정 목록
   */
  @GetMapping
  @Operation(
      summary = "AI 사용자 정정 목록 조회",
      description = """
                    관리자 AI 검수 목록을 조회합니다.

                    status 미지정:
                    전체 사용자 정정

                    status=PENDING:
                    검수 대기

                    status=APPROVED:
                    승인

                    status=REJECTED:
                    거절
                    """
  )
  public ApiResponse<
      PageResponse<
          AdminAiCorrectionDtos
              .CorrectionResponse
          >
      >
  getCorrections(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @RequestParam(
          name = "status",
          required = false
      )
      ImageReviewStatus status,

      @ParameterObject
      @PageableDefault(
          size = 20,
          sort = "userCorrectedAt",
          direction = Sort.Direction.DESC
      )
      Pageable pageable
  ) {
    PageResponse<
        AdminAiCorrectionDtos
            .CorrectionResponse
        > response =
        adminAiCorrectionService
            .getCorrections(
                userDetails
                    .getUserId(),
                status,
                pageable
            );

    return ApiResponse.success(
        "AI 사용자 정정 목록 조회 성공",
        response
    );
  }

  /**
   * 정정 상세
   */
  @GetMapping("/{imageLogId}")
  @Operation(
      summary = "AI 사용자 정정 상세 조회"
  )
  public ApiResponse<
      AdminAiCorrectionDtos
          .CorrectionResponse
      >
  getCorrection(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId
  ) {
    AdminAiCorrectionDtos
        .CorrectionResponse response =
        adminAiCorrectionService
            .getCorrection(
                userDetails
                    .getUserId(),
                imageLogId
            );

    return ApiResponse.success(
        "AI 사용자 정정 상세 조회 성공",
        response
    );
  }

  /**
   * 관리자 승인
   */
  @PostMapping(
      "/{imageLogId}/approve"
  )
  @Operation(
      summary = "AI 사용자 정정 승인",
      description = """
                    사용자의 정정 결과가 올바르다고 판단하면 승인합니다.

                    승인된 데이터는 향후
                    AI 재학습 후보 데이터로 사용할 수 있습니다.
                    """
  )
  public ApiResponse<
      AdminAiCorrectionDtos
          .CorrectionResponse
      >
  approve(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId,

      @Valid
      @RequestBody
      AdminAiCorrectionDtos
          .ReviewRequest request
  ) {
    AdminAiCorrectionDtos
        .CorrectionResponse response =
        adminAiCorrectionService
            .approve(
                userDetails
                    .getUserId(),
                imageLogId,
                request.memo()
            );

    return ApiResponse.success(
        "AI 사용자 정정을 승인했습니다.",
        response
    );
  }

  /**
   * 관리자 거절
   */
  @PostMapping(
      "/{imageLogId}/reject"
  )
  @Operation(
      summary = "AI 사용자 정정 거절",
      description = """
                    잘못된 사용자 정정,
                    부적절한 이미지,
                    재학습에 사용하기 어려운 데이터 등을 거절합니다.
                    """
  )
  public ApiResponse<
      AdminAiCorrectionDtos
          .CorrectionResponse
      >
  reject(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId,

      @Valid
      @RequestBody
      AdminAiCorrectionDtos
          .ReviewRequest request
  ) {
    AdminAiCorrectionDtos
        .CorrectionResponse response =
        adminAiCorrectionService
            .reject(
                userDetails
                    .getUserId(),
                imageLogId,
                request.memo()
            );

    return ApiResponse.success(
        "AI 사용자 정정을 거절했습니다.",
        response
    );
  }

  /**
   * 관리자 검수 이미지 조회
   */
  @GetMapping(
      "/{imageLogId}/image"
  )
  @Operation(
      summary = "AI 정정 검수 이미지 조회"
  )
  public ResponseEntity<Resource>
  getCorrectionImage(
      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId
  ) {
    ImageFileResponse response =
        adminAiCorrectionService
            .getCorrectionImage(
                userDetails
                    .getUserId(),
                imageLogId
            );

    return ResponseEntity
        .ok()
        .contentType(
            response.mediaType()
        )
        .header(
            HttpHeaders.CACHE_CONTROL,
            "no-store, no-cache, must-revalidate"
        )
        .header(
            "X-Content-Type-Options",
            "nosniff"
        )
        .body(
            response.resource()
        );
  }
}
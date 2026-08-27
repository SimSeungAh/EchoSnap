package com.smartrecycle.backend.domain.image.controller;

import com.smartrecycle.backend.domain.image.dto.request.ApproveImageReviewRequest;
import com.smartrecycle.backend.domain.image.dto.request.RejectImageReviewRequest;
import com.smartrecycle.backend.domain.image.dto.response.AdminImageReviewResponse;
import com.smartrecycle.backend.domain.image.dto.response.ImageFileResponse;
import com.smartrecycle.backend.domain.image.service.AdminImageReviewService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/admin/image-reviews"
)
@RequiredArgsConstructor
@Tag(
    name = "Admin Image Review",
    description = "관리자 AI 이미지 분석 결과 검수 API"
)
public class AdminImageReviewController {

  private final AdminImageReviewService
      adminImageReviewService;

  /**
   * 관리자 검수 대기 목록 조회
   */
  @GetMapping
  @Operation(
      summary = "AI 이미지 검수 대기 목록",
      description = """
                    사용자 수정으로 인해
                    관리자 검수가 필요한
                    PENDING ImageLog를 조회합니다.

                    오래 등록된 데이터부터 반환합니다.
                    """
  )
  public ApiResponse<Page<AdminImageReviewResponse>>
  getPendingReviews(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PageableDefault(
          size = 20
      )
      Pageable pageable
  ) {
    Page<AdminImageReviewResponse> response =
        adminImageReviewService
            .getPendingReviews(
                userDetails.getUserId(),
                pageable
            );

    return ApiResponse.success(
        "AI 이미지 검수 대기 목록을 조회했습니다.",
        response
    );
  }

  /**
   * 검수 상세 조회
   */
  @GetMapping(
      "/{imageLogId}"
  )
  @Operation(
      summary = "AI 이미지 검수 상세 조회",
      description = """
                    관리자가 하나의 ImageLog에서

                    - 모바일 AI 결과
                    - 서버 AI 결과
                    - 사용자 수정 결과
                    - 검수 상태

                    를 비교하여 조회합니다.
                    """
  )
  public ApiResponse<AdminImageReviewResponse>
  getReviewDetail(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId
  ) {
    AdminImageReviewResponse response =
        adminImageReviewService
            .getReviewDetail(
                userDetails.getUserId(),
                imageLogId
            );

    return ApiResponse.success(
        "AI 이미지 검수 상세 정보를 조회했습니다.",
        response
    );
  }

  /**
   * 관리자 검수용 원본 이미지 조회
   */
  @GetMapping(
      "/{imageLogId}/file"
  )
  @Operation(
      summary = "관리자 검수용 원본 이미지 조회"
  )
  public ResponseEntity<Resource>
  getReviewImageFile(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId
  ) {
    ImageFileResponse response =
        adminImageReviewService
            .getReviewImageFile(
                userDetails.getUserId(),
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

  /**
   * 사용자 수정 AI 데이터를 승인합니다.
   */
  @PatchMapping(
      "/{imageLogId}/approve"
  )
  @Operation(
      summary = "AI 이미지 검수 승인",
      description = """
                    PENDING 상태의 사용자 수정 데이터를
                    관리자가 승인합니다.

                    승인된 ImageLog는
                    reviewStatus가 APPROVED로 변경되며

                    - 검수 관리자
                    - 검수 메모
                    - 검수 시각

                    이 함께 기록됩니다.
                    """
  )
  public ApiResponse<AdminImageReviewResponse>
  approveReview(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId,

      @Valid
      @RequestBody
      ApproveImageReviewRequest request
  ) {
    AdminImageReviewResponse response =
        adminImageReviewService
            .approveReview(
                userDetails.getUserId(),
                imageLogId,
                request
            );

    return ApiResponse.success(
        "AI 이미지 검수를 승인했습니다.",
        response
    );
  }

  /**
   * 사용자 수정 AI 데이터를 거절합니다.
   */
  @PatchMapping(
      "/{imageLogId}/reject"
  )
  @Operation(
      summary = "AI 이미지 검수 거절",
      description = """
                    PENDING 상태의 사용자 수정 데이터를
                    관리자가 거절합니다.

                    거절 사유는 필수이며
                    reviewStatus가 REJECTED로 변경됩니다.

                    검수 관리자와 검수 시각도 함께 기록됩니다.
                    """
  )
  public ApiResponse<AdminImageReviewResponse>
  rejectReview(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId,

      @Valid
      @RequestBody
      RejectImageReviewRequest request
  ) {
    AdminImageReviewResponse response =
        adminImageReviewService
            .rejectReview(
                userDetails.getUserId(),
                imageLogId,
                request
            );

    return ApiResponse.success(
        "AI 이미지 검수를 거절했습니다.",
        response
    );
  }
}
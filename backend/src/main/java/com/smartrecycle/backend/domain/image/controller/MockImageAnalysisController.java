package com.smartrecycle.backend.domain.image.controller;

import com.smartrecycle.backend.domain.image.dto.request.MockImageAnalysisRequest;
import com.smartrecycle.backend.domain.image.dto.response.MockImageAnalysisResponse;
import com.smartrecycle.backend.domain.image.service.MockImageAnalysisService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실제 AI 모델이 준비되기 전
 * Flutter와 백엔드 전체 분석 흐름을 테스트하기 위한
 * 개발 전용 Mock AI Controller입니다.
 *
 * local 프로필에서만 Bean으로 등록됩니다.
 *
 * 따라서 운영 환경에서는 이 API 자체가
 * 생성되지 않도록 분리합니다.
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Profile("local")
@Tag(
    name = "Mock AI",
    description = "개발용 Mock 이미지 AI 분석 API"
)
public class MockImageAnalysisController {

  private final MockImageAnalysisService
      mockImageAnalysisService;

  /**
   * 업로드된 이미지에 대해
   * 원하는 Mock AI 시나리오를 실행합니다.
   */
  @PostMapping(
      "/{imageLogId}/mock-analysis"
  )
  @Operation(
      summary = "Mock AI 이미지 분석",
      description = """
                    실제 TensorFlow Lite 모델이 연결되기 전에
                    이미지 분석 전체 흐름을 테스트하기 위한
                    개발용 API입니다.

                    사용 가능한 scenario:

                    HIGH_CONFIDENCE
                    - 높은 신뢰도의 정상 분석 결과
                    - confidence = 0.92
                    - MOBILE_ANALYZED

                    LOW_CONFIDENCE
                    - 낮은 신뢰도의 분석 결과
                    - confidence = 0.45
                    - SERVER_REANALYSIS_PENDING

                    ANALYSIS_FAILED
                    - 이미지 분석 실패 상황
                    - ANALYSIS_FAILED

                    HIGH_CONFIDENCE와 LOW_CONFIDENCE에서는
                    wasteItemId가 필요합니다.

                    ANALYSIS_FAILED에서는
                    wasteItemId를 생략할 수 있습니다.

                    로그인한 사용자가 직접 업로드한
                    ImageLog에만 Mock 분석을 실행할 수 있습니다.

                    이 API는 local 프로필에서만 활성화됩니다.
                    """
  )
  public ApiResponse<MockImageAnalysisResponse>
  analyze(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId,

      @Valid
      @RequestBody
      MockImageAnalysisRequest request
  ) {
    MockImageAnalysisResponse response =
        mockImageAnalysisService
            .analyze(
                userDetails.getUserId(),
                imageLogId,
                request
            );

    return ApiResponse.success(
        "Mock AI 분석을 완료했습니다.",
        response
    );
  }
}
package com.echosnap.backend.domain.image.controller;

import com.echosnap.backend.domain.image.dto.response.ImageAnalysisResultResponse;
import com.echosnap.backend.domain.image.service.ImageAnalysisResultService;
import com.echosnap.backend.global.response.ApiResponse;
import com.echosnap.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 분석 결과와
 * EchoSnap 서비스 정보를 연결하는 API입니다.
 *
 * Mock AI와 실제 AI가 모두
 * 동일한 결과 조회 API를 사용하도록 분리합니다.
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(
    name = "Image Analysis Result",
    description = "AI 분석 결과·분리배출 가이드·거주지 일정 통합 API"
)
public class ImageAnalysisResultController {

  private final ImageAnalysisResultService
      imageAnalysisResultService;

  /**
   * AI 분석 결과 화면에 필요한
   * 전체 정보를 조회합니다.
   */
  @GetMapping(
      "/{imageLogId}/result"
  )
  @Operation(
      summary = "AI 분석 통합 결과 조회",
      description = """
                    로그인한 사용자가 자신의 이미지 분석 결과를 조회합니다.

                    하나의 응답에서 다음 정보를 반환합니다.

                    - 현재 최종 폐기물 품목
                    - 결과 출처
                      · MOBILE_AI
                      · SERVER_AI
                      · USER_CORRECTION
                    - AI 신뢰도
                    - 모델 버전
                    - 서버 재분석 필요 여부
                    - 분리배출 가이드
                    - 체크리스트
                    - 사용자 거주 형태
                    - 거주지 맞춤 배출 일정

                    MANAGED_COMPLEX:
                    AI가 인식한 WasteItem의
                    공동주택 품목별 일정을 반환합니다.

                    GENERAL_HOUSING:
                    사용자의 주소에 연결된
                    생활쓰레기, 음식물쓰레기,
                    재활용품 지역 일정을 반환합니다.

                    실제 AI와 Mock AI 모두
                    동일한 결과 조회 API를 사용합니다.
                    """
  )
  public ApiResponse<ImageAnalysisResultResponse>
  getAnalysisResult(

      @AuthenticationPrincipal
      CustomUserDetails userDetails,

      @PathVariable
      Long imageLogId
  ) {
    ImageAnalysisResultResponse response =
        imageAnalysisResultService
            .getMyAnalysisResult(
                userDetails.getUserId(),
                imageLogId
            );

    return ApiResponse.success(
        "AI 분석 결과를 조회했습니다.",
        response
    );
  }
}
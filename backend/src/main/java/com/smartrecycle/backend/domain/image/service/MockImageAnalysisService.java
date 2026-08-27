package com.smartrecycle.backend.domain.image.service;

import com.smartrecycle.backend.domain.image.dto.request.MockImageAnalysisRequest;
import com.smartrecycle.backend.domain.image.dto.response.MockImageAnalysisResponse;
import com.smartrecycle.backend.domain.image.entity.ImageAnalysisStatus;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.entity.MockAnalysisScenario;
import com.smartrecycle.backend.domain.image.repository.ImageLogRepository;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;
import com.smartrecycle.backend.domain.waste.repository.WasteItemRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 AI 모델 없이
 * 이미지 분석 흐름을 테스트하기 위한 Mock Service입니다.
 *
 * Flutter 화면과 백엔드 흐름을 먼저 완성한 뒤
 * 실제 TensorFlow Lite / Python YOLO 모델로
 * 교체하기 위한 개발용 구현입니다.
 */
@Service
@RequiredArgsConstructor
public class MockImageAnalysisService {

  private static final double
      HIGH_CONFIDENCE = 0.92;

  private static final double
      LOW_CONFIDENCE = 0.45;

  private static final double
      SERVER_REANALYSIS_THRESHOLD = 0.70;

  private static final String
      MOCK_MODEL_VERSION = "mock-ai-v1";

  private final ImageLogRepository
      imageLogRepository;

  private final WasteItemRepository
      wasteItemRepository;

  /**
   * Mock AI 분석을 수행합니다.
   */
  @Transactional
  public MockImageAnalysisResponse analyze(
      Long userId,
      Long imageLogId,
      MockImageAnalysisRequest request
  ) {
    ImageLog imageLog =
        imageLogRepository
            .findByIdAndUserId(
                imageLogId,
                userId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode
                            .IMAGE_LOG_NOT_FOUND
                    )
            );

    validateAnalysisState(
        imageLog
    );

    if (
        request.scenario()
            == MockAnalysisScenario
            .ANALYSIS_FAILED
    ) {
      imageLog.markAnalysisFailed();

      return MockImageAnalysisResponse
          .failed(
              imageLog
          );
    }

    /*
     * 성공 시나리오에서는
     * 실제 WasteItem이 필요합니다.
     */
    if (
        request.wasteItemId()
            == null
    ) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }

    WasteItem wasteItem =
        wasteItemRepository
            .findByIdAndActiveTrue(
                request.wasteItemId()
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode
                            .WASTE_ITEM_NOT_FOUND
                    )
            );

    double confidence =
        switch (
            request.scenario()
            ) {
          case HIGH_CONFIDENCE ->
              HIGH_CONFIDENCE;

          case LOW_CONFIDENCE ->
              LOW_CONFIDENCE;

          case ANALYSIS_FAILED ->
              throw new IllegalStateException(
                  "이미 처리된 실패 시나리오입니다."
              );
        };

    /*
     * Mock AI를 모바일 1차 분석처럼 기록합니다.
     *
     * 이렇게 해두면 Flutter에서는
     * Mock 결과와 실제 TFLite 결과를
     * 거의 같은 흐름으로 사용할 수 있습니다.
     */
    imageLog.recordMobileAnalysis(
        wasteItem,
        confidence,
        MOCK_MODEL_VERSION
    );

    if (
        confidence
            < SERVER_REANALYSIS_THRESHOLD
    ) {
      imageLog.requestServerReanalysis();
    }

    return MockImageAnalysisResponse
        .success(
            imageLog,
            request.scenario()
        );
  }

  /**
   * 아직 분석되지 않은 업로드 이미지에만
   * Mock AI를 실행할 수 있습니다.
   */
  private void validateAnalysisState(
      ImageLog imageLog
  ) {
    if (
        imageLog.getAnalysisStatus()
            != ImageAnalysisStatus.UPLOADED
    ) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }
  }
}
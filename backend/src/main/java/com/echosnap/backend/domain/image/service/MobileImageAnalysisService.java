package com.echosnap.backend.domain.image.service;

import com.echosnap.backend.domain.image.dto.request.RecordMobileAnalysisRequest;
import com.echosnap.backend.domain.image.dto.response.MobileAnalysisResponse;
import com.echosnap.backend.domain.image.entity.ImageAnalysisStatus;
import com.echosnap.backend.domain.image.entity.ImageLog;
import com.echosnap.backend.domain.image.repository.ImageLogRepository;
import com.echosnap.backend.domain.waste.entity.WasteItem;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flutter TensorFlow Lite의
 * 1차 이미지 분석 결과를 저장하는 Service입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileImageAnalysisService {

  /**
   * 이 값보다 낮은 신뢰도라면
   * Python YOLO 서버 재분석 대상으로 분류합니다.
   *
   * 현재 MVP 기준:
   * 70%
   */
  private static final double
      SERVER_REANALYSIS_THRESHOLD = 0.70;

  private final ImageLogRepository
      imageLogRepository;

  private final AiWasteItemMappingService
      aiWasteItemMappingService;

  /**
   * 로그인한 사용자가 업로드한 이미지에
   * 모바일 TFLite 결과를 기록합니다.
   */
  @Transactional
  public MobileAnalysisResponse
  recordMobileAnalysis(
      Long userId,
      Long imageLogId,
      RecordMobileAnalysisRequest request
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

    String modelLabel =
        request.modelLabel()
            .trim();

    /*
     * TFLite classId를 DB PK로 직접 사용하지 않습니다.
     *
     * modelLabel
     * ↓
     * AiWasteItemMapping
     * ↓
     * WasteItem
     */
    WasteItem wasteItem =
        aiWasteItemMappingService
            .findWasteItemByTfliteLabel(
                modelLabel
            )
            .orElseThrow(
                () -> {
                  log.error(
                      "TFLite label mapping not found. "
                          + "imageLogId={}, "
                          + "label={}, "
                          + "modelVersion={}",
                      imageLogId,
                      modelLabel,
                      request.modelVersion()
                  );

                  return new CustomException(
                      ErrorCode
                          .AI_MAPPING_NOT_FOUND
                  );
                }
            );

    String modelVersion =
        request.modelVersion()
            .trim();

    imageLog.recordMobileAnalysis(
        wasteItem,
        request.confidence(),
        modelVersion
    );

    if (
        needsServerReanalysis(
            request.confidence()
        )
    ) {
      imageLog.requestServerReanalysis();
    }

    return MobileAnalysisResponse.from(
        imageLog
    );
  }

  /**
   * 현재 ImageLog가
   * 모바일 1차 분석을 받을 수 있는 상태인지 검증합니다.
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

  /**
   * Python YOLO 재분석 여부를 판단합니다.
   *
   * 0.70 미만:
   * 재분석 필요
   *
   * 0.70 이상:
   * 모바일 결과 사용
   */
  private boolean needsServerReanalysis(
      Double confidence
  ) {
    return confidence
        < SERVER_REANALYSIS_THRESHOLD;
  }
}
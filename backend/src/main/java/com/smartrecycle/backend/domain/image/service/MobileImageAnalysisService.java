package com.smartrecycle.backend.domain.image.service;

import com.smartrecycle.backend.domain.image.dto.request.RecordMobileAnalysisRequest;
import com.smartrecycle.backend.domain.image.dto.response.MobileAnalysisResponse;
import com.smartrecycle.backend.domain.image.entity.ImageAnalysisStatus;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.repository.ImageLogRepository;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;
import com.smartrecycle.backend.domain.waste.repository.WasteItemRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flutter TensorFlow Lite의
 * 1차 이미지 분석 결과를 저장하는 Service입니다.
 */
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

  private final WasteItemRepository
      wasteItemRepository;

  /**
   * 로그인한 사용자가 업로드한 이미지에
   * 모바일 AI 결과를 기록합니다.
   */
  @Transactional
  public MobileAnalysisResponse
  recordMobileAnalysis(
      Long userId,
      Long imageLogId,
      RecordMobileAnalysisRequest request
  ) {
    /*
     * imageLogId만 조회하지 않고
     * userId를 함께 조회합니다.
     *
     * 따라서 다른 사용자의 ImageLog에
     * AI 결과를 기록할 수 없습니다.
     */
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

    /*
     * 최초 업로드 상태에서만
     * 모바일 AI 결과를 기록합니다.
     *
     * 이미 서버 재분석이 진행되었거나
     * 결과가 확정된 ImageLog를
     * 다시 모바일 결과로 되돌리지 않습니다.
     */
    validateAnalysisState(
        imageLog
    );

    /*
     * 일반 사용자 AI 결과이므로
     * 관리자에 의해 비활성화된 품목은
     * 새 분석 결과로 저장하지 않습니다.
     */
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

    String modelVersion =
        request.modelVersion()
            .trim();

    /*
     * 먼저 모바일 AI 결과 자체를 보존합니다.
     */
    imageLog.recordMobileAnalysis(
        wasteItem,
        request.confidence(),
        modelVersion
    );

    /*
     * 신뢰도가 기준보다 낮은 경우
     * Python YOLO 재분석 대기 상태로 전환합니다.
     */
    if (
        needsServerReanalysis(
            request.confidence()
        )
    ) {
      imageLog.requestServerReanalysis();
    }

    /*
     * ImageLog는 JPA 영속 상태이므로
     * 별도의 save()가 없어도 Dirty Checking으로
     * Transaction 종료 시 UPDATE 됩니다.
     */
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
      /*
       * 현재 ErrorCode에는 이미지 분석 상태 전용 코드가
       * 아직 없으므로 공통 입력 오류를 사용합니다.
       *
       * 서버 YOLO 단계에서 AI 상태 전환 규칙을
       * 완성할 때 전용 ErrorCode로 분리할 예정입니다.
       */
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
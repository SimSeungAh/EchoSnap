package com.smartrecycle.backend.domain.image.service;

import com.smartrecycle.backend.domain.image.dto.request.CorrectImageResultRequest;
import com.smartrecycle.backend.domain.image.dto.response.ImageCorrectionResponse;
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
 * 사용자가 AI 분석 결과를 직접 수정하는 Service입니다.
 *
 * 사용자 수정 데이터는 단순히 화면 표시만 바꾸는 것이 아니라
 * ImageLog에 별도로 보존하고 관리자 검수 대상으로 전환합니다.
 */
@Service
@RequiredArgsConstructor
public class ImageCorrectionService {

  private final ImageLogRepository
      imageLogRepository;

  private final WasteItemRepository
      wasteItemRepository;

  /**
   * AI 분석 결과를 사용자가 직접 수정합니다.
   */
  @Transactional
  public ImageCorrectionResponse correctResult(
      Long userId,
      Long imageLogId,
      CorrectImageResultRequest request
  ) {
    /*
     * imageLogId만 사용하지 않고
     * 로그인한 userId까지 함께 조회합니다.
     *
     * 다른 사용자의 AI 결과를 수정하는
     * IDOR 문제를 방지합니다.
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
     * 아직 AI 결과가 하나도 없는
     * 단순 UPLOADED 이미지에는
     * 사용자 수정 결과를 기록할 수 없습니다.
     *
     * 최소한 모바일 AI 또는 서버 AI의
     * 기존 판단이 있어야
     *
     * "AI 결과 → 사용자 수정"
     *
     * 관계를 기록할 수 있습니다.
     */
    validateAnalysisExists(
        imageLog
    );

    /*
     * 사용자가 선택한 품목 역시
     * 현재 활성화된 WasteItem이어야 합니다.
     */
    WasteItem correctedWasteItem =
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

    /*
     * 사용자 수정 결과를 별도 컬럼에 저장합니다.
     *
     * 기존 모바일/서버 AI 결과는 지우지 않습니다.
     */
    imageLog.correctByUser(
        correctedWasteItem
    );

    /*
     * correctByUser() 내부에서
     *
     * userCorrectedWasteItem 저장
     * userCorrectedAt 기록
     * reviewStatus = PENDING
     *
     * 처리가 이루어집니다.
     *
     * JPA Dirty Checking으로
     * Transaction 종료 시 UPDATE 됩니다.
     */
    return ImageCorrectionResponse.from(
        imageLog
    );
  }

  /**
   * 사용자 수정의 비교 대상이 될
   * AI 분석 결과가 존재하는지 검증합니다.
   */
  private void validateAnalysisExists(
      ImageLog imageLog
  ) {
    boolean mobileResultExists =
        imageLog.getMobileWasteItem()
            != null;

    boolean serverResultExists =
        imageLog.getServerWasteItem()
            != null;

    if (
        !mobileResultExists
            && !serverResultExists
    ) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT
      );
    }
  }
}
package com.smartrecycle.backend.domain.image.dto.response;

import com.smartrecycle.backend.domain.image.entity.ImageAnalysisStatus;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.entity.ImageReviewStatus;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;

import java.time.LocalDateTime;

/**
 * 사용자가 AI 결과를 수정한 뒤 반환하는 응답입니다.
 */
public record ImageCorrectionResponse(

    Long imageLogId,

    Long originalAiWasteItemId,

    String originalAiWasteItemName,

    Long correctedWasteItemId,

    String correctedWasteItemName,

    ImageAnalysisStatus analysisStatus,

    ImageReviewStatus reviewStatus,

    LocalDateTime correctedAt

) {

  public static ImageCorrectionResponse from(
      ImageLog imageLog
  ) {
    /*
     * 사용자 수정값을 제외하고
     * AI 자체의 최종 예측 결과를 찾습니다.
     *
     * 서버 YOLO 결과가 있으면 서버 결과를 우선하고,
     * 없다면 모바일 TensorFlow Lite 결과를 사용합니다.
     */
    WasteItem originalAiWasteItem =
        imageLog.getServerWasteItem()
            != null
            ? imageLog.getServerWasteItem()
            : imageLog.getMobileWasteItem();

    WasteItem correctedWasteItem =
        imageLog.getUserCorrectedWasteItem();

    return new ImageCorrectionResponse(
        imageLog.getId(),

        originalAiWasteItem != null
            ? originalAiWasteItem.getId()
            : null,

        originalAiWasteItem != null
            ? originalAiWasteItem.getName()
            : null,

        correctedWasteItem != null
            ? correctedWasteItem.getId()
            : null,

        correctedWasteItem != null
            ? correctedWasteItem.getName()
            : null,

        imageLog.getAnalysisStatus(),
        imageLog.getReviewStatus(),
        imageLog.getUserCorrectedAt()
    );
  }
}
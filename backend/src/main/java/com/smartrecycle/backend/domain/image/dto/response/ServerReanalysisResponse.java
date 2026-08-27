package com.smartrecycle.backend.domain.image.dto.response;

import com.smartrecycle.backend.domain.image.dto.external.YoloAnalysisResponse;
import com.smartrecycle.backend.domain.image.entity.ImageAnalysisStatus;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;

/**
 * Python YOLO 서버 재분석 결과 응답입니다.
 */
public record ServerReanalysisResponse(

    Long imageLogId,

    boolean detected,

    Integer classId,

    String label,

    Double confidence,

    Integer detectionCount,

    String modelVersion,

    Long wasteItemId,

    String wasteItemName,

    ImageAnalysisStatus analysisStatus

) {

  /**
   * YOLO가 정상적으로 품목을 찾았고
   * SmartRecycle WasteItem 매핑까지 완료된 경우입니다.
   */
  public static ServerReanalysisResponse detected(
      ImageLog imageLog,
      YoloAnalysisResponse yoloResponse
  ) {
    WasteItem wasteItem =
        imageLog.getServerWasteItem();

    return new ServerReanalysisResponse(
        imageLog.getId(),
        true,
        yoloResponse.classId(),
        yoloResponse.label(),
        yoloResponse.confidence(),
        yoloResponse.detectionCount(),
        yoloResponse.modelVersion(),
        wasteItem != null
            ? wasteItem.getId()
            : null,
        wasteItem != null
            ? wasteItem.getName()
            : null,
        imageLog.getAnalysisStatus()
    );
  }

  /**
   * YOLO가 이미지에서 유효한 품목을
   * 찾지 못한 경우입니다.
   */
  public static ServerReanalysisResponse notDetected(
      ImageLog imageLog,
      YoloAnalysisResponse yoloResponse
  ) {
    return new ServerReanalysisResponse(
        imageLog.getId(),
        false,
        yoloResponse.classId(),
        yoloResponse.label(),
        yoloResponse.confidence(),
        yoloResponse.detectionCount(),
        yoloResponse.modelVersion(),
        null,
        null,
        imageLog.getAnalysisStatus()
    );
  }
}
package com.echosnap.backend.domain.image.dto.response;

import com.echosnap.backend.domain.image.entity.ImageAnalysisStatus;
import com.echosnap.backend.domain.image.entity.ImageLog;
import com.echosnap.backend.domain.image.entity.MockAnalysisScenario;
import com.echosnap.backend.domain.waste.entity.WasteItem;

/**
 * Mock AI 분석 결과 응답입니다.
 */
public record MockImageAnalysisResponse(

    Long imageLogId,

    MockAnalysisScenario scenario,

    Long wasteItemId,

    String wasteItemName,

    Double confidence,

    String modelVersion,

    ImageAnalysisStatus analysisStatus,

    boolean needsServerReanalysis

) {

  public static MockImageAnalysisResponse success(
      ImageLog imageLog,
      MockAnalysisScenario scenario
  ) {
    WasteItem wasteItem =
        imageLog.getMobileWasteItem();

    boolean needsServerReanalysis =
        imageLog.getAnalysisStatus()
            == ImageAnalysisStatus
            .SERVER_REANALYSIS_PENDING;

    return new MockImageAnalysisResponse(
        imageLog.getId(),
        scenario,
        wasteItem != null
            ? wasteItem.getId()
            : null,
        wasteItem != null
            ? wasteItem.getName()
            : null,
        imageLog.getMobileConfidence(),
        imageLog.getMobileModelVersion(),
        imageLog.getAnalysisStatus(),
        needsServerReanalysis
    );
  }

  public static MockImageAnalysisResponse failed(
      ImageLog imageLog
  ) {
    return new MockImageAnalysisResponse(
        imageLog.getId(),
        MockAnalysisScenario.ANALYSIS_FAILED,
        null,
        null,
        null,
        "mock-ai-v1",
        imageLog.getAnalysisStatus(),
        false
    );
  }
}
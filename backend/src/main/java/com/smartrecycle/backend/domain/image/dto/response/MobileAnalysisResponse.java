package com.smartrecycle.backend.domain.image.dto.response;

import com.smartrecycle.backend.domain.image.entity.ImageAnalysisStatus;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;

public record MobileAnalysisResponse (
    Long imageLogId,
    Long wasteItemId,
    String wasteItemName,
    Double confidence,
    String modelVersion,
    ImageAnalysisStatus analysisStatus,
    boolean needsServerReanalysis
){
  public static MobileAnalysisResponse from(
      ImageLog imageLog
  ){
    WasteItem wasteItem = imageLog.getMobileWasteItem();
    boolean needsServerReanalysis = imageLog.getAnalysisStatus() == ImageAnalysisStatus.SERVER_REANALYSIS_PENDING;

    return new MobileAnalysisResponse(
        imageLog.getId(),
        wasteItem != null ? wasteItem.getId() : null,
        wasteItem != null ? wasteItem.getName():null,
        imageLog.getMobileConfidence(),
        imageLog.getMobileModelVersion(),
        imageLog.getAnalysisStatus(),
        needsServerReanalysis
    );
  }
}

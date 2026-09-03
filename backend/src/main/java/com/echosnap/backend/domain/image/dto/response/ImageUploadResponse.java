package com.echosnap.backend.domain.image.dto.response;

import com.echosnap.backend.domain.image.entity.ImageAnalysisStatus;
import com.echosnap.backend.domain.image.entity.ImageLog;

import java.time.LocalDateTime;

/**
 * 이미지 업로드 완료 응답입니다.
 *
 * 아직 AI 분석을 실행한 것은 아니므로
 * 최초 analysisStatus는 UPLOADED입니다.
 */
public record ImageUploadResponse(

    Long imageLogId,

    String imageUrl,

    String originalFileName,

    String contentType,

    Long fileSize,

    ImageAnalysisStatus analysisStatus,

    LocalDateTime createdAt

) {

  public static ImageUploadResponse from(
      ImageLog imageLog
  ) {
    return new ImageUploadResponse(
        imageLog.getId(),
        imageLog.getImageUrl(),
        imageLog.getOriginalFileName(),
        imageLog.getContentType(),
        imageLog.getFileSize(),
        imageLog.getAnalysisStatus(),
        imageLog.getCreatedAt()
    );
  }
}
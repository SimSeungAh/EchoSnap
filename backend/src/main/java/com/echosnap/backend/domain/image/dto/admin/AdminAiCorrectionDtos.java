package com.echosnap.backend.domain.image.dto.admin;

import com.echosnap.backend.domain.image.entity.ImageLog;
import com.echosnap.backend.domain.image.entity.ImageReviewStatus;
import com.echosnap.backend.domain.waste.entity.WasteItem;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AdminAiCorrectionDtos {

  private AdminAiCorrectionDtos() {
  }

  /**
   * 관리자 AI 정정 검수 목록 / 상세 응답
   */
  public record CorrectionResponse(
      Long id,
      Long imageLogId,
      String imageUrl,
      String userEmail,

      String aiWasteItemName,
      Double aiConfidence,
      String aiModelVersion,

      String correctedWasteItemName,
      String userDescription,

      ImageReviewStatus reviewStatus,

      LocalDateTime correctedAt,
      LocalDateTime reviewedAt,

      String reviewerEmail,
      String reviewMemo
  ) {

    public static CorrectionResponse from(
        ImageLog imageLog
    ) {
      WasteItem aiWasteItem =
          resolveAiWasteItem(
              imageLog
          );

      Double aiConfidence =
          resolveAiConfidence(
              imageLog
          );

      String aiModelVersion =
          resolveAiModelVersion(
              imageLog
          );

      WasteItem correctedItem =
          imageLog
              .getUserCorrectedWasteItem();

      String reviewerEmail =
          imageLog.getReviewedBy() == null
              ? null
              : imageLog
              .getReviewedBy()
              .getEmail();

      /*
       * 사용자 전용 이미지 URL을 그대로 주면
       * 관리자 웹이 다른 사용자의 이미지를 읽지 못하므로,
       *
       * 관리자 전용 이미지 조회 API 주소를 반환합니다.
       */
      String adminImageUrl =
          "/api/admin/ai-corrections/"
              + imageLog.getId()
              + "/image";

      return new CorrectionResponse(
          imageLog.getId(),
          imageLog.getId(),
          adminImageUrl,
          imageLog.getUser().getEmail(),

          aiWasteItem == null
              ? null
              : aiWasteItem.getName(),

          aiConfidence,
          aiModelVersion,

          correctedItem == null
              ? null
              : correctedItem.getName(),

          imageLog.getUserCorrectionDescription(),

          imageLog.getReviewStatus(),

          imageLog.getUserCorrectedAt(),
          imageLog.getReviewedAt(),

          reviewerEmail,
          imageLog.getReviewNote()
      );
    }

    /**
     * AI 원본 결과 우선순위:
     *
     * 서버 YOLO
     * >
     * 모바일 TFLite
     */
    private static WasteItem resolveAiWasteItem(
        ImageLog imageLog
    ) {
      if (
          imageLog.getServerWasteItem()
              != null
      ) {
        return imageLog
            .getServerWasteItem();
      }

      return imageLog
          .getMobileWasteItem();
    }

    private static Double resolveAiConfidence(
        ImageLog imageLog
    ) {
      if (
          imageLog.getServerWasteItem()
              != null
      ) {
        return imageLog
            .getServerConfidence();
      }

      return imageLog
          .getMobileConfidence();
    }

    private static String resolveAiModelVersion(
        ImageLog imageLog
    ) {
      if (
          imageLog.getServerWasteItem()
              != null
      ) {
        return imageLog
            .getServerModelVersion();
      }

      return imageLog
          .getMobileModelVersion();
    }
  }

  /**
   * 관리자 승인 / 거절 요청
   */
  public record ReviewRequest(

      @Size(
          max = 1000,
          message = "검수 메모는 1000자 이하여야 합니다."
      )
      String memo
  ) {
  }
}

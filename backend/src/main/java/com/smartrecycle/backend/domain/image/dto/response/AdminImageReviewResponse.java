package com.smartrecycle.backend.domain.image.dto.response;

import com.smartrecycle.backend.domain.image.entity.ImageAnalysisStatus;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.entity.ImageReviewStatus;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;

import java.time.LocalDateTime;

/**
 * 관리자 AI 이미지 검수 화면에서 사용하는 응답입니다.
 *
 * 하나의 응답에서 다음 정보를 비교할 수 있습니다.
 *
 * - 업로드 사용자
 * - 원본 이미지
 * - 모바일 TensorFlow Lite 결과
 * - 서버 YOLO 결과
 * - 사용자 수정 결과
 * - 현재 최종 결과
 * - 관리자 검수 상태
 */
public record AdminImageReviewResponse(

    Long imageLogId,

    UserInfo user,

    String adminImageUrl,

    String originalFileName,

    String contentType,

    Long fileSize,

    ImageAnalysisStatus analysisStatus,

    ImageReviewStatus reviewStatus,

    AiPrediction mobileAnalysis,

    AiPrediction serverAnalysis,

    UserCorrection userCorrection,

    EffectiveResult effectiveResult,

    ReviewerInfo reviewer,

    String reviewNote,

    LocalDateTime reviewedAt,

    LocalDateTime createdAt

) {

  public static AdminImageReviewResponse from(
      ImageLog imageLog
  ) {
    return new AdminImageReviewResponse(

        imageLog.getId(),

        UserInfo.from(
            imageLog.getUser()
        ),

        "/api/admin/image-reviews/"
            + imageLog.getId()
            + "/file",

        imageLog.getOriginalFileName(),

        imageLog.getContentType(),

        imageLog.getFileSize(),

        imageLog.getAnalysisStatus(),

        imageLog.getReviewStatus(),

        AiPrediction.from(
            imageLog.getMobileWasteItem(),
            imageLog.getMobileConfidence(),
            imageLog.getMobileModelVersion()
        ),

        AiPrediction.from(
            imageLog.getServerWasteItem(),
            imageLog.getServerConfidence(),
            imageLog.getServerModelVersion()
        ),

        UserCorrection.from(
            imageLog.getUserCorrectedWasteItem(),
            imageLog.getUserCorrectedAt()
        ),

        EffectiveResult.from(
            imageLog
        ),

        ReviewerInfo.from(
            imageLog.getReviewedBy()
        ),

        imageLog.getReviewNote(),

        imageLog.getReviewedAt(),

        imageLog.getCreatedAt()
    );
  }

  /**
   * 이미지를 등록한 사용자 정보입니다.
   */
  public record UserInfo(

      Long userId,

      String email,

      String nickname

  ) {

    private static UserInfo from(
        User user
    ) {
      if (user == null) {
        return null;
      }

      return new UserInfo(
          user.getId(),
          user.getEmail(),
          user.getNickname()
      );
    }
  }

  /**
   * 모바일 AI 또는 서버 AI의
   * 개별 분석 결과입니다.
   */
  public record AiPrediction(

      Long wasteItemId,

      String wasteItemName,

      Double confidence,

      String modelVersion

  ) {

    private static AiPrediction from(
        WasteItem wasteItem,
        Double confidence,
        String modelVersion
    ) {
      if (wasteItem == null) {
        return null;
      }

      return new AiPrediction(
          wasteItem.getId(),
          wasteItem.getName(),
          confidence,
          modelVersion
      );
    }
  }

  /**
   * 사용자가 직접 수정한 결과입니다.
   */
  public record UserCorrection(

      Long wasteItemId,

      String wasteItemName,

      LocalDateTime correctedAt

  ) {

    private static UserCorrection from(
        WasteItem wasteItem,
        LocalDateTime correctedAt
    ) {
      if (wasteItem == null) {
        return null;
      }

      return new UserCorrection(
          wasteItem.getId(),
          wasteItem.getName(),
          correctedAt
      );
    }
  }

  /**
   * 현재 서비스에서 최종적으로 사용하는 결과입니다.
   *
   * 우선순위:
   *
   * 사용자 수정
   * >
   * 서버 YOLO
   * >
   * 모바일 TensorFlow Lite
   */
  public record EffectiveResult(

      Long wasteItemId,

      String wasteItemName,

      Double confidence

  ) {

    private static EffectiveResult from(
        ImageLog imageLog
    ) {
      WasteItem effectiveWasteItem =
          imageLog.getEffectiveWasteItem();

      if (effectiveWasteItem == null) {
        return null;
      }

      return new EffectiveResult(
          effectiveWasteItem.getId(),
          effectiveWasteItem.getName(),
          imageLog.getEffectiveConfidence()
      );
    }
  }

  /**
   * 검수를 완료한 관리자 정보입니다.
   */
  public record ReviewerInfo(

      Long userId,

      String email,

      String nickname

  ) {

    private static ReviewerInfo from(
        User reviewer
    ) {
      if (reviewer == null) {
        return null;
      }

      return new ReviewerInfo(
          reviewer.getId(),
          reviewer.getEmail(),
          reviewer.getNickname()
      );
    }
  }
}
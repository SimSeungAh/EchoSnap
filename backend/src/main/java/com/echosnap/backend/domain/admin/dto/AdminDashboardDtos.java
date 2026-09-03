package com.echosnap.backend.domain.admin.dto;

import com.echosnap.backend.domain.image.entity.ImageLog;
import com.echosnap.backend.domain.image.entity.ImageReviewStatus;
import com.echosnap.backend.domain.publicdata.entity.PublicDataSyncLog;
import com.echosnap.backend.domain.waste.entity.WasteItem;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminDashboardDtos {

  private AdminDashboardDtos() {
  }

  /**
   * 관리자 메인 대시보드 전체 응답
   */
  public record DashboardResponse(
      long totalUsers,
      long activeUsers,
      long pendingResidences,
      long wasteItems,
      long pendingAiCorrections,
      long todayNotifications,
      List<RecentCorrectionResponse> recentCorrections,
      List<RecentSyncResponse> recentSyncLogs
  ) {
  }

  /**
   * 최근 AI 사용자 정정
   */
  public record RecentCorrectionResponse(
      Long id,
      Long imageLogId,
      String imageUrl,
      String userEmail,
      String aiWasteItemName,
      Double aiConfidence,
      String aiModelVersion,
      String correctedWasteItemName,
      ImageReviewStatus reviewStatus,
      LocalDateTime correctedAt,
      LocalDateTime reviewedAt,
      String reviewerEmail,
      String reviewMemo
  ) {

    public static RecentCorrectionResponse from(
        ImageLog imageLog
    ) {
      WasteItem originalAiItem =
          resolveOriginalAiItem(
              imageLog
          );

      Double originalConfidence =
          resolveOriginalConfidence(
              imageLog
          );

      String originalModelVersion =
          resolveOriginalModelVersion(
              imageLog
          );

      WasteItem correctedItem =
          imageLog.getUserCorrectedWasteItem();

      String reviewerEmail =
          imageLog.getReviewedBy() == null
              ? null
              : imageLog
              .getReviewedBy()
              .getEmail();

      return new RecentCorrectionResponse(
          imageLog.getId(),
          imageLog.getId(),
          "/api/admin/ai-corrections/"
              + imageLog.getId()
              + "/image",
          imageLog.getUser().getEmail(),

          originalAiItem == null
              ? null
              : originalAiItem.getName(),

          originalConfidence,
          originalModelVersion,

          correctedItem == null
              ? null
              : correctedItem.getName(),

          imageLog.getReviewStatus(),
          imageLog.getUserCorrectedAt(),
          imageLog.getReviewedAt(),
          reviewerEmail,
          imageLog.getReviewNote()
      );
    }

    private static WasteItem resolveOriginalAiItem(
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

    private static Double resolveOriginalConfidence(
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

    private static String resolveOriginalModelVersion(
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
   * 최근 공공데이터 동기화 이력
   */
  public record RecentSyncResponse(
      Long id,
      String source,
      String status,
      LocalDateTime startedAt,
      LocalDateTime finishedAt,
      int insertedCount,
      int updatedCount,
      int failedCount,
      int skippedCount,
      String message
  ) {

    public static RecentSyncResponse from(
        PublicDataSyncLog log
    ) {
      return new RecentSyncResponse(
          log.getId(),
          log.getSource(),
          log.getStatus().name(),
          log.getStartedAt(),
          log.getFinishedAt(),
          log.getInsertedCount(),
          log.getUpdatedCount(),
          log.getFailedCount(),
          log.getSkippedCount(),
          log.getMessage()
      );
    }
  }
}
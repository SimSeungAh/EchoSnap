package com.smartrecycle.backend.domain.image.entity;

import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;
import com.smartrecycle.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자가 촬영하거나 선택한 이미지와
 * AI 분석 과정을 저장하는 이력 Entity입니다.
 *
 * 하나의 ImageLog에서 다음 데이터를 추적합니다.
 *
 * - 업로드 이미지
 * - Flutter TensorFlow Lite 결과
 * - Python YOLO 결과
 * - 각 모델의 신뢰도
 * - 모델 버전
 * - 사용자 수정 결과
 * - 관리자 검수 결과
 *
 * AI 결과를 하나의 컬럼에 계속 덮어쓰지 않고
 * 각 단계 결과를 별도로 보존하는 것이 핵심입니다.
 */
@Getter
@Entity
@Table(
    name = "image_logs",
    indexes = {
        @Index(
            name = "idx_image_logs_user",
            columnList = "user_id"
        ),
        @Index(
            name = "idx_image_logs_analysis_status",
            columnList = "analysis_status"
        ),
        @Index(
            name = "idx_image_logs_review_status",
            columnList = "review_status"
        ),
        @Index(
            name = "idx_image_logs_created_at",
            columnList = "created_at"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageLog extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /*
   * =========================================================
   * 사용자
   * =========================================================
   */

  /**
   * 이미지를 분석한 사용자입니다.
   */
  @ManyToOne(
      fetch = FetchType.LAZY,
      optional = false
  )
  @JoinColumn(
      name = "user_id",
      nullable = false
  )
  private User user;

  /*
   * =========================================================
   * 이미지 파일 정보
   * =========================================================
   */

  /**
   * 실제 이미지에 접근할 수 있는 경로입니다.
   *
   * 초기:
   * 로컬 저장 경로 또는 로컬 URL
   *
   * 이후:
   * S3 URL
   */
  @Column(
      name = "image_url",
      nullable = false,
      length = 1000
  )
  private String imageUrl;

  /**
   * 사용자가 업로드한 원래 파일명입니다.
   */
  @Column(
      name = "original_file_name",
      length = 500
  )
  private String originalFileName;

  /**
   * 서버가 충돌 방지를 위해 생성한
   * 실제 저장 파일명입니다.
   */
  @Column(
      name = "stored_file_name",
      length = 500
  )
  private String storedFileName;

  /**
   * 이미지 Content-Type입니다.
   *
   * 예:
   * image/jpeg
   * image/png
   */
  @Column(
      name = "content_type",
      length = 100
  )
  private String contentType;

  /**
   * 파일 크기(byte)
   */
  @Column(
      name = "file_size"
  )
  private Long fileSize;

  /*
   * =========================================================
   * 분석 상태
   * =========================================================
   */

  @Enumerated(EnumType.STRING)
  @Column(
      name = "analysis_status",
      nullable = false,
      length = 40
  )
  private ImageAnalysisStatus analysisStatus;

  /*
   * =========================================================
   * 모바일 TensorFlow Lite 결과
   * =========================================================
   */

  /**
   * Flutter TensorFlow Lite가
   * 1차 분류한 폐기물 품목입니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "mobile_waste_item_id"
  )
  private WasteItem mobileWasteItem;

  /**
   * 모바일 AI 신뢰도
   *
   * 0.0 ~ 1.0
   */
  @Column(
      name = "mobile_confidence"
  )
  private Double mobileConfidence;

  /**
   * Flutter에 탑재된
   * TensorFlow Lite 모델 버전
   *
   * 예:
   * waste-classifier-v1.0
   */
  @Column(
      name = "mobile_model_version",
      length = 100
  )
  private String mobileModelVersion;

  /*
   * =========================================================
   * 서버 Python YOLO 결과
   * =========================================================
   */

  /**
   * Python YOLO 서버가
   * 재분석한 폐기물 품목입니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "server_waste_item_id"
  )
  private WasteItem serverWasteItem;

  /**
   * 서버 AI 신뢰도
   *
   * 0.0 ~ 1.0
   */
  @Column(
      name = "server_confidence"
  )
  private Double serverConfidence;

  /**
   * 서버에서 사용한 YOLO 모델 버전
   *
   * 예:
   * smartrecycle-yolo-v1
   */
  @Column(
      name = "server_model_version",
      length = 100
  )
  private String serverModelVersion;

  /*
   * =========================================================
   * 사용자 수정 결과
   * =========================================================
   */

  /**
   * 사용자가 AI 결과가 틀렸다고 판단하고
   * 직접 선택한 올바른 WasteItem입니다.
   *
   * 수정하지 않았다면 null입니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "user_corrected_waste_item_id"
  )
  private WasteItem userCorrectedWasteItem;

  /**
   * 사용자가 AI 결과를 수정한 시각
   */
  @Column(
      name = "user_corrected_at"
  )
  private LocalDateTime userCorrectedAt;

  /*
   * =========================================================
   * 관리자 검수
   * =========================================================
   */

  @Enumerated(EnumType.STRING)
  @Column(
      name = "review_status",
      nullable = false,
      length = 30
  )
  private ImageReviewStatus reviewStatus;

  /**
   * 검수한 관리자
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "reviewed_by"
  )
  private User reviewedBy;

  /**
   * 관리자 검수 메모
   */
  @Column(
      name = "review_note",
      length = 1000
  )
  private String reviewNote;

  /**
   * 관리자 검수 완료 시각
   */
  @Column(
      name = "reviewed_at"
  )
  private LocalDateTime reviewedAt;

  private ImageLog(
      User user,
      String imageUrl,
      String originalFileName,
      String storedFileName,
      String contentType,
      Long fileSize
  ) {
    this.user = user;

    this.imageUrl =
        imageUrl;

    this.originalFileName =
        originalFileName;

    this.storedFileName =
        storedFileName;

    this.contentType =
        contentType;

    this.fileSize =
        fileSize;

    this.analysisStatus =
        ImageAnalysisStatus.UPLOADED;

    this.reviewStatus =
        ImageReviewStatus.NOT_REQUIRED;
  }

  /**
   * 업로드가 완료된 이미지 이력을 생성합니다.
   */
  public static ImageLog createUploaded(
      User user,
      String imageUrl,
      String originalFileName,
      String storedFileName,
      String contentType,
      Long fileSize
  ) {
    return new ImageLog(
        user,
        imageUrl,
        originalFileName,
        storedFileName,
        contentType,
        fileSize
    );
  }

  /*
   * =========================================================
   * 모바일 AI
   * =========================================================
   */

  /**
   * Flutter TensorFlow Lite의
   * 1차 분석 결과를 기록합니다.
   */
  public void recordMobileAnalysis(
      WasteItem wasteItem,
      Double confidence,
      String modelVersion
  ) {
    this.mobileWasteItem =
        wasteItem;

    this.mobileConfidence =
        confidence;

    this.mobileModelVersion =
        modelVersion;

    this.analysisStatus =
        ImageAnalysisStatus.MOBILE_ANALYZED;
  }

  /**
   * 모바일 AI 신뢰도가 낮아
   * 서버 YOLO 재분석이 필요한 상태로 변경합니다.
   */
  public void requestServerReanalysis() {
    this.analysisStatus =
        ImageAnalysisStatus.SERVER_REANALYSIS_PENDING;
  }

  /*
   * =========================================================
   * 서버 AI
   * =========================================================
   */

  /**
   * Python YOLO 재분석 결과를 기록합니다.
   */
  public void recordServerAnalysis(
      WasteItem wasteItem,
      Double confidence,
      String modelVersion
  ) {
    this.serverWasteItem =
        wasteItem;

    this.serverConfidence =
        confidence;

    this.serverModelVersion =
        modelVersion;

    this.analysisStatus =
        ImageAnalysisStatus.SERVER_ANALYZED;
  }

  /**
   * AI 분석 실패 상태로 변경합니다.
   */
  public void markAnalysisFailed() {
    this.analysisStatus =
        ImageAnalysisStatus.ANALYSIS_FAILED;
  }

  /*
   * =========================================================
   * 사용자 수정
   * =========================================================
   */

  /**
   * 사용자가 AI 결과를 직접 수정합니다.
   *
   * 사용자 수정 데이터는
   * 이후 관리자 검수 대상으로 전환합니다.
   */
  public void correctByUser(
      WasteItem correctedWasteItem
  ) {
    this.userCorrectedWasteItem =
        correctedWasteItem;

    this.userCorrectedAt =
        LocalDateTime.now();

    this.reviewStatus =
        ImageReviewStatus.PENDING;

    /*
     * 기존 관리자 검수 정보가 있었다면
     * 새 사용자 수정으로 인해 다시 검수가 필요하므로
     * 초기화합니다.
     */
    this.reviewedBy =
        null;

    this.reviewNote =
        null;

    this.reviewedAt =
        null;
  }

  /*
   * =========================================================
   * 관리자 검수
   * =========================================================
   */

  /**
   * 관리자가 검수 데이터를 승인합니다.
   */
  public void approveReview(
      User reviewer,
      String reviewNote
  ) {
    this.reviewStatus =
        ImageReviewStatus.APPROVED;

    this.reviewedBy =
        reviewer;

    this.reviewNote =
        reviewNote;

    this.reviewedAt =
        LocalDateTime.now();
  }

  /**
   * 관리자가 검수 데이터를 거절합니다.
   */
  public void rejectReview(
      User reviewer,
      String reviewNote
  ) {
    this.reviewStatus =
        ImageReviewStatus.REJECTED;

    this.reviewedBy =
        reviewer;

    this.reviewNote =
        reviewNote;

    this.reviewedAt =
        LocalDateTime.now();
  }

  /*
   * =========================================================
   * 최종 결과 계산
   * =========================================================
   */

  /**
   * 사용자에게 최종적으로 사용할 품목을 계산합니다.
   *
   * 우선순위:
   *
   * 사용자 직접 수정
   * >
   * 서버 YOLO 결과
   * >
   * 모바일 TensorFlow Lite 결과
   */
  public WasteItem getEffectiveWasteItem() {

    if (userCorrectedWasteItem != null) {
      return userCorrectedWasteItem;
    }

    if (serverWasteItem != null) {
      return serverWasteItem;
    }

    return mobileWasteItem;
  }

  /**
   * 최종 AI 신뢰도를 반환합니다.
   *
   * 사용자가 직접 수정한 경우에는
   * AI 신뢰도라는 개념이 아니므로 null을 반환합니다.
   */
  public Double getEffectiveConfidence() {

    if (userCorrectedWasteItem != null) {
      return null;
    }

    if (serverWasteItem != null) {
      return serverConfidence;
    }

    return mobileConfidence;
  }

  /**
   * 사용자 수정 여부
   */
  public boolean isUserCorrected() {
    return userCorrectedWasteItem != null;
  }

  /**
   * 관리자 검수가 필요한지 확인합니다.
   */
  public boolean isReviewPending() {
    return reviewStatus
        == ImageReviewStatus.PENDING;
  }
}
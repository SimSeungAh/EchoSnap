package com.smartrecycle.backend.domain.publicdata.entity;

import com.smartrecycle.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
    name = "public_data_sync_logs",
    indexes = {
        @Index(
            name = "idx_public_data_sync_logs_started_at",
            columnList = "started_at"
        ),
        @Index(
            name = "idx_public_data_sync_logs_status",
            columnList = "status"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicDataSyncLog extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 어떤 공공데이터를 동기화했는지 표시합니다.
   *
   * 현재는 행정안전부 생활쓰레기배출정보를 사용하지만
   * 추후 다른 공공데이터가 추가되어도
   * 같은 테이블에서 관리할 수 있습니다.
   */
  @Column(
      nullable = false,
      length = 150
  )
  private String source;

  @Enumerated(EnumType.STRING)
  @Column(
      nullable = false,
      length = 20
  )
  private SyncStatus status;

  @Column(
      name = "started_at",
      nullable = false
  )
  private LocalDateTime startedAt;

  @Column(
      name = "finished_at"
  )
  private LocalDateTime finishedAt;

  @Column(
      name = "inserted_count",
      nullable = false
  )
  private int insertedCount;

  @Column(
      name = "updated_count",
      nullable = false
  )
  private int updatedCount;

  @Column(
      name = "failed_count",
      nullable = false
  )
  private int failedCount;

  @Column(
      name = "skipped_count",
      nullable = false
  )
  private int skippedCount;

  @Column(
      length = 1000
  )
  private String message;

  private PublicDataSyncLog(
      String source
  ) {
    this.source = source;
    this.status = SyncStatus.RUNNING;
    this.startedAt = LocalDateTime.now();

    this.insertedCount = 0;
    this.updatedCount = 0;
    this.failedCount = 0;
    this.skippedCount = 0;

    this.message = "공공데이터 동기화 진행 중";
  }

  public static PublicDataSyncLog start(
      String source
  ) {
    return new PublicDataSyncLog(
        source
    );
  }

  /**
   * 동기화 성공
   */
  public void completeSuccess(
      int insertedCount,
      int updatedCount,
      int skippedCount
  ) {
    this.status = SyncStatus.SUCCESS;

    this.insertedCount =
        insertedCount;

    this.updatedCount =
        updatedCount;

    this.failedCount = 0;

    this.skippedCount =
        skippedCount;

    this.finishedAt =
        LocalDateTime.now();

    this.message =
        "공공데이터 동기화가 정상적으로 완료되었습니다.";
  }

  /**
   * 동기화 실패
   */
  public void completeFailure(
      String message
  ) {
    this.status = SyncStatus.FAILED;

    this.failedCount = 1;

    this.finishedAt =
        LocalDateTime.now();

    if (
        message == null
            || message.isBlank()
    ) {
      this.message =
          "공공데이터 동기화 중 오류가 발생했습니다.";

      return;
    }

    this.message =
        truncate(
            message,
            1000
        );
  }

  private String truncate(
      String value,
      int maxLength
  ) {
    if (
        value.length()
            <= maxLength
    ) {
      return value;
    }

    return value.substring(
        0,
        maxLength
    );
  }

  public enum SyncStatus {
    RUNNING,
    SUCCESS,
    FAILED
  }
}
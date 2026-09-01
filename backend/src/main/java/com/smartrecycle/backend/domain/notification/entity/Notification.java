package com.smartrecycle.backend.domain.notification.entity;

import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.global.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@Table(
    name = "app_notifications",
    indexes = {
        @Index(
            name = "idx_notifications_status_sent_at",
            columnList = "status, sent_at"
        ),
        @Index(
            name = "idx_notifications_target_type",
            columnList = "target_type"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(
      nullable = false,
      length = 120
  )
  private String title;

  @Column(
      nullable = false,
      length = 2000
  )
  private String body;

  /**
   * 알림 대상
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "target_type",
      nullable = false,
      length = 40
  )
  private TargetType targetType;

  /**
   * 발송 상태
   */
  @Enumerated(EnumType.STRING)
  @Column(
      nullable = false,
      length = 20
  )
  private Status status;

  /**
   * 알림을 발송한 관리자
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "sender_user_id",
      nullable = false
  )
  private User sender;

  @Column(
      name = "sent_at",
      nullable = false
  )
  private LocalDateTime sentAt;

  @Column(
      name = "cancelled_at"
  )
  private LocalDateTime cancelledAt;

  /**
   * 이 알림을 읽은 사용자 ID 목록입니다.
   *
   * 알림 본문과 사용자별 읽음 상태를 분리하기 위해
   * 별도의 CollectionTable에 저장합니다.
   */
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "notification_reads",
      joinColumns = @JoinColumn(
          name = "notification_id"
      ),
      uniqueConstraints = @UniqueConstraint(
          name = "uk_notification_reads_notification_user",
          columnNames = {
              "notification_id",
              "reader_user_id"
          }
      )
  )
  @Column(
      name = "reader_user_id",
      nullable = false
  )
  private Set<Long> readUserIds =
      new HashSet<>();

  private Notification(
      String title,
      String body,
      TargetType targetType,
      User sender
  ) {
    this.title = title;
    this.body = body;
    this.targetType = targetType;
    this.sender = sender;

    this.status = Status.SENT;
    this.sentAt = LocalDateTime.now();
  }

  public static Notification send(
      String title,
      String body,
      TargetType targetType,
      User sender
  ) {
    return new Notification(
        title,
        body,
        targetType,
        sender
    );
  }

  public boolean isReadBy(
      Long userId
  ) {
    return readUserIds.contains(
        userId
    );
  }

  public void markAsRead(
      Long userId
  ) {
    readUserIds.add(
        userId
    );
  }

  /**
   * 발송 취소 시 사용자 알림 목록에서 더 이상 노출하지 않습니다.
   */
  public void cancel() {
    if (status == Status.CANCELLED) {
      return;
    }

    this.status = Status.CANCELLED;
    this.cancelledAt =
        LocalDateTime.now();
  }

  public enum TargetType {

    /**
     * 현재 활성화된 모든 사용자
     */
    ALL_ACTIVE_USERS,

    /**
     * 알림 수신 동의 사용자를 대상으로 함
     */
    NOTIFICATION_ENABLED_USERS,

    /**
     * 아파트 / 오피스텔 등 공동주택 사용자
     */
    MANAGED_COMPLEX_USERS,

    /**
     * 일반주택 사용자
     */
    GENERAL_HOUSING_USERS
  }

  public enum Status {
    SENT,
    CANCELLED
  }
}
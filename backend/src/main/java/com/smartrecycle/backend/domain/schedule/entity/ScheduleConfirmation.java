package com.smartrecycle.backend.domain.schedule.entity;

import com.smartrecycle.backend.domain.user.entity.User;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주민 일정 제보에 대해
 * 동일한 일정 적용 범위의 다른 주민이 남기는 확인 값입니다.
 *
 * 선호도를 선택하는 투표가 아니라
 * 제보 내용이 실제 일정과 맞는지 확인하는 기능입니다.
 */
@Getter
@Entity
@Table(
    name = "schedule_confirmations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_schedule_confirmations_report_confirmer",
            columnNames = {
                "schedule_report_id",
                "confirmer_id"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_schedule_confirmations_report",
            columnList = "schedule_report_id"
        ),
        @Index(
            name = "idx_schedule_confirmations_confirmer",
            columnList = "confirmer_id"
        ),
        @Index(
            name = "idx_schedule_confirmations_value",
            columnList = "confirmation_value"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleConfirmation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 주민이 확인하는 일정 제보입니다.
   */
  @ManyToOne(
      fetch = FetchType.LAZY,
      optional = false
  )
  @JoinColumn(
      name = "schedule_report_id",
      nullable = false
  )
  private ScheduleReport scheduleReport;

  /**
   * 확인을 남긴 주민입니다.
   */
  @ManyToOne(
      fetch = FetchType.LAZY,
      optional = false
  )
  @JoinColumn(
      name = "confirmer_id",
      nullable = false
  )
  private User confirmer;

  /**
   * CONFIRMED:
   * 실제 일정과 제보 내용이 맞음
   *
   * DIFFERENT:
   * 실제 일정과 제보 내용이 다름
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "confirmation_value",
      nullable = false,
      length = 20
  )
  private ScheduleConfirmationValue value;

  private ScheduleConfirmation(
      ScheduleReport scheduleReport,
      User confirmer,
      ScheduleConfirmationValue value
  ) {
    this.scheduleReport = scheduleReport;
    this.confirmer = confirmer;
    this.value = value;
  }

  /**
   * 주민 확인을 새로 생성합니다.
   */
  public static ScheduleConfirmation create(
      ScheduleReport scheduleReport,
      User confirmer,
      ScheduleConfirmationValue value
  ) {
    return new ScheduleConfirmation(
        scheduleReport,
        confirmer,
        value
    );
  }

  /**
   * 관리자가 아직 검토하기 전이라면
   * 사용자가 자신의 확인 값을 변경할 수 있도록 사용합니다.
   *
   * 실제 변경 가능 여부는 Service에서
   * ScheduleReport 상태를 확인한 뒤 호출합니다.
   */
  public void changeValue(
      ScheduleConfirmationValue value
  ) {
    this.value = value;
  }
}
package com.echosnap.backend.domain.schedule.entity;

import com.echosnap.backend.domain.apartment.entity.Apartment;
import com.echosnap.backend.domain.collectionarea.entity.CollectionArea;
import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
import com.echosnap.backend.domain.user.entity.User;
import com.echosnap.backend.domain.waste.entity.WasteItem;
import com.echosnap.backend.global.entity.BaseEntity;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 주민이 등록하는 배출 일정 제보입니다.
 *
 * 공동주택과 일반주택 모두 하나의 주민 제보 흐름을 사용하지만,
 * 실제 공식 일정의 저장 구조는 서로 다르기 때문에
 * 대상에 따라 필요한 필드를 구분해서 사용합니다.
 *
 * 공동주택:
 * Apartment + WasteItem
 *
 * 일반주택:
 * CollectionArea + CollectionWasteType
 */
@Getter
@Entity
@Table(
    name = "schedule_reports",
    indexes = {
        @Index(
            name = "idx_schedule_reports_reporter",
            columnList = "reporter_id"
        ),
        @Index(
            name = "idx_schedule_reports_apartment",
            columnList = "apartment_id"
        ),
        @Index(
            name = "idx_schedule_reports_collection_area",
            columnList = "collection_area_id"
        ),
        @Index(
            name = "idx_schedule_reports_status",
            columnList = "status"
        ),
        @Index(
            name = "idx_schedule_reports_type",
            columnList = "report_type"
        ),
        @Index(
            name = "idx_schedule_reports_effective_date",
            columnList = "effective_date"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleReport extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 제보를 등록한 주민입니다.
   */
  @ManyToOne(
      fetch = FetchType.LAZY,
      optional = false
  )
  @JoinColumn(
      name = "reporter_id",
      nullable = false
  )
  private User reporter;

  /**
   * 최초 일정 / 정기 일정 정정 / 특정 날짜 변경
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "report_type",
      nullable = false,
      length = 30
  )
  private ScheduleReportType reportType;

  /**
   * 관리자 검토 상태입니다.
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "status",
      nullable = false,
      length = 20
  )
  private ScheduleReportStatus status;

  /*
   * ---------------------------------------------------------
   * 공동주택 제보 대상
   * ---------------------------------------------------------
   */

  /**
   * 공동주택 일정 제보인 경우 대상 Apartment입니다.
   *
   * 일반주택 제보에서는 null입니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "apartment_id"
  )
  private Apartment apartment;

  /**
   * 공동주택 일정 제보인 경우 대상 WasteItem입니다.
   *
   * 일반주택 제보에서는 null입니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "waste_item_id"
  )
  private WasteItem wasteItem;

  /*
   * ---------------------------------------------------------
   * 일반주택 제보 대상
   * ---------------------------------------------------------
   */

  /**
   * 일반주택 일정 제보인 경우 대상 CollectionArea입니다.
   *
   * 공동주택 제보에서는 null입니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "collection_area_id"
  )
  private CollectionArea collectionArea;

  /**
   * 일반주택 일정 제보의 폐기물 종류입니다.
   *
   * LIFE_WASTE
   * FOOD_WASTE
   * RECYCLABLE
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "collection_waste_type",
      length = 30
  )
  private CollectionWasteType collectionWasteType;

  /*
   * ---------------------------------------------------------
   * 기존 공식 일정 참조
   * ---------------------------------------------------------
   */

  /**
   * SCHEDULE_CORRECTION 또는 TEMPORARY_CHANGE에서
   * 기존 공식 일정을 특정할 필요가 있는 경우 사용합니다.
   *
   * 공동주택:
   * RecycleSchedule ID
   *
   * 일반주택:
   * CollectionAreaSchedule ID
   *
   * INITIAL_SCHEDULE에서는 일반적으로 null입니다.
   *
   * Apartment / CollectionArea 중 어느 대상인지에 따라
   * 어떤 일정 테이블의 ID인지 구분할 수 있습니다.
   */
  @Column(
      name = "reference_schedule_id"
  )
  private Long referenceScheduleId;

  /*
   * ---------------------------------------------------------
   * 주민이 제보한 일정 내용
   * ---------------------------------------------------------
   */

  /**
   * 공동주택의 주간 일정 제보에서 사용하는 요일입니다.
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "reported_day_of_week",
      length = 20
  )
  private DayOfWeek reportedDayOfWeek;

  /**
   * 일반주택 공공데이터형 일정에서 사용하는
   * 원본 요일 표현입니다.
   *
   * 예:
   * 월+수+금
   * 일+화+목
   */
  @Column(
      name = "reported_emission_days",
      length = 500
  )
  private String reportedEmissionDays;

  /**
   * 제보된 배출 시작 시간입니다.
   */
  @Column(
      name = "reported_start_time"
  )
  private LocalTime reportedStartTime;

  /**
   * 제보된 배출 종료 시간입니다.
   *
   * 일반주택에서는 20:00 ~ 02:00처럼
   * 자정을 넘어가는 값도 가능합니다.
   */
  @Column(
      name = "reported_end_time"
  )
  private LocalTime reportedEndTime;

  /**
   * 공동주택 일정이 상시 배출인지 여부입니다.
   *
   * 일반주택에서는 null일 수 있습니다.
   */
  @Column(
      name = "reported_always_available"
  )
  private Boolean reportedAlwaysAvailable;

  /*
   * ---------------------------------------------------------
   * 특정 날짜 변경 제보
   * ---------------------------------------------------------
   */

  /**
   * TEMPORARY_CHANGE가 적용되는 특정 날짜입니다.
   *
   * 정기 일정 제보에서는 null입니다.
   */
  @Column(
      name = "effective_date"
  )
  private LocalDate effectiveDate;

  /**
   * 특정 날짜에 아예 수거 또는 배출이 불가능한지 나타냅니다.
   *
   * 예:
   * 명절 휴무
   * 시설 점검
   * 수거업체 사정
   *
   * TEMPORARY_CHANGE가 아닌 경우 null입니다.
   */
  @Column(
      name = "temporary_unavailable"
  )
  private Boolean temporaryUnavailable;

  /**
   * 주민이 관리자와 다른 주민에게 전달하는
   * 제보 설명입니다.
   *
   * 예:
   * "관리사무소 공지에서 이번 주 목요일 수거가
   * 금요일로 변경됐다고 안내했습니다."
   */
  @Column(
      name = "report_note",
      length = 1000
  )
  private String reportNote;

  /*
   * ---------------------------------------------------------
   * 관리자 검토
   * ---------------------------------------------------------
   */

  /**
   * 최종 검토한 관리자입니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "reviewed_by"
  )
  private User reviewedBy;

  /**
   * 관리자의 승인/거절 메모입니다.
   */
  @Column(
      name = "review_note",
      length = 1000
  )
  private String reviewNote;

  /**
   * 관리자 검토 완료 시각입니다.
   */
  @Column(
      name = "reviewed_at"
  )
  private LocalDateTime reviewedAt;

  private ScheduleReport(
      User reporter,
      ScheduleReportType reportType,
      Apartment apartment,
      WasteItem wasteItem,
      CollectionArea collectionArea,
      CollectionWasteType collectionWasteType,
      Long referenceScheduleId,
      DayOfWeek reportedDayOfWeek,
      String reportedEmissionDays,
      LocalTime reportedStartTime,
      LocalTime reportedEndTime,
      Boolean reportedAlwaysAvailable,
      LocalDate effectiveDate,
      Boolean temporaryUnavailable,
      String reportNote
  ) {
    this.reporter = reporter;
    this.reportType = reportType;
    this.status = ScheduleReportStatus.PENDING;

    this.apartment = apartment;
    this.wasteItem = wasteItem;

    this.collectionArea = collectionArea;
    this.collectionWasteType =
        collectionWasteType;

    this.referenceScheduleId =
        referenceScheduleId;

    this.reportedDayOfWeek =
        reportedDayOfWeek;

    this.reportedEmissionDays =
        reportedEmissionDays;

    this.reportedStartTime =
        reportedStartTime;

    this.reportedEndTime =
        reportedEndTime;

    this.reportedAlwaysAvailable =
        reportedAlwaysAvailable;

    this.effectiveDate =
        effectiveDate;

    this.temporaryUnavailable =
        temporaryUnavailable;

    this.reportNote =
        reportNote;
  }

  /**
   * 공동주택 주민 일정 제보를 생성합니다.
   */
  public static ScheduleReport createForApartment(
      User reporter,
      ScheduleReportType reportType,
      Apartment apartment,
      WasteItem wasteItem,
      Long referenceScheduleId,
      DayOfWeek reportedDayOfWeek,
      LocalTime reportedStartTime,
      LocalTime reportedEndTime,
      Boolean reportedAlwaysAvailable,
      LocalDate effectiveDate,
      Boolean temporaryUnavailable,
      String reportNote
  ) {
    return new ScheduleReport(
        reporter,
        reportType,
        apartment,
        wasteItem,
        null,
        null,
        referenceScheduleId,
        reportedDayOfWeek,
        null,
        reportedStartTime,
        reportedEndTime,
        reportedAlwaysAvailable,
        effectiveDate,
        temporaryUnavailable,
        reportNote
    );
  }

  /**
   * 일반주택 주민 일정 제보를 생성합니다.
   */
  public static ScheduleReport createForCollectionArea(
      User reporter,
      ScheduleReportType reportType,
      CollectionArea collectionArea,
      CollectionWasteType collectionWasteType,
      Long referenceScheduleId,
      String reportedEmissionDays,
      LocalTime reportedStartTime,
      LocalTime reportedEndTime,
      LocalDate effectiveDate,
      Boolean temporaryUnavailable,
      String reportNote
  ) {
    return new ScheduleReport(
        reporter,
        reportType,
        null,
        null,
        collectionArea,
        collectionWasteType,
        referenceScheduleId,
        null,
        reportedEmissionDays,
        reportedStartTime,
        reportedEndTime,
        null,
        effectiveDate,
        temporaryUnavailable,
        reportNote
    );
  }

  /**
   * 아직 관리자 검토가 가능한 상태인지 확인합니다.
   */
  public boolean isPending() {
    return status
        == ScheduleReportStatus.PENDING;
  }

  /**
   * 관리자 승인 상태로 변경합니다.
   *
   * 실제 공식 일정 반영은
   * 관리자 Service가 같은 Transaction 안에서 수행합니다.
   */
  public void approve(
      User reviewer,
      String reviewNote
  ) {
    this.status =
        ScheduleReportStatus.APPROVED;

    this.reviewedBy =
        reviewer;

    this.reviewNote =
        reviewNote;

    this.reviewedAt =
        LocalDateTime.now();
  }

  /**
   * 관리자 거절 상태로 변경합니다.
   */
  public void reject(
      User reviewer,
      String reviewNote
  ) {
    this.status =
        ScheduleReportStatus.REJECTED;

    this.reviewedBy =
        reviewer;

    this.reviewNote =
        reviewNote;

    this.reviewedAt =
        LocalDateTime.now();
  }

  /**
   * 공동주택 일정 제보인지 확인합니다.
   */
  public boolean isApartmentReport() {
    return apartment != null;
  }

  /**
   * 일반주택 수거구역 일정 제보인지 확인합니다.
   */
  public boolean isCollectionAreaReport() {
    return collectionArea != null;
  }
}
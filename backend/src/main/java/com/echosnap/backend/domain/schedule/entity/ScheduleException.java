package com.echosnap.backend.domain.schedule.entity;

import com.echosnap.backend.domain.apartment.entity.Apartment;
import com.echosnap.backend.domain.collectionarea.entity.CollectionArea;
import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 특정 날짜에만 적용되는 공식 배출 일정 예외입니다.
 *
 * 반복되는 정기 일정 자체를 수정하지 않고
 * 특정 날짜의 휴무, 시간 변경 등을 별도로 저장합니다.
 *
 * 공동주택:
 * Apartment + WasteItem
 *
 * 일반주택:
 * CollectionArea + CollectionWasteType
 *
 * 조회 시 해당 날짜의 ScheduleException이 존재하면
 * 정기 일정보다 우선 적용합니다.
 */
@Getter
@Entity
@Table(
    name = "schedule_exceptions",
    uniqueConstraints = {

        /**
         * 동일 공동주택 + 품목 + 날짜에는
         * 공식 예외 일정을 하나만 허용합니다.
         */
        @UniqueConstraint(
            name = "uk_schedule_exception_apartment_date",
            columnNames = {
                "apartment_id",
                "waste_item_id",
                "effective_date"
            }
        ),

        /**
         * 동일 수거구역 + 폐기물 종류 + 날짜에도
         * 공식 예외 일정을 하나만 허용합니다.
         */
        @UniqueConstraint(
            name = "uk_schedule_exception_collection_area_date",
            columnNames = {
                "collection_area_id",
                "collection_waste_type",
                "effective_date"
            }
        ),

        /**
         * 주민 TEMPORARY_CHANGE 제보 하나가
         * 여러 예외 일정으로 중복 승인되는 것을 막습니다.
         */
        @UniqueConstraint(
            name = "uk_schedule_exception_source_report",
            columnNames = {
                "source_report_id"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_schedule_exception_effective_date",
            columnList = "effective_date"
        ),
        @Index(
            name = "idx_schedule_exception_apartment",
            columnList = "apartment_id"
        ),
        @Index(
            name = "idx_schedule_exception_collection_area",
            columnList = "collection_area_id"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleException extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /*
   * =========================================================
   * 공동주택 대상
   * =========================================================
   */

  /**
   * 공동주택 일정 예외인 경우 대상 Apartment입니다.
   *
   * 일반주택 일정 예외에서는 null입니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "apartment_id"
  )
  private Apartment apartment;

  /**
   * 공동주택 일정 예외의 대상 품목입니다.
   *
   * 일반주택에서는 null입니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "waste_item_id"
  )
  private WasteItem wasteItem;

  /*
   * =========================================================
   * 일반주택 대상
   * =========================================================
   */

  /**
   * 일반주택 일정 예외인 경우
   * 대상 CollectionArea입니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "collection_area_id"
  )
  private CollectionArea collectionArea;

  /**
   * 일반주택 일정 예외의 폐기물 종류입니다.
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "collection_waste_type",
      length = 30
  )
  private CollectionWasteType collectionWasteType;

  /*
   * =========================================================
   * 제보 출처
   * =========================================================
   */

  /**
   * 어떤 주민 TEMPORARY_CHANGE 제보가
   * 관리자 승인되어 생성된 예외인지 기록합니다.
   *
   * 관리자 수동 등록 기능을 나중에 추가한다면
   * sourceReport는 null일 수도 있습니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "source_report_id"
  )
  private ScheduleReport sourceReport;

  /*
   * =========================================================
   * 예외 날짜
   * =========================================================
   */

  /**
   * 이 예외가 적용되는 날짜입니다.
   *
   * 반복 규칙이 아니라 특정 날짜 하나입니다.
   */
  @Column(
      name = "effective_date",
      nullable = false
  )
  private LocalDate effectiveDate;

  /*
   * =========================================================
   * 예외 내용
   * =========================================================
   */

  /**
   * 해당 날짜에 배출 또는 수거 자체가
   * 불가능한지 나타냅니다.
   *
   * true:
   * 해당 날짜 배출 불가
   *
   * false:
   * 수거는 진행하지만
   * 시간 또는 조건이 변경됨
   */
  @Column(
      name = "unavailable",
      nullable = false
  )
  private boolean unavailable;

  /**
   * 변경된 시작 시간입니다.
   *
   * unavailable == true이면 null입니다.
   */
  @Column(
      name = "start_time"
  )
  private LocalTime startTime;

  /**
   * 변경된 종료 시간입니다.
   *
   * 일반주택에서는
   * 20:00 ~ 02:00처럼 자정을 넘길 수 있습니다.
   */
  @Column(
      name = "end_time"
  )
  private LocalTime endTime;

  /**
   * 공동주택에서 해당 날짜만
   * 상시 배출 가능한 경우 사용합니다.
   *
   * 일반주택에서는 일반적으로 null입니다.
   */
  @Column(
      name = "always_available"
  )
  private Boolean alwaysAvailable;

  /**
   * 관리자가 사용자 화면에 표시할 수 있는
   * 예외 일정 설명입니다.
   *
   * 예:
   * "추석 연휴로 인해 당일 수거하지 않습니다."
   */
  @Column(
      name = "reason",
      length = 1000
  )
  private String reason;

  private ScheduleException(
      Apartment apartment,
      WasteItem wasteItem,
      CollectionArea collectionArea,
      CollectionWasteType collectionWasteType,
      ScheduleReport sourceReport,
      LocalDate effectiveDate,
      boolean unavailable,
      LocalTime startTime,
      LocalTime endTime,
      Boolean alwaysAvailable,
      String reason
  ) {
    this.apartment = apartment;
    this.wasteItem = wasteItem;

    this.collectionArea = collectionArea;
    this.collectionWasteType =
        collectionWasteType;

    this.sourceReport = sourceReport;

    this.effectiveDate = effectiveDate;

    this.unavailable = unavailable;

    this.startTime = startTime;
    this.endTime = endTime;

    this.alwaysAvailable =
        alwaysAvailable;

    this.reason = reason;
  }

  /**
   * 공동주택 특정 날짜 예외를 생성합니다.
   */
  public static ScheduleException createForApartment(
      Apartment apartment,
      WasteItem wasteItem,
      ScheduleReport sourceReport,
      LocalDate effectiveDate,
      boolean unavailable,
      LocalTime startTime,
      LocalTime endTime,
      Boolean alwaysAvailable,
      String reason
  ) {
    return new ScheduleException(
        apartment,
        wasteItem,
        null,
        null,
        sourceReport,
        effectiveDate,
        unavailable,
        startTime,
        endTime,
        alwaysAvailable,
        reason
    );
  }

  /**
   * 일반주택 특정 날짜 예외를 생성합니다.
   */
  public static ScheduleException createForCollectionArea(
      CollectionArea collectionArea,
      CollectionWasteType collectionWasteType,
      ScheduleReport sourceReport,
      LocalDate effectiveDate,
      boolean unavailable,
      LocalTime startTime,
      LocalTime endTime,
      String reason
  ) {
    return new ScheduleException(
        null,
        null,
        collectionArea,
        collectionWasteType,
        sourceReport,
        effectiveDate,
        unavailable,
        startTime,
        endTime,
        null,
        reason
    );
  }

  /**
   * 공동주택 예외인지 확인합니다.
   */
  public boolean isApartmentException() {
    return apartment != null;
  }

  /**
   * 일반주택 수거구역 예외인지 확인합니다.
   */
  public boolean isCollectionAreaException() {
    return collectionArea != null;
  }

  /**
   * 변경된 시간 범위가 존재하는지 확인합니다.
   */
  public boolean hasTimeWindow() {
    return !unavailable
        && startTime != null
        && endTime != null;
  }

  /**
   * 자정을 넘기는 일정인지 확인합니다.
   *
   * 예:
   *
   * 20:00 → 02:00
   *
   * 일반주택에서 실제로 발생할 수 있습니다.
   */
  public boolean isOvernight() {
    return hasTimeWindow()
        && endTime.isBefore(startTime);
  }
}
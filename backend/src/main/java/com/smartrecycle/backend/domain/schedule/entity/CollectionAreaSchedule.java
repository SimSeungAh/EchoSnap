package com.smartrecycle.backend.domain.schedule.entity;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
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

import java.time.LocalTime;

/**
 * 일반주택 사용자에게 적용되는
 * CollectionArea 기반 생활폐기물 공식 배출 일정입니다.
 *
 * 기존 RecycleSchedule은
 * Apartment + WasteItem 기반의 공동주택 공식 일정입니다.
 *
 * CollectionAreaSchedule은
 * 일반주택의 CollectionArea +
 * CollectionWasteType 기준 공식 일정을 저장합니다.
 *
 * 일정 규칙의 출처는
 *
 * PUBLIC_DATA
 * ADMIN_APPROVED_REPORT
 *
 * 두 가지로 구분합니다.
 */
@Getter
@Entity
@Table(
    name = "collection_area_schedules",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_collection_area_schedules_area_waste_type",
            columnNames = {
                "collection_area_id",
                "waste_type"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_collection_area_schedules_area",
            columnList = "collection_area_id"
        ),
        @Index(
            name = "idx_collection_area_schedules_waste_type",
            columnList = "waste_type"
        ),
        @Index(
            name = "idx_collection_area_schedules_source_type",
            columnList = "source_type"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionAreaSchedule
    extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 일정이 적용되는 일반주택 수거구역입니다.
   */
  @ManyToOne(
      fetch = FetchType.LAZY,
      optional = false
  )
  @JoinColumn(
      name = "collection_area_id",
      nullable = false
  )
  private CollectionArea collectionArea;

  /**
   * 폐기물 종류입니다.
   *
   * LIFE_WASTE
   * FOOD_WASTE
   * RECYCLABLE
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "waste_type",
      nullable = false,
      length = 30
  )
  private CollectionWasteType wasteType;

  /**
   * 현재 공식 일정 규칙의 출처입니다.
   *
   * PUBLIC_DATA:
   * 공공데이터 일정
   *
   * ADMIN_APPROVED_REPORT:
   * 주민 제보를 관리자가 승인하여
   * 공식 일정으로 반영한 값
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "source_type",
      nullable = false,
      length = 30
  )
  private CollectionAreaScheduleSourceType
      sourceType;

  /**
   * 배출 요일 표현입니다.
   *
   * 예:
   * 일+화+목
   * 월~금
   * 매일
   */
  @Column(
      name = "emission_days",
      length = 500
  )
  private String emissionDays;

  /**
   * 배출 시작 시간
   */
  @Column(
      name = "start_time"
  )
  private LocalTime startTime;

  /**
   * 배출 종료 시간
   *
   * 일반주택은
   * 20:00 ~ 02:00처럼
   * 자정을 넘기는 일정도 허용합니다.
   */
  @Column(
      name = "end_time"
  )
  private LocalTime endTime;

  /**
   * 지자체가 안내하는 배출 방법입니다.
   *
   * 주민 제보에서는 현재 이 값을 수정하지 않습니다.
   */
  @Column(
      name = "emission_method",
      length = 2000
  )
  private String emissionMethod;

  /**
   * 배출 장소
   */
  @Column(
      name = "emission_place",
      length = 500
  )
  private String emissionPlace;

  /**
   * 배출 장소 유형
   */
  @Column(
      name = "emission_place_type",
      length = 200
  )
  private String emissionPlaceType;

  /**
   * 공공데이터의 미수거일 안내 원문입니다.
   */
  @Column(
      name = "uncollected_day",
      length = 1000
  )
  private String uncollectedDay;

  private CollectionAreaSchedule(
      CollectionArea collectionArea,
      CollectionWasteType wasteType,
      CollectionAreaScheduleSourceType sourceType,
      String emissionDays,
      LocalTime startTime,
      LocalTime endTime,
      String emissionMethod,
      String emissionPlace,
      String emissionPlaceType,
      String uncollectedDay
  ) {
    this.collectionArea =
        collectionArea;

    this.wasteType =
        wasteType;

    this.sourceType =
        sourceType;

    this.emissionDays =
        emissionDays;

    this.startTime =
        startTime;

    this.endTime =
        endTime;

    this.emissionMethod =
        emissionMethod;

    this.emissionPlace =
        emissionPlace;

    this.emissionPlaceType =
        emissionPlaceType;

    this.uncollectedDay =
        uncollectedDay;
  }

  /**
   * 공공데이터를 기준으로
   * 공식 일정을 최초 생성합니다.
   */
  public static CollectionAreaSchedule
  createFromPublicData(
      CollectionArea collectionArea,
      CollectionWasteType wasteType,
      String emissionDays,
      LocalTime startTime,
      LocalTime endTime,
      String emissionMethod,
      String emissionPlace,
      String emissionPlaceType,
      String uncollectedDay
  ) {
    return new CollectionAreaSchedule(
        collectionArea,
        wasteType,
        CollectionAreaScheduleSourceType.PUBLIC_DATA,
        emissionDays,
        startTime,
        endTime,
        emissionMethod,
        emissionPlace,
        emissionPlaceType,
        uncollectedDay
    );
  }

  /**
   * 주민 INITIAL_SCHEDULE 제보가
   * 관리자 승인된 경우
   * 새로운 공식 일정을 생성합니다.
   *
   * 주민 제보에는 현재
   * 배출 방법/장소 데이터가 없으므로
   * 해당 필드는 null로 시작합니다.
   */
  public static CollectionAreaSchedule
  createFromApprovedReport(
      CollectionArea collectionArea,
      CollectionWasteType wasteType,
      String emissionDays,
      LocalTime startTime,
      LocalTime endTime
  ) {
    return new CollectionAreaSchedule(
        collectionArea,
        wasteType,
        CollectionAreaScheduleSourceType
            .ADMIN_APPROVED_REPORT,
        emissionDays,
        startTime,
        endTime,
        null,
        null,
        null,
        null
    );
  }

  /**
   * 공공데이터가 다시 동기화됐을 때
   * 일정을 최신 데이터로 갱신합니다.
   *
   * 단, 주민 제보를 관리자가 승인하여
   * 공식 일정 규칙을 직접 보정한 상태라면
   * emissionDays / startTime / endTime은
   * 공공데이터가 덮어쓰지 않습니다.
   *
   * 배출 방법, 장소, 미수거일 같은
   * 부가 정보는 계속 최신 공공데이터로 갱신합니다.
   */
  public void updateFromPublicData(
      String emissionDays,
      LocalTime startTime,
      LocalTime endTime,
      String emissionMethod,
      String emissionPlace,
      String emissionPlaceType,
      String uncollectedDay
  ) {
    if (
        sourceType
            != CollectionAreaScheduleSourceType
            .ADMIN_APPROVED_REPORT
    ) {
      this.emissionDays =
          emissionDays;

      this.startTime =
          startTime;

      this.endTime =
          endTime;

      this.sourceType =
          CollectionAreaScheduleSourceType
              .PUBLIC_DATA;
    }

    /*
     * 일정 규칙을 주민 승인 값으로 보호하더라도
     * 공공데이터의 부가 정보는 계속 최신화합니다.
     */
    this.emissionMethod =
        emissionMethod;

    this.emissionPlace =
        emissionPlace;

    this.emissionPlaceType =
        emissionPlaceType;

    this.uncollectedDay =
        uncollectedDay;
  }

  /**
   * 주민 SCHEDULE_CORRECTION 제보가
   * 관리자 승인된 경우
   * 공식 일정 규칙을 수정합니다.
   *
   * 기존 공공데이터의 배출 방법/장소 등은
   * 그대로 보존합니다.
   */
  public void updateFromApprovedReport(
      String emissionDays,
      LocalTime startTime,
      LocalTime endTime
  ) {
    this.emissionDays =
        emissionDays;

    this.startTime =
        startTime;

    this.endTime =
        endTime;

    this.sourceType =
        CollectionAreaScheduleSourceType
            .ADMIN_APPROVED_REPORT;
  }

  /**
   * 현재 공식 일정이
   * 관리자 승인 주민 제보로 보정된 상태인지 확인합니다.
   */
  public boolean isAdminApprovedOverride() {
    return sourceType
        == CollectionAreaScheduleSourceType
        .ADMIN_APPROVED_REPORT;
  }

  /**
   * 시작/종료 시간이 모두 존재하는지 확인합니다.
   */
  public boolean hasTimeWindow() {
    return startTime != null
        && endTime != null;
  }

  /**
   * 자정을 넘기는 일정인지 확인합니다.
   *
   * 예:
   * 20:00 ~ 02:00
   */
  public boolean isOvernight() {
    if (!hasTimeWindow()) {
      return false;
    }

    return endTime.isBefore(
        startTime
    );
  }
}
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
 * CollectionArea 기반 생활폐기물 배출 일정입니다.
 *
 * 기존 RecycleSchedule은 Apartment + WasteItem 단위의
 * 공동주택 공식 일정 전용으로 유지합니다.
 *
 * CollectionAreaSchedule은 행정안전부
 * 생활쓰레기배출정보 공공데이터의
 * 생활쓰레기 / 음식물쓰레기 / 재활용품 단위 일정을 저장합니다.
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
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionAreaSchedule extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 이 일정이 적용되는 지자체 수거구역입니다.
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
   * 공공데이터 기준 폐기물 종류입니다.
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
   * 공공데이터에서 제공하는 원본 배출요일 문자열입니다.
   *
   * 예:
   * 일+월+화+수+목+금
   * 일+화+목
   * 매일
   *
   * 지자체마다 표현 형식이 다를 수 있으므로
   * 원본 문자열을 보존합니다.
   */
  @Column(
      name = "emission_days",
      length = 500
  )
  private String emissionDays;

  /**
   * 배출 시작 시간입니다.
   *
   * 공공데이터에 시간이 없거나
   * 해석할 수 없는 경우 null일 수 있습니다.
   */
  @Column(
      name = "start_time"
  )
  private LocalTime startTime;

  /**
   * 배출 종료 시간입니다.
   *
   * 20:00 ~ 02:00처럼
   * 다음 날로 넘어가는 일정도 허용합니다.
   */
  @Column(
      name = "end_time"
  )
  private LocalTime endTime;

  /**
   * 지자체가 안내하는 실제 배출 방법입니다.
   */
  @Column(
      name = "emission_method",
      length = 2000
  )
  private String emissionMethod;

  /**
   * 배출장소입니다.
   */
  @Column(
      name = "emission_place",
      length = 500
  )
  private String emissionPlace;

  /**
   * 배출장소 유형입니다.
   */
  @Column(
      name = "emission_place_type",
      length = 200
  )
  private String emissionPlaceType;

  /**
   * 공공데이터의 미수거일 안내입니다.
   *
   * 예:
   * 공휴일
   * 명절 연휴
   * 토요일
   *
   * 자유 텍스트일 수 있기 때문에
   * 우선 원문 그대로 보관합니다.
   */
  @Column(
      name = "uncollected_day",
      length = 1000
  )
  private String uncollectedDay;

  private CollectionAreaSchedule(
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
    this.collectionArea = collectionArea;
    this.wasteType = wasteType;
    this.emissionDays = emissionDays;
    this.startTime = startTime;
    this.endTime = endTime;
    this.emissionMethod = emissionMethod;
    this.emissionPlace = emissionPlace;
    this.emissionPlaceType = emissionPlaceType;
    this.uncollectedDay = uncollectedDay;
  }

  /**
   * 공공데이터를 기준으로
   * CollectionArea 일정을 새로 생성합니다.
   */
  public static CollectionAreaSchedule createFromPublicData(
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
   * 같은 CollectionArea + 폐기물 종류의
   * 공공데이터가 다시 들어오면
   * 기존 일정을 최신 값으로 갱신합니다.
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
    this.emissionDays = emissionDays;
    this.startTime = startTime;
    this.endTime = endTime;
    this.emissionMethod = emissionMethod;
    this.emissionPlace = emissionPlace;
    this.emissionPlaceType = emissionPlaceType;
    this.uncollectedDay = uncollectedDay;
  }

  /**
   * 시작 시간과 종료 시간이 모두 존재하는지 확인합니다.
   */
  public boolean hasTimeWindow() {
    return startTime != null
        && endTime != null;
  }

  /**
   * 자정을 넘어가는 일정인지 확인합니다.
   *
   * 예:
   * 20:00 ~ 02:00
   *
   * 종료 시간이 시작 시간보다 이르면
   * 다음 날 종료되는 일정으로 판단합니다.
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
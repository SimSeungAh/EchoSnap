package com.smartrecycle.backend.domain.collectionarea.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 일반주택 사용자에게 적용되는
 * 행정구역·생활폐기물 수거구역 정보입니다.
 *
 * 하나의 CollectionArea는 공공데이터 내용에 따라
 * 생활쓰레기, 음식물쓰레기, 재활용품 중
 * 하나 이상에 적용될 수 있습니다.
 */
@Getter
@Entity
@Table(
    name = "collection_areas",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_collection_areas_source_external_number",
            columnNames = {
                "source_type",
                "external_management_number"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_collection_areas_region",
            columnList = "sido,sigungu"
        ),
        @Index(
            name = "idx_collection_areas_area_name",
            columnList = "area_name"
        ),
        @Index(
            name = "idx_collection_areas_target_area",
            columnList = "target_area_name"
        ),
        @Index(
            name = "idx_collection_areas_active",
            columnList = "active"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionArea extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 수거구역 데이터의 출처
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "source_type",
      nullable = false,
      length = 30
  )
  private CollectionAreaSourceType sourceType;

  /**
   * 외부 공공데이터 관리번호
   *
   * 행정안전부 생활쓰레기배출정보의 MNG_NO를 저장합니다.
   */
  @Column(
      name = "external_management_number",
      length = 100
  )
  private String externalManagementNumber;

  /**
   * 시/도
   */
  @Column(
      nullable = false,
      length = 50
  )
  private String sido;

  /**
   * 시/군/구
   */
  @Column(
      nullable = false,
      length = 100
  )
  private String sigungu;

  /**
   * 공공데이터 관리구역명
   */
  @Column(
      name = "area_name",
      nullable = false,
      length = 200
  )
  private String areaName;

  /**
   * 관리구역 대상지역명
   *
   * 예:
   * 산격1동+관문동+태전1동
   */
  @Column(
      name = "target_area_name",
      length = 500
  )
  private String targetAreaName;

  /**
   * 이 수거구역이 실제로 담당하는 폐기물 종류입니다.
   *
   * 한 공공데이터 레코드가
   * 여러 종류의 수거 기준을 동시에 제공할 수도 있으므로
   * Set으로 관리합니다.
   */
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "collection_area_waste_types",
      joinColumns = @JoinColumn(
          name = "collection_area_id"
      )
  )
  @Enumerated(EnumType.STRING)
  @Column(
      name = "waste_type",
      nullable = false,
      length = 30
  )
  @Getter(AccessLevel.NONE)
  private Set<CollectionWasteType> supportedWasteTypes =
      new HashSet<>();

  /**
   * 외부 공공데이터의 기준일자
   */
  @Column(
      name = "source_reference_date"
  )
  private LocalDate sourceReferenceDate;

  /**
   * 현재 서비스에서 사용하는 수거구역인지 여부
   */
  @Column(
      nullable = false
  )
  private boolean active;

  private CollectionArea(
      CollectionAreaSourceType sourceType,
      String externalManagementNumber,
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName,
      Set<CollectionWasteType> supportedWasteTypes,
      LocalDate sourceReferenceDate
  ) {
    this.sourceType = sourceType;
    this.externalManagementNumber =
        externalManagementNumber;
    this.sido = sido;
    this.sigungu = sigungu;
    this.areaName = areaName;
    this.targetAreaName = targetAreaName;

    replaceSupportedWasteTypes(
        supportedWasteTypes
    );

    this.sourceReferenceDate =
        sourceReferenceDate;
    this.active = true;
  }

  /**
   * 행정안전부 공공데이터를 기반으로
   * 수거구역을 생성합니다.
   */
  public static CollectionArea createFromPublicData(
      String externalManagementNumber,
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName,
      Set<CollectionWasteType> supportedWasteTypes,
      LocalDate sourceReferenceDate
  ) {
    return new CollectionArea(
        CollectionAreaSourceType
            .MOIS_HOUSEHOLD_WASTE,
        externalManagementNumber,
        sido,
        sigungu,
        areaName,
        targetAreaName,
        supportedWasteTypes,
        sourceReferenceDate
    );
  }

  /**
   * 관리자가 직접 수거구역을 생성합니다.
   */
  public static CollectionArea createManual(
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName,
      Set<CollectionWasteType> supportedWasteTypes
  ) {
    return new CollectionArea(
        CollectionAreaSourceType.MANUAL,
        null,
        sido,
        sigungu,
        areaName,
        targetAreaName,
        supportedWasteTypes,
        null
    );
  }

  /**
   * 같은 공공데이터 관리번호가 다시 조회된 경우
   * 최신 내용으로 갱신합니다.
   */
  public void updateFromPublicData(
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName,
      Set<CollectionWasteType> supportedWasteTypes,
      LocalDate sourceReferenceDate
  ) {
    this.sido = sido;
    this.sigungu = sigungu;
    this.areaName = areaName;
    this.targetAreaName = targetAreaName;

    replaceSupportedWasteTypes(
        supportedWasteTypes
    );

    this.sourceReferenceDate =
        sourceReferenceDate;

    this.active = true;
  }

  /**
   * 관리자가 직접 관리하는
   * 수거구역 정보를 수정합니다.
   */
  public void updateManual(
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName,
      Set<CollectionWasteType> supportedWasteTypes
  ) {
    this.sido = sido;
    this.sigungu = sigungu;
    this.areaName = areaName;
    this.targetAreaName = targetAreaName;

    replaceSupportedWasteTypes(
        supportedWasteTypes
    );
  }

  /**
   * 특정 폐기물 종류를
   * 이 수거구역이 담당하는지 확인합니다.
   */
  public boolean supports(
      CollectionWasteType wasteType
  ) {
    return supportedWasteTypes.contains(
        wasteType
    );
  }

  /**
   * 외부에서 Set 자체를 수정하지 못하도록
   * 읽기 전용 형태로 반환합니다.
   */
  public Set<CollectionWasteType>
  getSupportedWasteTypes() {
    return Collections.unmodifiableSet(
        supportedWasteTypes
    );
  }

  /**
   * 지원 폐기물 종류를
   * 공공데이터 최신 내용으로 교체합니다.
   */
  private void replaceSupportedWasteTypes(
      Set<CollectionWasteType> supportedWasteTypes
  ) {
    this.supportedWasteTypes.clear();

    if (supportedWasteTypes == null) {
      return;
    }

    this.supportedWasteTypes.addAll(
        supportedWasteTypes
    );
  }

  /**
   * 더 이상 사용하지 않는 수거구역을 비활성화합니다.
   */
  public void deactivate() {
    this.active = false;
  }

  /**
   * 비활성화된 수거구역을 다시 사용합니다.
   */
  public void activate() {
    this.active = true;
  }
}
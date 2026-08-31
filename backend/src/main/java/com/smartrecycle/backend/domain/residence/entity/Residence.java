package com.smartrecycle.backend.domain.residence.entity;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 주소 기반 지역 수거 일정을 사용하는
 * 일반주택 사용자의 거주지 정보입니다.
 *
 * 주소와 행정구역 정보,
 * 일반주택 세부 유형을 보관하고,
 * 폐기물 종류별 CollectionArea 적용 관계를 관리합니다.
 */
@Getter
@Entity
@Table(
    name = "residences",
    indexes = {
        @Index(
            name = "idx_residences_legal_dong_code",
            columnList = "legal_dong_code"
        ),
        @Index(
            name = "idx_residences_administrative_dong_code",
            columnList = "administrative_dong_code"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Residence extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 일반주택의 세부 주거 형태입니다.
   *
   * 기존 Residence 데이터와의 호환을 위해
   * 현재 개발 단계에서는 nullable로 둡니다.
   *
   * 모든 기존 데이터가 갱신된 뒤
   * 운영 마이그레이션에서는 NOT NULL로
   * 변경할 수 있습니다.
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "general_housing_type",
      length = 30
  )
  private GeneralHousingType generalHousingType;

  @Column(
      name = "address_name",
      nullable = false,
      length = 255
  )
  private String addressName;

  @Column(
      name = "road_address",
      length = 255
  )
  private String roadAddress;

  @Column(
      name = "jibun_address",
      length = 255
  )
  private String jibunAddress;

  @Column(
      name = "building_name",
      length = 200
  )
  private String buildingName;

  @Column(
      name = "zone_no",
      length = 10
  )
  private String zoneNo;

  @Column(
      nullable = false,
      length = 50
  )
  private String sido;

  @Column(
      nullable = false,
      length = 100
  )
  private String sigungu;

  @Column(
      name = "legal_dong",
      length = 100
  )
  private String legalDong;

  @Column(
      name = "administrative_dong",
      length = 100
  )
  private String administrativeDong;

  @Column(
      name = "legal_dong_code",
      length = 20
  )
  private String legalDongCode;

  @Column(
      name = "administrative_dong_code",
      length = 20
  )
  private String administrativeDongCode;

  @Column(
      precision = 10,
      scale = 7
  )
  private BigDecimal latitude;

  @Column(
      precision = 10,
      scale = 7
  )
  private BigDecimal longitude;

  /**
   * 현재 Residence에 적용되는
   * 폐기물 종류별 수거구역 관계입니다.
   *
   * Residence가 삭제되면 연결 정보도 함께 삭제하지만,
   * CollectionArea 자체는 삭제하지 않습니다.
   */
  @OneToMany(
      mappedBy = "residence",
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  @Getter(AccessLevel.NONE)
  private final List<ResidenceCollectionArea>
      collectionAreaMappings = new ArrayList<>();

  private Residence(
      GeneralHousingType generalHousingType,
      String addressName,
      String roadAddress,
      String jibunAddress,
      String buildingName,
      String zoneNo,
      String sido,
      String sigungu,
      String legalDong,
      String administrativeDong,
      String legalDongCode,
      String administrativeDongCode,
      BigDecimal latitude,
      BigDecimal longitude
  ) {
    this.generalHousingType =
        generalHousingType;

    this.addressName = addressName;
    this.roadAddress = roadAddress;
    this.jibunAddress = jibunAddress;
    this.buildingName = buildingName;
    this.zoneNo = zoneNo;
    this.sido = sido;
    this.sigungu = sigungu;
    this.legalDong = legalDong;
    this.administrativeDong =
        administrativeDong;
    this.legalDongCode = legalDongCode;
    this.administrativeDongCode =
        administrativeDongCode;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public static Residence create(
      GeneralHousingType generalHousingType,
      String addressName,
      String roadAddress,
      String jibunAddress,
      String buildingName,
      String zoneNo,
      String sido,
      String sigungu,
      String legalDong,
      String administrativeDong,
      String legalDongCode,
      String administrativeDongCode,
      BigDecimal latitude,
      BigDecimal longitude
  ) {
    return new Residence(
        generalHousingType,
        addressName,
        roadAddress,
        jibunAddress,
        buildingName,
        zoneNo,
        sido,
        sigungu,
        legalDong,
        administrativeDong,
        legalDongCode,
        administrativeDongCode,
        latitude,
        longitude
    );
  }

  /**
   * 사용자의 일반주택 정보와 주소를 변경합니다.
   *
   * 주소 또는 주거 형태가 바뀌면
   * 기존 수거구역 적용 관계는 더 이상
   * 유효하다고 보장할 수 없으므로 제거합니다.
   *
   * 이후 새 정보 기준으로 다시 매핑합니다.
   */
  public void update(
      GeneralHousingType generalHousingType,
      String addressName,
      String roadAddress,
      String jibunAddress,
      String buildingName,
      String zoneNo,
      String sido,
      String sigungu,
      String legalDong,
      String administrativeDong,
      String legalDongCode,
      String administrativeDongCode,
      BigDecimal latitude,
      BigDecimal longitude
  ) {
    this.generalHousingType =
        generalHousingType;

    this.addressName = addressName;
    this.roadAddress = roadAddress;
    this.jibunAddress = jibunAddress;
    this.buildingName = buildingName;
    this.zoneNo = zoneNo;
    this.sido = sido;
    this.sigungu = sigungu;
    this.legalDong = legalDong;
    this.administrativeDong =
        administrativeDong;
    this.legalDongCode = legalDongCode;
    this.administrativeDongCode =
        administrativeDongCode;
    this.latitude = latitude;
    this.longitude = longitude;

    clearCollectionAreas();
  }

  /**
   * 특정 폐기물 종류의 수거구역을 설정합니다.
   *
   * 이미 같은 폐기물 종류의 매핑이 존재한다면
   * 새로운 연결을 계속 생성하지 않고
   * 기존 매핑의 CollectionArea만 변경합니다.
   */
  public void assignCollectionArea(
      CollectionArea collectionArea,
      CollectionWasteType wasteType
  ) {
    ResidenceCollectionArea existing =
        findCollectionAreaMapping(
            wasteType
        );

    if (existing != null) {
      existing.changeCollectionArea(
          collectionArea
      );

      return;
    }

    collectionAreaMappings.add(
        ResidenceCollectionArea.create(
            this,
            collectionArea,
            wasteType
        )
    );
  }

  /**
   * 특정 폐기물 종류의 수거구역 연결을 제거합니다.
   */
  public void removeCollectionArea(
      CollectionWasteType wasteType
  ) {
    collectionAreaMappings.removeIf(
        mapping ->
            mapping.getWasteType()
                == wasteType
    );
  }

  /**
   * 주소 또는 주거형태 변경 등으로
   * 기존 매핑이 더 이상 유효하지 않을 때
   * 전체 연결을 제거합니다.
   */
  public void clearCollectionAreas() {
    collectionAreaMappings.clear();
  }

  /**
   * 외부에서 컬렉션 자체를 수정하지 못하도록
   * 읽기 전용 List로 반환합니다.
   */
  public List<ResidenceCollectionArea>
  getCollectionAreaMappings() {
    return Collections.unmodifiableList(
        collectionAreaMappings
    );
  }

  /**
   * 특정 폐기물 종류에 현재 적용되는
   * 수거구역 관계를 찾습니다.
   */
  private ResidenceCollectionArea
  findCollectionAreaMapping(
      CollectionWasteType wasteType
  ) {
    return collectionAreaMappings.stream()
        .filter(
            mapping ->
                mapping.getWasteType()
                    == wasteType
        )
        .findFirst()
        .orElse(null);
  }
}
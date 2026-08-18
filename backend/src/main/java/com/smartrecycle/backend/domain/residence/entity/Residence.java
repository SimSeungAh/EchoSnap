package com.smartrecycle.backend.domain.residence.entity;

import com.smartrecycle.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주소 기반 지역 수거 일정을 사용하는
 * 일반주택 사용자의 거주지 정보입니다.
 *
 * 도로명/지번 주소와 행정구역 코드, 좌표를 저장하며
 * 이후 CollectionArea와 연결하여
 * 실제 지자체/수거구역 배출 일정을 적용합니다.
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
   * 주소 검색 API에서 반환된 대표 주소입니다.
   *
   * 사용자가 검색하고 선택한 주소를
   * 화면에 표시할 때 기본값으로 사용할 수 있습니다.
   */
  @Column(
      name = "address_name",
      nullable = false,
      length = 255
  )
  private String addressName;

  /**
   * 도로명 주소
   *
   * 검색 결과에 도로명 주소가 없는 경우에는
   * null일 수 있습니다.
   */
  @Column(
      name = "road_address",
      length = 255
  )
  private String roadAddress;

  /**
   * 지번 주소
   *
   * 검색 결과에 지번 주소가 없는 경우에는
   * null일 수 있습니다.
   */
  @Column(
      name = "jibun_address",
      length = 255
  )
  private String jibunAddress;

  /**
   * 건물명
   *
   * 주소 검색 결과에 건물명이 없는 경우
   * null일 수 있습니다.
   */
  @Column(
      name = "building_name",
      length = 200
  )
  private String buildingName;

  /**
   * 우편번호
   */
  @Column(
      name = "zone_no",
      length = 10
  )
  private String zoneNo;

  /**
   * 시/도
   *
   * 예:
   * 서울특별시
   * 부산광역시
   * 경기도
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
   * 법정동
   *
   * 이후 주소 기반 수거구역을 판별할 때
   * 보조 정보로 사용합니다.
   */
  @Column(
      name = "legal_dong",
      length = 100
  )
  private String legalDong;

  /**
   * 행정동
   *
   * 지자체 및 수거구역 데이터가
   * 행정동 단위로 제공되는 경우 활용합니다.
   */
  @Column(
      name = "administrative_dong",
      length = 100
  )
  private String administrativeDong;

  /**
   * 법정동 코드
   *
   * 주소 문자열보다 안정적으로
   * 지역 데이터를 연결하기 위한 값입니다.
   */
  @Column(
      name = "legal_dong_code",
      length = 20
  )
  private String legalDongCode;

  /**
   * 행정동 코드
   *
   * 이후 CollectionArea 매핑에 활용합니다.
   */
  @Column(
      name = "administrative_dong_code",
      length = 20
  )
  private String administrativeDongCode;

  /**
   * 위도
   */
  @Column(
      precision = 10,
      scale = 7
  )
  private BigDecimal latitude;

  /**
   * 경도
   */
  @Column(
      precision = 10,
      scale = 7
  )
  private BigDecimal longitude;

  private Residence(
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
    this.addressName = addressName;
    this.roadAddress = roadAddress;
    this.jibunAddress = jibunAddress;
    this.buildingName = buildingName;
    this.zoneNo = zoneNo;
    this.sido = sido;
    this.sigungu = sigungu;
    this.legalDong = legalDong;
    this.administrativeDong = administrativeDong;
    this.legalDongCode = legalDongCode;
    this.administrativeDongCode =
        administrativeDongCode;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  /**
   * 사용자가 주소 검색 결과를 선택하여
   * 일반주택 거주지를 처음 설정할 때 사용합니다.
   */
  public static Residence create(
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
   * 일반주택 사용자가 주소를 변경할 때
   * 기존 Residence 정보를 갱신합니다.
   */
  public void update(
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
    this.addressName = addressName;
    this.roadAddress = roadAddress;
    this.jibunAddress = jibunAddress;
    this.buildingName = buildingName;
    this.zoneNo = zoneNo;
    this.sido = sido;
    this.sigungu = sigungu;
    this.legalDong = legalDong;
    this.administrativeDong = administrativeDong;
    this.legalDongCode = legalDongCode;
    this.administrativeDongCode =
        administrativeDongCode;
    this.latitude = latitude;
    this.longitude = longitude;
  }
}
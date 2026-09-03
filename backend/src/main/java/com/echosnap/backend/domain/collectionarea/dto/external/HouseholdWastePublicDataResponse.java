package com.echosnap.backend.domain.collectionarea.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 행정안전부 생활쓰레기배출정보 조회서비스의
 * JSON 응답을 매핑하는 외부 API DTO입니다.
 *
 * 외부 데이터의 원본 필드 구조를 그대로 받아들이고,
 * CollectionArea 및 RecycleSchedule로 변환하는 책임은
 * 이후 동기화 Service가 담당합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseholdWastePublicDataResponse(

    Response response

) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Response(

      Header header,

      Body body

  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Header(

      String resultCode,

      String resultMsg

  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Body(

      String dataType,

      Items items,

      Integer numOfRows,

      Integer pageNo,

      Integer totalCount

  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Items(

      List<Item> item

  ) {
  }

  /**
   * 생활쓰레기배출정보의 실제 한 건입니다.
   *
   * 외부 API 값은 빈 문자열이나
   * 다양한 형식으로 내려올 수 있으므로
   * 날짜/시간도 이 DTO에서는 String으로 수신합니다.
   *
   * 실제 LocalDate, LocalTime, DayOfWeek 변환은
   * 내부 도메인으로 변환할 때 수행합니다.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Item(

      /**
       * 시도명
       */
      @JsonProperty("CTPV_NM")
      String sido,

      /**
       * 시군구명
       */
      @JsonProperty("SGG_NM")
      String sigungu,

      /**
       * 배출정보 관리번호
       */
      @JsonProperty("MNG_NO")
      String managementNumber,

      /**
       * 관리구역명
       */
      @JsonProperty("MNG_ZONE_NM")
      String managementZoneName,

      /**
       * 관리구역 대상지역명
       */
      @JsonProperty("MNG_ZONE_TRGT_RGN_NM")
      String managementZoneTargetRegionName,

      /**
       * 개방자치단체 그룹코드
       */
      @JsonProperty("OPN_ATMY_GRP_CD")
      String openAutonomyGroupCode,

      /**
       * 배출장소
       */
      @JsonProperty("EMSN_PLC")
      String emissionPlace,

      /**
       * 배출장소 유형
       */
      @JsonProperty("EMSN_PLC_TYPE")
      String emissionPlaceType,

      /*
       * 일반 생활쓰레기
       */

      @JsonProperty("LF_WST_EMSN_MTHD")
      String lifeWasteEmissionMethod,

      @JsonProperty("LF_WST_EMSN_DOW")
      String lifeWasteEmissionDays,

      @JsonProperty("LF_WST_EMSN_BGNG_TM")
      String lifeWasteEmissionStartTime,

      @JsonProperty("LF_WST_EMSN_END_TM")
      String lifeWasteEmissionEndTime,

      /*
       * 음식물쓰레기
       */

      @JsonProperty("FOD_WST_EMSN_MTHD")
      String foodWasteEmissionMethod,

      @JsonProperty("FOD_WST_EMSN_DOW")
      String foodWasteEmissionDays,

      @JsonProperty("FOD_WST_EMSN_BGNG_TM")
      String foodWasteEmissionStartTime,

      @JsonProperty("FOD_WST_EMSN_END_TM")
      String foodWasteEmissionEndTime,

      /*
       * 재활용품
       */

      @JsonProperty("RCYCL_EMSN_MTHD")
      String recycleEmissionMethod,

      @JsonProperty("RCYCL_EMSN_DOW")
      String recycleEmissionDays,

      @JsonProperty("RCYCL_EMSN_BGNG_TM")
      String recycleEmissionStartTime,

      @JsonProperty("RCYCL_EMSN_END_TM")
      String recycleEmissionEndTime,

      /*
       * 임시 대형폐기물
       */

      @JsonProperty("TMPRY_BULK_WASTE_EMSN_MTHD")
      String temporaryBulkWasteEmissionMethod,

      @JsonProperty("TMPRY_BULK_WASTE_EMSN_PLC")
      String temporaryBulkWasteEmissionPlace,

      @JsonProperty("TMPRY_BULK_WASTE_EMSN_BGNG_TM")
      String temporaryBulkWasteEmissionStartTime,

      @JsonProperty("TMPRY_BULK_WASTE_EMSN_END_TM")
      String temporaryBulkWasteEmissionEndTime,

      /**
       * 미수거일
       */
      @JsonProperty("UNCLLT_DAY")
      String uncollectedDay,

      /**
       * 관리부서명
       */
      @JsonProperty("MNG_DEPT_NM")
      String managementDepartmentName,

      /**
       * 관리부서 전화번호
       */
      @JsonProperty("MNG_DEPT_TELNO")
      String managementDepartmentTelephone,

      /**
       * 데이터 기준일자
       */
      @JsonProperty("DAT_CRTR_YMD")
      String dataReferenceDate,

      /**
       * 데이터 갱신시점
       */
      @JsonProperty("DAT_UPDT_PNT")
      String dataUpdatePoint,

      /**
       * 데이터 갱신 구분
       *
       * 실제 응답에서 I, U 등의 값이 확인됩니다.
       */
      @JsonProperty("DAT_UPDT_SE")
      String dataUpdateType,

      /**
       * 최종 수정시점
       */
      @JsonProperty("LAST_MDFCN_PNT")
      String lastModifiedPoint

  ) {
  }
}
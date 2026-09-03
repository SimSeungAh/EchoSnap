package com.echosnap.backend.domain.schedule.dto.admin;

import com.echosnap.backend.domain.collectionarea.entity.CollectionArea;
import com.echosnap.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
import com.echosnap.backend.domain.schedule.entity.CollectionAreaSchedule;
import com.echosnap.backend.domain.schedule.entity.CollectionAreaScheduleSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdminCollectionAreaScheduleDtos {

  private AdminCollectionAreaScheduleDtos() {
  }

  public record ScheduleResponse(
      Long id,
      Long collectionAreaId,
      String collectionAreaName,
      String sido,
      String sigungu,
      CollectionWasteType wasteType,
      CollectionAreaScheduleSourceType sourceType,
      String emissionDays,
      LocalTime startTime,
      LocalTime endTime,
      boolean overnight,
      String emissionMethod,
      String emissionPlace,
      String emissionPlaceType,
      String uncollectedDay,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {

    public static ScheduleResponse from(
        CollectionAreaSchedule schedule
    ) {
      CollectionArea area =
          schedule.getCollectionArea();

      return new ScheduleResponse(
          schedule.getId(),
          area.getId(),
          area.getAreaName(),
          area.getSido(),
          area.getSigungu(),
          schedule.getWasteType(),
          schedule.getSourceType(),
          schedule.getEmissionDays(),
          schedule.getStartTime(),
          schedule.getEndTime(),
          schedule.isOvernight(),
          schedule.getEmissionMethod(),
          schedule.getEmissionPlace(),
          schedule.getEmissionPlaceType(),
          schedule.getUncollectedDay(),
          schedule.getCreatedAt(),
          schedule.getUpdatedAt()
      );
    }
  }

  /**
   * 실제 CollectionArea 원본 하나의 일정 현황.
   *
   * 지역 그룹의 상세 보기에서 사용합니다.
   */
  public record AreaScheduleCoverageResponse(
      Long collectionAreaId,
      String collectionAreaName,
      CollectionAreaSourceType areaSourceType,
      String externalManagementNumber,
      String sido,
      String sigungu,
      String targetAreaName,
      boolean active,
      List<CollectionWasteType> supportedWasteTypes,
      List<ScheduleResponse> schedules,
      List<CollectionWasteType> missingWasteTypes
  ) {

    public static AreaScheduleCoverageResponse from(
        CollectionArea area,
        List<CollectionAreaSchedule> schedules
    ) {
      Set<CollectionWasteType>
          registeredWasteTypes =
          new HashSet<>();

      for (
          CollectionAreaSchedule schedule
          : schedules
      ) {
        registeredWasteTypes.add(
            schedule.getWasteType()
        );
      }

      List<CollectionWasteType>
          supportedWasteTypes =
          area.getSupportedWasteTypes()
              .stream()
              .sorted()
              .toList();

      List<CollectionWasteType>
          missingWasteTypes =
          supportedWasteTypes
              .stream()
              .filter(
                  wasteType ->
                      !registeredWasteTypes
                          .contains(
                              wasteType
                          )
              )
              .toList();

      List<ScheduleResponse>
          scheduleResponses =
          schedules
              .stream()
              .sorted(
                  (first, second) ->
                      first.getWasteType()
                          .name()
                          .compareTo(
                              second
                                  .getWasteType()
                                  .name()
                          )
              )
              .map(
                  ScheduleResponse::from
              )
              .toList();

      return new AreaScheduleCoverageResponse(
          area.getId(),
          area.getAreaName(),
          area.getSourceType(),
          area.getExternalManagementNumber(),
          area.getSido(),
          area.getSigungu(),
          area.getTargetAreaName(),
          area.isActive(),
          supportedWasteTypes,
          scheduleResponses,
          missingWasteTypes
      );
    }
  }

  /**
   * 지역 그룹 안에서 폐기물 종류별 일정 등록 현황.
   *
   * 예:
   *
   * LIFE_WASTE
   * 지원 수거구역 12개
   * 일정 등록 12개
   * 미등록 0개
   */
  public record WasteTypeCoverageResponse(
      CollectionWasteType wasteType,
      int supportedAreaCount,
      int registeredAreaCount,
      int missingAreaCount
  ) {
  }

  /**
   * 관리자 배출 일정 목록의 최상위 응답.
   *
   * CollectionArea 원본 1개가 아니라
   *
   * 시도 + 시군구 + 수거구역 + 대상지역 + 출처 + 활성상태
   *
   * 를 하나의 화면 행으로 반환합니다.
   */
  public record AreaScheduleGroupResponse(
      Long representativeCollectionAreaId,
      String collectionAreaName,
      String sido,
      String sigungu,
      String targetAreaName,
      CollectionAreaSourceType areaSourceType,
      boolean active,
      int collectionAreaCount,
      boolean allSchedulesRegistered,
      List<CollectionWasteType> supportedWasteTypes,
      List<WasteTypeCoverageResponse> wasteTypeCoverage,
      List<AreaScheduleCoverageResponse> areas
  ) {

    public static AreaScheduleGroupResponse from(
        List<CollectionArea> areas,
        Map<Long, List<CollectionAreaSchedule>>
            schedulesByAreaId
    ) {
      if (
          areas == null
              || areas.isEmpty()
      ) {
        throw new IllegalArgumentException(
            "지역 그룹에는 최소 1개의 CollectionArea가 필요합니다."
        );
      }

      CollectionArea representative =
          areas.get(0);

      List<AreaScheduleCoverageResponse>
          areaResponses =
          areas.stream()
              .map(
                  area ->
                      AreaScheduleCoverageResponse
                          .from(
                              area,
                              schedulesByAreaId
                                  .getOrDefault(
                                      area.getId(),
                                      List.of()
                                  )
                          )
              )
              .toList();

      List<CollectionWasteType>
          supportedWasteTypes =
          areaResponses
              .stream()
              .flatMap(
                  response ->
                      response
                          .supportedWasteTypes()
                          .stream()
              )
              .distinct()
              .sorted()
              .toList();

      List<WasteTypeCoverageResponse>
          wasteTypeCoverage =
          supportedWasteTypes
              .stream()
              .map(
                  wasteType ->
                      createWasteTypeCoverage(
                          wasteType,
                          areaResponses
                      )
              )
              .toList();

      boolean allSchedulesRegistered =
          wasteTypeCoverage
              .stream()
              .allMatch(
                  coverage ->
                      coverage
                          .missingAreaCount()
                          == 0
              );

      return new AreaScheduleGroupResponse(
          representative.getId(),
          representative.getAreaName(),
          representative.getSido(),
          representative.getSigungu(),
          representative.getTargetAreaName(),
          representative.getSourceType(),
          representative.isActive(),
          areaResponses.size(),
          allSchedulesRegistered,
          supportedWasteTypes,
          wasteTypeCoverage,
          areaResponses
      );
    }

    private static WasteTypeCoverageResponse
    createWasteTypeCoverage(
        CollectionWasteType wasteType,
        List<AreaScheduleCoverageResponse>
            areaResponses
    ) {
      int supportedAreaCount =
          (int) areaResponses
              .stream()
              .filter(
                  response ->
                      response
                          .supportedWasteTypes()
                          .contains(
                              wasteType
                          )
              )
              .count();

      int registeredAreaCount =
          (int) areaResponses
              .stream()
              .filter(
                  response ->
                      response
                          .supportedWasteTypes()
                          .contains(
                              wasteType
                          )
              )
              .filter(
                  response ->
                      response
                          .schedules()
                          .stream()
                          .anyMatch(
                              schedule ->
                                  schedule
                                      .wasteType()
                                      == wasteType
                          )
              )
              .count();

      return new WasteTypeCoverageResponse(
          wasteType,
          supportedAreaCount,
          registeredAreaCount,
          supportedAreaCount
              - registeredAreaCount
      );
    }
  }

  public record CreateRequest(
      @NotNull
      Long collectionAreaId,

      @NotNull
      CollectionWasteType wasteType,

      @NotBlank
      @Size(max = 500)
      String emissionDays,

      LocalTime startTime,

      LocalTime endTime
  ) {
  }

  public record UpdateRequest(
      @NotBlank
      @Size(max = 500)
      String emissionDays,

      LocalTime startTime,

      LocalTime endTime
  ) {
  }
}
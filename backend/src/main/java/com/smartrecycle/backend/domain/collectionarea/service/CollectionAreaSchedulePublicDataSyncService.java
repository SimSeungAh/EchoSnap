package com.smartrecycle.backend.domain.schedule.service;

import com.smartrecycle.backend.domain.collectionarea.dto.external.HouseholdWastePublicDataResponse;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.schedule.entity.CollectionAreaSchedule;
import com.smartrecycle.backend.domain.schedule.repository.CollectionAreaScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CollectionAreaSchedulePublicDataSyncService {

  private final CollectionAreaScheduleRepository
      collectionAreaScheduleRepository;

  /**
   * CollectionArea 동기화가 끝난 한 페이지의 데이터를
   * 일반주택용 CollectionAreaSchedule로 동기화합니다.
   *
   * 한 페이지 단위로 트랜잭션을 사용하여
   * 공공 API 전체 호출 동안 하나의 긴 DB 트랜잭션이
   * 유지되지 않도록 합니다.
   */
  @Transactional
  public void syncPage(
      List<SyncTarget> targets
  ) {
    if (
        targets == null
            || targets.isEmpty()
    ) {
      return;
    }

    for (SyncTarget target : targets) {
      syncOne(
          target
      );
    }
  }

  /**
   * 공공데이터 한 건에 포함된
   * 생활쓰레기 / 음식물 / 재활용 일정을
   * CollectionAreaSchedule에 반영합니다.
   */
  private void syncOne(
      SyncTarget target
  ) {
    CollectionArea collectionArea =
        target.collectionArea();

    HouseholdWastePublicDataResponse.Item item =
        target.item();

    Set<CollectionWasteType> supportedWasteTypes =
        target.supportedWasteTypes();

    List<CollectionAreaSchedule> existingSchedules =
        collectionAreaScheduleRepository
            .findAllByCollectionAreaId(
                collectionArea.getId()
            );

    List<CollectionAreaSchedule> saveTargets =
        new ArrayList<>();

    List<CollectionAreaSchedule> deleteTargets =
        new ArrayList<>();

    /*
     * 이전 동기화 때는 존재했지만
     * 이번 공공데이터에서는 더 이상 지원하지 않는 종류라면
     * 오래된 일정을 제거합니다.
     */
    for (
        CollectionAreaSchedule existing
        : existingSchedules
    ) {
      if (
          !supportedWasteTypes.contains(
              existing.getWasteType()
          )
      ) {
        deleteTargets.add(
            existing
        );
      }
    }

    for (
        CollectionWasteType wasteType
        : supportedWasteTypes
    ) {
      ScheduleData scheduleData =
          extractScheduleData(
              item,
              wasteType
          );

      CollectionAreaSchedule existing =
          findExistingSchedule(
              existingSchedules,
              wasteType
          );

      if (existing == null) {
        saveTargets.add(
            CollectionAreaSchedule
                .createFromPublicData(
                    collectionArea,
                    wasteType,
                    scheduleData.emissionDays(),
                    scheduleData.startTime(),
                    scheduleData.endTime(),
                    scheduleData.emissionMethod(),
                    scheduleData.emissionPlace(),
                    scheduleData.emissionPlaceType(),
                    scheduleData.uncollectedDay()
                )
        );

        continue;
      }

      existing.updateFromPublicData(
          scheduleData.emissionDays(),
          scheduleData.startTime(),
          scheduleData.endTime(),
          scheduleData.emissionMethod(),
          scheduleData.emissionPlace(),
          scheduleData.emissionPlaceType(),
          scheduleData.uncollectedDay()
      );

      saveTargets.add(
          existing
      );
    }

    if (!deleteTargets.isEmpty()) {
      collectionAreaScheduleRepository
          .deleteAll(
              deleteTargets
          );
    }

    if (!saveTargets.isEmpty()) {
      collectionAreaScheduleRepository
          .saveAll(
              saveTargets
          );
    }
  }

  /**
   * CollectionWasteType에 따라
   * 실제 공공데이터 필드를 선택합니다.
   */
  private ScheduleData extractScheduleData(
      HouseholdWastePublicDataResponse.Item item,
      CollectionWasteType wasteType
  ) {
    return switch (wasteType) {

      case LIFE_WASTE ->
          new ScheduleData(
              normalizeOptionalText(
                  item.lifeWasteEmissionDays()
              ),
              parseTimeOrNull(
                  item.lifeWasteEmissionStartTime()
              ),
              parseTimeOrNull(
                  item.lifeWasteEmissionEndTime()
              ),
              normalizeOptionalText(
                  item.lifeWasteEmissionMethod()
              ),
              normalizeOptionalText(
                  item.emissionPlace()
              ),
              normalizeOptionalText(
                  item.emissionPlaceType()
              ),
              normalizeOptionalText(
                  item.uncollectedDay()
              )
          );

      case FOOD_WASTE ->
          new ScheduleData(
              normalizeOptionalText(
                  item.foodWasteEmissionDays()
              ),
              parseTimeOrNull(
                  item.foodWasteEmissionStartTime()
              ),
              parseTimeOrNull(
                  item.foodWasteEmissionEndTime()
              ),
              normalizeOptionalText(
                  item.foodWasteEmissionMethod()
              ),
              normalizeOptionalText(
                  item.emissionPlace()
              ),
              normalizeOptionalText(
                  item.emissionPlaceType()
              ),
              normalizeOptionalText(
                  item.uncollectedDay()
              )
          );

      case RECYCLABLE ->
          new ScheduleData(
              normalizeOptionalText(
                  item.recycleEmissionDays()
              ),
              parseTimeOrNull(
                  item.recycleEmissionStartTime()
              ),
              parseTimeOrNull(
                  item.recycleEmissionEndTime()
              ),
              normalizeOptionalText(
                  item.recycleEmissionMethod()
              ),
              normalizeOptionalText(
                  item.emissionPlace()
              ),
              normalizeOptionalText(
                  item.emissionPlaceType()
              ),
              normalizeOptionalText(
                  item.uncollectedDay()
              )
          );
    };
  }

  /**
   * 동일 CollectionArea에 이미 저장된
   * 같은 폐기물 종류의 일정을 찾습니다.
   */
  private CollectionAreaSchedule findExistingSchedule(
      List<CollectionAreaSchedule> existingSchedules,
      CollectionWasteType wasteType
  ) {
    return existingSchedules.stream()
        .filter(
            schedule ->
                schedule.getWasteType()
                    == wasteType
        )
        .findFirst()
        .orElse(null);
  }

  /**
   * 공공데이터 시간을 LocalTime으로 변환합니다.
   *
   * 지원 예:
   * 20:00
   * 20:00:00
   *
   * 24:00은 Java LocalTime에서 표현할 수 없으므로
   * 다음 날 00:00으로 정규화합니다.
   *
   * 해석할 수 없는 값 하나 때문에
   * 전체 공공데이터 동기화를 실패시키지 않고 null 처리합니다.
   */
  private LocalTime parseTimeOrNull(
      String value
  ) {
    String normalized =
        normalizeOptionalText(
            value
        );

    if (normalized == null) {
      return null;
    }

    String compact =
        normalized.replace(
            " ",
            ""
        );

    if (
        "24:00".equals(compact)
            || "24:00:00".equals(compact)
    ) {
      return LocalTime.MIDNIGHT;
    }

    try {
      return LocalTime.parse(
          compact
      );
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  /**
   * 공공데이터의 값 없음 표현을
   * 내부에서는 null로 정규화합니다.
   */
  private String normalizeOptionalText(
      String value
  ) {
    if (value == null) {
      return null;
    }

    String trimmed =
        value.trim();

    if (trimmed.isEmpty()) {
      return null;
    }

    String compact =
        trimmed.replace(
            " ",
            ""
        );

    if (
        "없음".equals(compact)
            || "해당없음".equals(compact)
            || "-".equals(compact)
    ) {
      return null;
    }

    return trimmed;
  }

  /**
   * CollectionArea 동기화 Service에서
   * 일정 동기화 Service로 전달하는 한 건의 데이터입니다.
   */
  public record SyncTarget(
      CollectionArea collectionArea,
      HouseholdWastePublicDataResponse.Item item,
      Set<CollectionWasteType> supportedWasteTypes
  ) {

    public SyncTarget {
      if (supportedWasteTypes == null) {
        supportedWasteTypes =
            EnumSet.noneOf(
                CollectionWasteType.class
            );
      } else {
        supportedWasteTypes =
            Set.copyOf(
                supportedWasteTypes
            );
      }
    }
  }

  /**
   * 공공데이터의 종류별 필드를
   * CollectionAreaSchedule 생성에 필요한 형태로 묶습니다.
   */
  private record ScheduleData(
      String emissionDays,
      LocalTime startTime,
      LocalTime endTime,
      String emissionMethod,
      String emissionPlace,
      String emissionPlaceType,
      String uncollectedDay
  ) {
  }
}
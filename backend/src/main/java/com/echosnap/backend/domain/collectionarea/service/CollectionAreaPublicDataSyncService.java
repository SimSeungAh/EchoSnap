package com.echosnap.backend.domain.collectionarea.service;

import com.echosnap.backend.domain.collectionarea.client.HouseholdWastePublicDataClient;
import com.echosnap.backend.domain.collectionarea.dto.external.HouseholdWastePublicDataResponse;
import com.echosnap.backend.domain.collectionarea.dto.response.CollectionAreaSyncResultResponse;
import com.echosnap.backend.domain.collectionarea.entity.CollectionArea;
import com.echosnap.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
import com.echosnap.backend.domain.collectionarea.repository.CollectionAreaRepository;
import com.echosnap.backend.domain.schedule.entity.CollectionAreaSchedule;
import com.echosnap.backend.domain.schedule.repository.CollectionAreaScheduleRepository;
import com.echosnap.backend.domain.schedule.service.CollectionAreaSchedulePublicDataSyncService;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CollectionAreaPublicDataSyncService {

  private static final String SUCCESS_RESULT_CODE = "0";

  private static final int PAGE_SIZE = 100;

  private final HouseholdWastePublicDataClient
      householdWastePublicDataClient;

  private final CollectionAreaRepository
      collectionAreaRepository;

  private final CollectionAreaScheduleRepository
      collectionAreaScheduleRepository;

  private final CollectionAreaSchedulePublicDataSyncService
      collectionAreaSchedulePublicDataSyncService;

  /**
   * 행정안전부 생활쓰레기배출정보 전체 데이터를
   * CollectionArea와 CollectionAreaSchedule에 동기화합니다.
   *
   * 중요한 중복 기준:
   *
   * 관리번호만 다르고
   *
   * 1. 시도
   * 2. 시군구
   * 3. 관리구역명
   * 4. 대상지역명
   * 5. 생활쓰레기 일정
   * 6. 음식물 일정
   * 7. 재활용 일정
   *
   * 까지 모두 같을 때만 동일한 CollectionArea로 봅니다.
   */
  public CollectionAreaSyncResultResponse syncAll() {

    HouseholdWastePublicDataResponse firstResponse =
        fetchAndValidatePage(
            1
        );

    int sourceTotalCount =
        resolveTotalCount(
            firstResponse
        );

    int pageCount =
        calculatePageCount(
            sourceTotalCount
        );

    SyncCounter counter =
        new SyncCounter();

    Set<String> processedManagementNumbers =
        new HashSet<>();

    /*
     * 한 번의 전체 동기화 과정에서 이미 처리한
     * 지역 + 전체 일정 패턴을 기억합니다.
     *
     * 서로 다른 페이지에 같은 원본 중복 데이터가 있어도
     * 다시 CollectionArea를 만들지 않습니다.
     */
    Map<ExactPublicDataKey, CollectionArea>
        processedExactAreas =
        new HashMap<>();

    processPage(
        firstResponse,
        processedManagementNumbers,
        processedExactAreas,
        counter
    );

    for (
        int pageNo = 2;
        pageNo <= pageCount;
        pageNo++
    ) {
      HouseholdWastePublicDataResponse response =
          fetchAndValidatePage(
              pageNo
          );

      processPage(
          response,
          processedManagementNumbers,
          processedExactAreas,
          counter
      );
    }

    return new CollectionAreaSyncResultResponse(
        sourceTotalCount,
        counter.fetchedCount,
        counter.createdCount,
        counter.updatedCount,
        counter.skippedCount,
        pageCount
    );
  }

  /**
   * 공공데이터 한 페이지를 조회합니다.
   */
  private HouseholdWastePublicDataResponse
  fetchAndValidatePage(
      int pageNo
  ) {
    HouseholdWastePublicDataResponse response;

    try {
      response =
          householdWastePublicDataClient
              .fetchPage(
                  pageNo,
                  PAGE_SIZE
              );

    } catch (RestClientException e) {
      throw new CustomException(
          ErrorCode.PUBLIC_DATA_API_ERROR
      );
    }

    validateResponse(
        response
    );

    return response;
  }

  /**
   * 공공데이터 응답 구조와 resultCode를 검증합니다.
   */
  private void validateResponse(
      HouseholdWastePublicDataResponse response
  ) {
    if (
        response == null
            || response.response() == null
            || response.response().header() == null
            || response.response().body() == null
    ) {
      throw new CustomException(
          ErrorCode.PUBLIC_DATA_INVALID_RESPONSE
      );
    }

    String resultCode =
        response.response()
            .header()
            .resultCode();

    if (
        !SUCCESS_RESULT_CODE.equals(
            resultCode
        )
    ) {
      throw new CustomException(
          ErrorCode.PUBLIC_DATA_API_ERROR
      );
    }
  }

  /**
   * 한 페이지의 CollectionArea와 일정을 처리합니다.
   */
  private void processPage(
      HouseholdWastePublicDataResponse response,
      Set<String> processedManagementNumbers,
      Map<ExactPublicDataKey, CollectionArea>
          processedExactAreas,
      SyncCounter counter
  ) {
    List<HouseholdWastePublicDataResponse.Item> items =
        extractItems(
            response
        );

    if (items.isEmpty()) {
      return;
    }

    Set<CollectionArea> saveTargets =
        new LinkedHashSet<>();

    List<PendingScheduleSync> pendingScheduleSyncs =
        new ArrayList<>();

    for (
        HouseholdWastePublicDataResponse.Item item
        : items
    ) {
      counter.fetchedCount++;

      String managementNumber =
          trimToNull(
              item.managementNumber()
          );

      String sido =
          normalizeRequiredText(
              item.sido()
          );

      String sigungu =
          normalizeRequiredText(
              item.sigungu()
          );

      if (
          managementNumber == null
              || sido == null
              || sigungu == null
      ) {
        counter.skippedCount++;
        continue;
      }

      /*
       * 완전히 같은 MNG_NO가 API 안에서 반복되는 경우는
       * 기존 방식대로 한 번만 처리합니다.
       */
      if (
          !processedManagementNumbers.add(
              managementNumber
          )
      ) {
        counter.skippedCount++;
        continue;
      }

      Set<CollectionWasteType> supportedWasteTypes =
          detectSupportedWasteTypes(
              item
          );

      if (supportedWasteTypes.isEmpty()) {
        counter.skippedCount++;
        continue;
      }

      String areaName =
          normalizeAreaName(
              item.managementZoneName(),
              sigungu
          );

      String targetAreaName =
          normalizeTargetAreaName(
              item.managementZoneTargetRegionName(),
              sigungu
          );

      LocalDate sourceReferenceDate =
          parseDateOrNull(
              item.dataReferenceDate()
          );

      SchedulePattern incomingSchedulePattern =
          createIncomingSchedulePattern(
              item,
              supportedWasteTypes
          );

      ExactPublicDataKey exactKey =
          new ExactPublicDataKey(
              createLogicalAreaKey(
                  sido,
                  sigungu,
                  areaName,
                  targetAreaName
              ),
              incomingSchedulePattern
          );

      /*
       * 같은 전체 동기화 안에서 이미
       *
       * 지역 + 일정
       *
       * 이 완전히 같은 데이터를 처리했다면
       * 관리번호가 다르더라도 다시 만들지 않습니다.
       */
      CollectionArea alreadyProcessed =
          processedExactAreas.get(
              exactKey
          );

      if (alreadyProcessed != null) {
        counter.skippedCount++;
        continue;
      }

      /*
       * 1순위:
       *
       * 같은 MNG_NO가 기존 DB에 있으면
       * 같은 공공데이터 레코드가 수정된 것으로 판단합니다.
       */
      Optional<CollectionArea> byManagementNumber =
          collectionAreaRepository
              .findBySourceTypeAndExternalManagementNumber(
                  CollectionAreaSourceType
                      .MOIS_HOUSEHOLD_WASTE,
                  managementNumber
              );

      if (byManagementNumber.isPresent()) {
        CollectionArea collectionArea =
            updateExistingArea(
                byManagementNumber.get(),
                sido,
                sigungu,
                areaName,
                targetAreaName,
                supportedWasteTypes,
                sourceReferenceDate,
                counter
            );

        saveTargets.add(
            collectionArea
        );

        pendingScheduleSyncs.add(
            new PendingScheduleSync(
                collectionArea,
                item,
                supportedWasteTypes
            )
        );

        processedExactAreas.put(
            exactKey,
            collectionArea
        );

        continue;
      }

      /*
       * 2순위:
       *
       * MNG_NO가 다르더라도
       *
       * 지역 + 전체 일정
       *
       * 이 이미 DB에 완전히 동일하게 존재하면
       * 중복 공공데이터로 판단합니다.
       */
      Optional<CollectionArea> exactExistingArea =
          findExactExistingArea(
              sido,
              sigungu,
              areaName,
              targetAreaName,
              incomingSchedulePattern
          );

      if (exactExistingArea.isPresent()) {
        CollectionArea collectionArea =
            exactExistingArea.get();

        /*
         * 과거에 비활성화된 데이터가 현재 공공데이터에
         * 다시 등장했다면 다시 활성화합니다.
         */
        if (!collectionArea.isActive()) {
          collectionArea.activate();

          saveTargets.add(
              collectionArea
          );

          counter.updatedCount++;
        } else {
          counter.skippedCount++;
        }

        processedExactAreas.put(
            exactKey,
            collectionArea
        );

        /*
         * 이미 DB의 일정까지 완전히 동일하다는 것을
         * 확인했으므로 Schedule Sync는 다시 하지 않습니다.
         */
        continue;
      }

      /*
       * 3순위:
       *
       * 관리번호도 없고
       * 같은 지역 + 같은 일정도 없다면
       * 실제로 다른 수거규칙이므로 새 CollectionArea를 만듭니다.
       */
      CollectionArea newArea =
          createNewArea(
              managementNumber,
              sido,
              sigungu,
              areaName,
              targetAreaName,
              supportedWasteTypes,
              sourceReferenceDate,
              counter
          );

      saveTargets.add(
          newArea
      );

      pendingScheduleSyncs.add(
          new PendingScheduleSync(
              newArea,
              item,
              supportedWasteTypes
          )
      );

      processedExactAreas.put(
          exactKey,
          newArea
      );
    }

    if (saveTargets.isEmpty()) {
      return;
    }

    /*
     * 신규 CollectionArea의 ID가 먼저 필요합니다.
     */
    collectionAreaRepository.saveAll(
        saveTargets
    );

    if (pendingScheduleSyncs.isEmpty()) {
      return;
    }

    List<
        CollectionAreaSchedulePublicDataSyncService.SyncTarget
        > scheduleSyncTargets =
        pendingScheduleSyncs.stream()
            .map(
                pending ->
                    new CollectionAreaSchedulePublicDataSyncService
                        .SyncTarget(
                        pending.collectionArea(),
                        pending.item(),
                        pending.supportedWasteTypes()
                    )
            )
            .toList();

    collectionAreaSchedulePublicDataSyncService
        .syncPage(
            scheduleSyncTargets
        );
  }

  /**
   * 같은 지역에 이미 존재하는 CollectionArea들 중에서
   * 실제 전체 일정까지 완전히 같은 하나를 찾습니다.
   */
  private Optional<CollectionArea> findExactExistingArea(
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName,
      SchedulePattern incomingPattern
  ) {
    List<CollectionArea> candidates =
        collectionAreaRepository
            .findAllBySourceTypeAndSidoAndSigunguAndAreaNameAndTargetAreaNameOrderByIdAsc(
                CollectionAreaSourceType
                    .MOIS_HOUSEHOLD_WASTE,
                sido,
                sigungu,
                areaName,
                targetAreaName
            );

    if (candidates.isEmpty()) {
      return Optional.empty();
    }

    List<Long> candidateIds =
        candidates.stream()
            .map(
                CollectionArea::getId
            )
            .toList();

    List<CollectionAreaSchedule> schedules =
        collectionAreaScheduleRepository
            .findAllWithCollectionAreaByCollectionAreaIdIn(
                candidateIds
            );

    Map<Long, List<CollectionAreaSchedule>>
        schedulesByAreaId =
        new HashMap<>();

    for (
        CollectionAreaSchedule schedule
        : schedules
    ) {
      Long collectionAreaId =
          schedule.getCollectionArea()
              .getId();

      schedulesByAreaId
          .computeIfAbsent(
              collectionAreaId,
              ignored -> new ArrayList<>()
          )
          .add(
              schedule
          );
    }

    /*
     * Repository에서 ID ASC로 가져왔으므로
     * 과거 중복이 여러 개 존재해도
     * 가장 오래된 ID를 대표값으로 선택합니다.
     */
    for (
        CollectionArea candidate
        : candidates
    ) {
      List<CollectionAreaSchedule>
          candidateSchedules =
          schedulesByAreaId.getOrDefault(
              candidate.getId(),
              List.of()
          );

      SchedulePattern existingPattern =
          createExistingSchedulePattern(
              candidateSchedules
          );

      /*
       * null은 관리자 승인 일정이 섞였거나
       * DB 일정 상태가 중복 판별에 안전하지 않다는 뜻입니다.
       */
      if (existingPattern == null) {
        continue;
      }

      if (
          existingPattern.equals(
              incomingPattern
          )
      ) {
        return Optional.of(
            candidate
        );
      }
    }

    return Optional.empty();
  }

  /**
   * API 한 행을 일정 비교용 패턴으로 변환합니다.
   */
  private SchedulePattern createIncomingSchedulePattern(
      HouseholdWastePublicDataResponse.Item item,
      Set<CollectionWasteType> supportedWasteTypes
  ) {
    ScheduleFingerprint lifeWaste =
        supportedWasteTypes.contains(
            CollectionWasteType.LIFE_WASTE
        )
            ? createIncomingScheduleFingerprint(
            item,
            CollectionWasteType.LIFE_WASTE
        )
            : null;

    ScheduleFingerprint foodWaste =
        supportedWasteTypes.contains(
            CollectionWasteType.FOOD_WASTE
        )
            ? createIncomingScheduleFingerprint(
            item,
            CollectionWasteType.FOOD_WASTE
        )
            : null;

    ScheduleFingerprint recyclable =
        supportedWasteTypes.contains(
            CollectionWasteType.RECYCLABLE
        )
            ? createIncomingScheduleFingerprint(
            item,
            CollectionWasteType.RECYCLABLE
        )
            : null;

    return new SchedulePattern(
        lifeWaste,
        foodWaste,
        recyclable
    );
  }

  /**
   * DB CollectionAreaSchedule 목록을
   * API와 비교 가능한 동일한 패턴으로 변환합니다.
   *
   * ADMIN_APPROVED_REPORT 일정이 하나라도 포함돼 있으면
   * 자동 중복 병합 대상에서 제외합니다.
   *
   * 관리자가 승인한 주민 보정 일정을 공공데이터 중복이라고
   * 잘못 판단해서 합치는 일을 막기 위함입니다.
   */
  private SchedulePattern createExistingSchedulePattern(
      List<CollectionAreaSchedule> schedules
  ) {
    if (
        schedules == null
            || schedules.isEmpty()
    ) {
      return null;
    }

    Map<
        CollectionWasteType,
        CollectionAreaSchedule
        > byWasteType =
        new EnumMap<>(
            CollectionWasteType.class
        );

    for (
        CollectionAreaSchedule schedule
        : schedules
    ) {
      if (
          schedule.isAdminApprovedOverride()
      ) {
        return null;
      }

      CollectionAreaSchedule previous =
          byWasteType.put(
              schedule.getWasteType(),
              schedule
          );

      /*
       * DB 유니크 제약상 발생하면 안 되지만
       * 혹시 데이터가 깨진 경우에는 자동 병합하지 않습니다.
       */
      if (previous != null) {
        return null;
      }
    }

    return new SchedulePattern(
        createExistingScheduleFingerprint(
            byWasteType.get(
                CollectionWasteType.LIFE_WASTE
            )
        ),
        createExistingScheduleFingerprint(
            byWasteType.get(
                CollectionWasteType.FOOD_WASTE
            )
        ),
        createExistingScheduleFingerprint(
            byWasteType.get(
                CollectionWasteType.RECYCLABLE
            )
        )
    );
  }

  /**
   * API에서 폐기물 종류 하나의 실제 일정값을 만듭니다.
   */
  private ScheduleFingerprint
  createIncomingScheduleFingerprint(
      HouseholdWastePublicDataResponse.Item item,
      CollectionWasteType wasteType
  ) {
    return switch (wasteType) {

      case LIFE_WASTE ->
          new ScheduleFingerprint(
              normalizeOptionalScheduleText(
                  item.lifeWasteEmissionDays()
              ),
              parseTimeOrNull(
                  item.lifeWasteEmissionStartTime()
              ),
              parseTimeOrNull(
                  item.lifeWasteEmissionEndTime()
              ),
              normalizeOptionalScheduleText(
                  item.lifeWasteEmissionMethod()
              ),
              normalizeOptionalScheduleText(
                  item.emissionPlace()
              ),
              normalizeOptionalScheduleText(
                  item.emissionPlaceType()
              ),
              normalizeOptionalScheduleText(
                  item.uncollectedDay()
              )
          );

      case FOOD_WASTE ->
          new ScheduleFingerprint(
              normalizeOptionalScheduleText(
                  item.foodWasteEmissionDays()
              ),
              parseTimeOrNull(
                  item.foodWasteEmissionStartTime()
              ),
              parseTimeOrNull(
                  item.foodWasteEmissionEndTime()
              ),
              normalizeOptionalScheduleText(
                  item.foodWasteEmissionMethod()
              ),
              normalizeOptionalScheduleText(
                  item.emissionPlace()
              ),
              normalizeOptionalScheduleText(
                  item.emissionPlaceType()
              ),
              normalizeOptionalScheduleText(
                  item.uncollectedDay()
              )
          );

      case RECYCLABLE ->
          new ScheduleFingerprint(
              normalizeOptionalScheduleText(
                  item.recycleEmissionDays()
              ),
              parseTimeOrNull(
                  item.recycleEmissionStartTime()
              ),
              parseTimeOrNull(
                  item.recycleEmissionEndTime()
              ),
              normalizeOptionalScheduleText(
                  item.recycleEmissionMethod()
              ),
              normalizeOptionalScheduleText(
                  item.emissionPlace()
              ),
              normalizeOptionalScheduleText(
                  item.emissionPlaceType()
              ),
              normalizeOptionalScheduleText(
                  item.uncollectedDay()
              )
          );
    };
  }

  /**
   * DB 일정 하나를 비교용 Fingerprint로 변환합니다.
   */
  private ScheduleFingerprint
  createExistingScheduleFingerprint(
      CollectionAreaSchedule schedule
  ) {
    if (schedule == null) {
      return null;
    }

    return new ScheduleFingerprint(
        normalizeOptionalScheduleText(
            schedule.getEmissionDays()
        ),
        schedule.getStartTime(),
        schedule.getEndTime(),
        normalizeOptionalScheduleText(
            schedule.getEmissionMethod()
        ),
        normalizeOptionalScheduleText(
            schedule.getEmissionPlace()
        ),
        normalizeOptionalScheduleText(
            schedule.getEmissionPlaceType()
        ),
        normalizeOptionalScheduleText(
            schedule.getUncollectedDay()
        )
    );
  }

  /**
   * 실제 공공데이터 한 건이 지원하는 폐기물 종류를 판단합니다.
   */
  private Set<CollectionWasteType>
  detectSupportedWasteTypes(
      HouseholdWastePublicDataResponse.Item item
  ) {
    Set<CollectionWasteType> result =
        EnumSet.noneOf(
            CollectionWasteType.class
        );

    if (
        isMeaningfulEmissionMethod(
            item.lifeWasteEmissionMethod()
        )
    ) {
      result.add(
          CollectionWasteType.LIFE_WASTE
      );
    }

    if (
        isMeaningfulEmissionMethod(
            item.foodWasteEmissionMethod()
        )
    ) {
      result.add(
          CollectionWasteType.FOOD_WASTE
      );
    }

    if (
        isMeaningfulEmissionMethod(
            item.recycleEmissionMethod()
        )
    ) {
      result.add(
          CollectionWasteType.RECYCLABLE
      );
    }

    return result;
  }

  private boolean isMeaningfulEmissionMethod(
      String value
  ) {
    return normalizeOptionalPublicDataText(
        value
    ) != null;
  }

  private List<HouseholdWastePublicDataResponse.Item>
  extractItems(
      HouseholdWastePublicDataResponse response
  ) {
    HouseholdWastePublicDataResponse.Items items =
        response.response()
            .body()
            .items();

    if (
        items == null
            || items.item() == null
    ) {
      return List.of();
    }

    return items.item();
  }

  /**
   * 기존 MNG_NO 데이터를 최신 공공데이터로 갱신합니다.
   */
  private CollectionArea updateExistingArea(
      CollectionArea collectionArea,
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName,
      Set<CollectionWasteType> supportedWasteTypes,
      LocalDate sourceReferenceDate,
      SyncCounter counter
  ) {
    collectionArea.updateFromPublicData(
        sido,
        sigungu,
        areaName,
        targetAreaName,
        supportedWasteTypes,
        sourceReferenceDate
    );

    counter.updatedCount++;

    return collectionArea;
  }

  /**
   * 새 공공데이터 수거구역을 생성합니다.
   */
  private CollectionArea createNewArea(
      String managementNumber,
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName,
      Set<CollectionWasteType> supportedWasteTypes,
      LocalDate sourceReferenceDate,
      SyncCounter counter
  ) {
    counter.createdCount++;

    return CollectionArea.createFromPublicData(
        managementNumber,
        sido,
        sigungu,
        areaName,
        targetAreaName,
        supportedWasteTypes,
        sourceReferenceDate
    );
  }

  /**
   * 일정 동기화 Service와 동일하게
   * 공공데이터 시간 문자열을 LocalTime으로 변환합니다.
   *
   * 24:00은 다음 날 00:00으로 취급합니다.
   */
  private LocalTime parseTimeOrNull(
      String value
  ) {
    String normalized =
        normalizeOptionalScheduleText(
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
   * SchedulePublicDataSyncService와 동일한
   * 일정 문자열 정규화 규칙입니다.
   */
  private String normalizeOptionalScheduleText(
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

  private LogicalAreaKey createLogicalAreaKey(
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName
  ) {
    return new LogicalAreaKey(
        normalizeKeyPart(
            sido
        ),
        normalizeKeyPart(
            sigungu
        ),
        normalizeKeyPart(
            areaName
        ),
        normalizeKeyPart(
            targetAreaName
        )
    );
  }

  private String normalizeKeyPart(
      String value
  ) {
    String trimmed =
        trimToNull(
            value
        );

    if (trimmed == null) {
      return "";
    }

    return trimmed
        .replaceAll(
            "\\s+",
            ""
        )
        .toLowerCase(
            Locale.ROOT
        );
  }

  private int resolveTotalCount(
      HouseholdWastePublicDataResponse response
  ) {
    Integer totalCount =
        response.response()
            .body()
            .totalCount();

    if (
        totalCount == null
            || totalCount < 0
    ) {
      throw new CustomException(
          ErrorCode.PUBLIC_DATA_INVALID_RESPONSE
      );
    }

    return totalCount;
  }

  private int calculatePageCount(
      int totalCount
  ) {
    if (totalCount == 0) {
      return 1;
    }

    return (
        totalCount
            + PAGE_SIZE
            - 1
    ) / PAGE_SIZE;
  }

  private String normalizeAreaName(
      String value,
      String sigungu
  ) {
    String normalized =
        normalizeOptionalPublicDataText(
            value
        );

    if (normalized != null) {
      return normalized;
    }

    return sigungu + " 전체";
  }

  private String normalizeTargetAreaName(
      String value,
      String sigungu
  ) {
    String normalized =
        normalizeOptionalPublicDataText(
            value
        );

    if (normalized != null) {
      return normalized;
    }

    return sigungu + " 전체";
  }

  private String normalizeOptionalPublicDataText(
      String value
  ) {
    String trimmed =
        trimToNull(
            value
        );

    if (trimmed == null) {
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

  private String normalizeRequiredText(
      String value
  ) {
    return trimToNull(
        value
    );
  }

  private LocalDate parseDateOrNull(
      String value
  ) {
    String trimmed =
        trimToNull(
            value
        );

    if (trimmed == null) {
      return null;
    }

    try {
      return LocalDate.parse(
          trimmed
      );
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private String trimToNull(
      String value
  ) {
    if (value == null) {
      return null;
    }

    String trimmed =
        value.trim();

    return trimmed.isEmpty()
        ? null
        : trimmed;
  }

  /**
   * 공공데이터의 실제 지역 식별값입니다.
   */
  private record LogicalAreaKey(
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName
  ) {
  }

  /**
   * 폐기물 종류 하나의 실제 배출규칙입니다.
   */
  private record ScheduleFingerprint(
      String emissionDays,
      LocalTime startTime,
      LocalTime endTime,
      String emissionMethod,
      String emissionPlace,
      String emissionPlaceType,
      String uncollectedDay
  ) {
  }

  /**
   * CollectionArea 전체 일정 패턴입니다.
   *
   * 같은 지역이라도 이 값이 다르면
   * 서로 다른 CollectionArea로 유지합니다.
   */
  private record SchedulePattern(
      ScheduleFingerprint lifeWaste,
      ScheduleFingerprint foodWaste,
      ScheduleFingerprint recyclable
  ) {
  }

  /**
   * 최종 중복 판별키입니다.
   *
   * 지역 + 전체 일정이 모두 같아야 equals가 true가 됩니다.
   */
  private record ExactPublicDataKey(
      LogicalAreaKey area,
      SchedulePattern schedulePattern
  ) {
  }

  private record PendingScheduleSync(
      CollectionArea collectionArea,
      HouseholdWastePublicDataResponse.Item item,
      Set<CollectionWasteType> supportedWasteTypes
  ) {

    private PendingScheduleSync {
      supportedWasteTypes =
          Set.copyOf(
              supportedWasteTypes
          );
    }
  }

  private static class SyncCounter {

    private int fetchedCount;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
  }
}
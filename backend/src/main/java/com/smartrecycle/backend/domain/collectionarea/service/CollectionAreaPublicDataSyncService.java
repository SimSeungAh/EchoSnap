package com.smartrecycle.backend.domain.collectionarea.service;

import com.smartrecycle.backend.domain.collectionarea.client.HouseholdWastePublicDataClient;
import com.smartrecycle.backend.domain.collectionarea.dto.external.HouseholdWastePublicDataResponse;
import com.smartrecycle.backend.domain.collectionarea.dto.response.CollectionAreaSyncResultResponse;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.collectionarea.repository.CollectionAreaRepository;
import com.smartrecycle.backend.domain.schedule.service.CollectionAreaSchedulePublicDataSyncService;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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

  private final CollectionAreaSchedulePublicDataSyncService
      collectionAreaSchedulePublicDataSyncService;

  /**
   * 행정안전부 생활쓰레기배출정보 전체 데이터를
   * CollectionArea와 CollectionAreaSchedule에
   * 동기화합니다.
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

    processPage(
        firstResponse,
        processedManagementNumbers,
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
   * HTTP 성공 여부와 별개로
   * 공공데이터 내부 resultCode와 응답 구조를 검증합니다.
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
   * 한 페이지를 CollectionArea로 변환한 뒤 저장하고,
   * 저장된 CollectionArea를 기준으로 일정까지 동기화합니다.
   */
  private void processPage(
      HouseholdWastePublicDataResponse response,
      Set<String> processedManagementNumbers,
      SyncCounter counter
  ) {
    List<HouseholdWastePublicDataResponse.Item> items =
        extractItems(
            response
        );

    if (items.isEmpty()) {
      return;
    }

    List<CollectionArea> saveTargets =
        new ArrayList<>();

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
       * 동일한 전체 동기화 안에서
       * 같은 MNG_NO가 반복되면 한 번만 처리합니다.
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

      /*
       * 생활 / 음식물 / 재활용 중
       * 실제 사용할 수 있는 정보가 하나도 없다면
       * SmartRecycle에서는 사용하지 않습니다.
       */
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

      CollectionArea collectionArea =
          collectionAreaRepository
              .findBySourceTypeAndExternalManagementNumber(
                  CollectionAreaSourceType
                      .MOIS_HOUSEHOLD_WASTE,
                  managementNumber
              )
              .map(
                  existing ->
                      updateExistingArea(
                          existing,
                          sido,
                          sigungu,
                          areaName,
                          targetAreaName,
                          supportedWasteTypes,
                          sourceReferenceDate,
                          counter
                      )
              )
              .orElseGet(
                  () ->
                      createNewArea(
                          managementNumber,
                          sido,
                          sigungu,
                          areaName,
                          targetAreaName,
                          supportedWasteTypes,
                          sourceReferenceDate,
                          counter
                      )
              );

      saveTargets.add(
          collectionArea
      );

      /*
       * CollectionArea가 DB에 저장되어 ID를 갖게 된 후
       * 일정 저장을 수행해야 하므로
       * 우선 pending 목록에 보관합니다.
       */
      pendingScheduleSyncs.add(
          new PendingScheduleSync(
              collectionArea,
              item,
              supportedWasteTypes
          )
      );
    }

    if (saveTargets.isEmpty()) {
      return;
    }

    /*
     * 신규 CollectionArea도 이 시점에서
     * DB ID를 부여받습니다.
     */
    collectionAreaRepository.saveAll(
        saveTargets
    );

    /*
     * CollectionArea 저장 이후
     * 같은 공공데이터로 종류별 배출 일정을 동기화합니다.
     *
     * 페이지 단위 Transaction은
     * CollectionAreaSchedulePublicDataSyncService가 담당합니다.
     */
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
   * 실제 공공데이터 한 건이
   * 어떤 폐기물 종류의 정보를 제공하는지 판단합니다.
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
   * 기존 CollectionArea를 최신 공공데이터로 갱신합니다.
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
   * 처음 조회된 MNG_NO라면
   * 새로운 CollectionArea를 생성합니다.
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
   * CollectionArea 저장 전까지
   * Item과 지원 폐기물 종류를 함께 들고 있는
   * Service 내부용 데이터입니다.
   */
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
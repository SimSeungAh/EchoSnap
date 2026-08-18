package com.smartrecycle.backend.domain.collectionarea.service;

import com.smartrecycle.backend.domain.collectionarea.client.HouseholdWastePublicDataClient;
import com.smartrecycle.backend.domain.collectionarea.dto.external.HouseholdWastePublicDataResponse;
import com.smartrecycle.backend.domain.collectionarea.dto.response.CollectionAreaSyncResultResponse;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.collectionarea.repository.CollectionAreaRepository;
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

  /**
   * 행정안전부 생활쓰레기배출정보 전체 데이터를
   * CollectionArea와 동기화합니다.
   */
  public CollectionAreaSyncResultResponse syncAll() {

    HouseholdWastePublicDataResponse firstResponse =
        fetchAndValidatePage(1);

    int sourceTotalCount =
        resolveTotalCount(firstResponse);

    int pageCount =
        calculatePageCount(sourceTotalCount);

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
   * 공공데이터 한 페이지를 조회하고
   * 응답 구조와 결과 코드를 검증합니다.
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
   * HTTP 요청은 성공했더라도
   * 공공데이터의 내부 resultCode가
   * 실패일 수 있기 때문에 별도로 검증합니다.
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

    if (!SUCCESS_RESULT_CODE.equals(resultCode)) {
      throw new CustomException(
          ErrorCode.PUBLIC_DATA_API_ERROR
      );
    }
  }

  /**
   * 한 페이지의 데이터를 CollectionArea로 변환합니다.
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

      if (
          !processedManagementNumbers.add(
              managementNumber
          )
      ) {
        counter.skippedCount++;
        continue;
      }

      /*
       * 실제 공공데이터의 배출방법 필드를 기준으로
       * 이 레코드가 어떤 폐기물 종류에 적용되는지 확인합니다.
       */
      Set<CollectionWasteType> supportedWasteTypes =
          detectSupportedWasteTypes(
              item
          );

      /*
       * 생활/음식물/재활용 중
       * 실제 적용 가능한 정보가 하나도 없는 레코드는
       * SmartRecycle 수거구역으로 사용할 이유가 없으므로
       * 건너뜁니다.
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
    }

    if (!saveTargets.isEmpty()) {
      collectionAreaRepository.saveAll(
          saveTargets
      );
    }
  }

  /**
   * 공공데이터 한 건이 어떤 폐기물 종류의
   * 실제 수거정보를 포함하는지 판단합니다.
   *
   * 실제 데이터에서 전용 수거구역이 아닌 항목에는
   * 배출방법이 "해당없음"으로 내려오는 것을 확인했기 때문에
   * 배출방법을 주요 판단 기준으로 사용합니다.
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

  /**
   * "없음", "해당없음", "-" 같은 값은
   * 실제 배출방법이 존재하지 않는 것으로 판단합니다.
   */
  private boolean isMeaningfulEmissionMethod(
      String value
  ) {
    String normalized =
        normalizeOptionalPublicDataText(
            value
        );

    return normalized != null;
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

  /**
   * 공공데이터에서 값이 없음을 표현하는
   * 대표 문자열을 null로 정규화합니다.
   */
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

  private static class SyncCounter {

    private int fetchedCount;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
  }
}
package com.smartrecycle.backend.domain.image.dto.response;

import com.smartrecycle.backend.domain.image.entity.ImageAnalysisStatus;
import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.entity.ImageResultSource;
import com.smartrecycle.backend.domain.image.entity.ImageReviewStatus;
import com.smartrecycle.backend.domain.schedule.dto.response.GeneralHousingScheduleResponse;
import com.smartrecycle.backend.domain.schedule.dto.response.WasteItemScheduleResponse;
import com.smartrecycle.backend.domain.user.entity.ResidenceType;
import com.smartrecycle.backend.domain.waste.dto.response.RecycleGuideResponse;
import com.smartrecycle.backend.domain.waste.entity.WasteCategory;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;

/**
 * AI 분석 결과 화면에 필요한
 * 최종 통합 응답입니다.
 *
 * 하나의 응답에서:
 *
 * - AI 분석 결과
 * - 결과 출처
 * - 신뢰도
 * - 분리배출 가이드
 * - 사용자 거주 형태
 * - 거주지 맞춤 배출 일정
 *
 * 을 함께 반환합니다.
 */
public record ImageAnalysisResultResponse(

    Long imageLogId,

    String imageUrl,

    ImageAnalysisStatus analysisStatus,

    ImageReviewStatus reviewStatus,

    ImageResultSource resultSource,

    WasteItemInfo wasteItem,

    Double confidence,

    String modelVersion,

    boolean needsServerReanalysis,

    RecycleGuideResponse guide,

    ResidenceType residenceType,

    /**
     * MANAGED_COMPLEX 사용자에게 적용되는
     * 해당 WasteItem의 공동주택 일정입니다.
     *
     * GENERAL_HOUSING이면 null입니다.
     */
    WasteItemScheduleResponse managedComplexSchedule,

    /**
     * GENERAL_HOUSING 사용자에게 적용되는
     * 주소/수거구역 기준 지역 일정입니다.
     *
     * MANAGED_COMPLEX이면 null입니다.
     */
    GeneralHousingScheduleResponse generalHousingSchedule

) {

  public static ImageAnalysisResultResponse of(
      ImageLog imageLog,
      RecycleGuideResponse guide,
      ResidenceType residenceType,
      WasteItemScheduleResponse managedComplexSchedule,
      GeneralHousingScheduleResponse generalHousingSchedule
  ) {
    WasteItem effectiveWasteItem =
        imageLog.getEffectiveWasteItem();

    ImageResultSource resultSource =
        resolveResultSource(
            imageLog
        );

    Double confidence =
        resolveConfidence(
            imageLog,
            resultSource
        );

    String modelVersion =
        resolveModelVersion(
            imageLog,
            resultSource
        );

    boolean needsServerReanalysis =
        imageLog.getAnalysisStatus()
            == ImageAnalysisStatus
            .SERVER_REANALYSIS_PENDING;

    return new ImageAnalysisResultResponse(

        imageLog.getId(),

        imageLog.getImageUrl(),

        imageLog.getAnalysisStatus(),

        imageLog.getReviewStatus(),

        resultSource,

        WasteItemInfo.from(
            effectiveWasteItem
        ),

        confidence,

        modelVersion,

        needsServerReanalysis,

        guide,

        residenceType,

        managedComplexSchedule,

        generalHousingSchedule
    );
  }

  /**
   * 현재 최종 결과가
   * 어디에서 결정되었는지 계산합니다.
   *
   * 우선순위:
   *
   * USER_CORRECTION
   * >
   * SERVER_AI
   * >
   * MOBILE_AI
   */
  private static ImageResultSource
  resolveResultSource(
      ImageLog imageLog
  ) {
    if (
        imageLog.getUserCorrectedWasteItem()
            != null
    ) {
      return ImageResultSource
          .USER_CORRECTION;
    }

    if (
        imageLog.getServerWasteItem()
            != null
    ) {
      return ImageResultSource
          .SERVER_AI;
    }

    if (
        imageLog.getMobileWasteItem()
            != null
    ) {
      return ImageResultSource
          .MOBILE_AI;
    }

    return ImageResultSource.NONE;
  }

  /**
   * 현재 최종 결과의 AI 신뢰도를 계산합니다.
   *
   * 사용자가 직접 수정한 결과는
   * AI 예측이 아니므로 confidence를
   * 반환하지 않습니다.
   */
  private static Double resolveConfidence(
      ImageLog imageLog,
      ImageResultSource resultSource
  ) {
    return switch (resultSource) {

      case MOBILE_AI ->
          imageLog.getMobileConfidence();

      case SERVER_AI ->
          imageLog.getServerConfidence();

      case USER_CORRECTION, NONE ->
          null;
    };
  }

  /**
   * 현재 최종 결과에 사용된
   * AI 모델 버전을 반환합니다.
   *
   * 사용자 직접 수정은 AI 결과가 아니므로
   * 모델 버전이 없습니다.
   */
  private static String resolveModelVersion(
      ImageLog imageLog,
      ImageResultSource resultSource
  ) {
    return switch (resultSource) {

      case MOBILE_AI ->
          imageLog.getMobileModelVersion();

      case SERVER_AI ->
          imageLog.getServerModelVersion();

      case USER_CORRECTION, NONE ->
          null;
    };
  }

  /**
   * AI 결과 화면에 필요한
   * WasteItem 요약 정보입니다.
   */
  public record WasteItemInfo(

      Long id,

      String name,

      String imageUrl,

      CategoryInfo category

  ) {

    private static WasteItemInfo from(
        WasteItem wasteItem
    ) {
      if (wasteItem == null) {
        return null;
      }

      return new WasteItemInfo(

          wasteItem.getId(),

          wasteItem.getName(),

          wasteItem.getImageUrl(),

          CategoryInfo.from(
              wasteItem.getCategory()
          )
      );
    }
  }

  /**
   * WasteItem의 카테고리 정보입니다.
   */
  public record CategoryInfo(

      Long id,

      String code,

      String name

  ) {

    private static CategoryInfo from(
        WasteCategory category
    ) {
      if (category == null) {
        return null;
      }

      return new CategoryInfo(
          category.getId(),
          category.getCode(),
          category.getName()
      );
    }
  }
}
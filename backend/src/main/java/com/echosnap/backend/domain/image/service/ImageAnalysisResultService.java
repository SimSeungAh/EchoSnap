package com.echosnap.backend.domain.image.service;

import com.echosnap.backend.domain.image.dto.response.ImageAnalysisResultResponse;
import com.echosnap.backend.domain.image.entity.ImageLog;
import com.echosnap.backend.domain.image.repository.ImageLogRepository;
import com.echosnap.backend.domain.schedule.dto.response.GeneralHousingScheduleResponse;
import com.echosnap.backend.domain.schedule.dto.response.WasteItemScheduleResponse;
import com.echosnap.backend.domain.schedule.service.CollectionAreaScheduleQueryService;
import com.echosnap.backend.domain.schedule.service.RecycleScheduleQueryService;
import com.echosnap.backend.domain.user.entity.ResidenceType;
import com.echosnap.backend.domain.waste.dto.response.RecycleGuideResponse;
import com.echosnap.backend.domain.waste.entity.RecycleGuide;
import com.echosnap.backend.domain.waste.entity.RecycleGuideCheckItem;
import com.echosnap.backend.domain.waste.entity.WasteItem;
import com.echosnap.backend.domain.waste.repository.RecycleGuideCheckItemRepository;
import com.echosnap.backend.domain.waste.repository.RecycleGuideRepository;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 분석 결과를 실제 EchoSnap 서비스 정보와
 * 연결하는 Service입니다.
 *
 * AI가 품목을 분류하는 데서 끝내지 않고,
 *
 * WasteItem
 * ↓
 * RecycleGuide
 * ↓
 * 사용자의 거주지 맞춤 배출 일정
 *
 * 으로 연결합니다.
 */
@Service
@RequiredArgsConstructor
public class ImageAnalysisResultService {

  private final ImageLogRepository
      imageLogRepository;

  private final RecycleGuideRepository
      recycleGuideRepository;

  private final RecycleGuideCheckItemRepository
      recycleGuideCheckItemRepository;

  private final RecycleScheduleQueryService
      recycleScheduleQueryService;

  private final CollectionAreaScheduleQueryService
      collectionAreaScheduleQueryService;

  /**
   * 로그인 사용자의 AI 분석 결과 화면에 필요한
   * 전체 정보를 조회합니다.
   */
  @Transactional(readOnly = true)
  public ImageAnalysisResultResponse
  getMyAnalysisResult(
      Long userId,
      Long imageLogId
  ) {
    /*
     * ImageLog ID만으로 조회하지 않고
     * 반드시 현재 로그인 사용자를 함께 검증합니다.
     */
    ImageLog imageLog =
        imageLogRepository
            .findByIdAndUserId(
                imageLogId,
                userId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode
                            .IMAGE_LOG_NOT_FOUND
                    )
            );

    /*
     * 현재 최종 품목을 가져옵니다.
     *
     * 우선순위는 ImageLog에서:
     *
     * 사용자 수정
     * >
     * 서버 YOLO
     * >
     * 모바일 TFLite
     *
     * 순으로 계산합니다.
     */
    WasteItem wasteItem =
        imageLog.getEffectiveWasteItem();

    /*
     * 분석 실패 또는 아직 분석 전이라
     * WasteItem이 없는 경우에는
     * 가이드와 일정 없이 현재 상태만 반환합니다.
     *
     * 이를 통해 Flutter에서
     *
     * UPLOADED
     * ANALYSIS_FAILED
     *
     * 같은 상태 화면도 같은 API로 처리할 수 있습니다.
     */
    if (wasteItem == null) {
      return ImageAnalysisResultResponse.of(
          imageLog,
          null,
          imageLog.getUser()
              .getResidenceType(),
          null,
          null
      );
    }

    RecycleGuideResponse guide =
        buildGuide(
            wasteItem.getId()
        );

    ResidenceType residenceType =
        imageLog.getUser()
            .getResidenceType();

    WasteItemScheduleResponse
        managedComplexSchedule =
        null;

    GeneralHousingScheduleResponse
        generalHousingSchedule =
        null;

    /*
     * 사용자의 거주 형태에 따라
     * 일정 조회 방식을 분기합니다.
     */
    if (
        residenceType
            == ResidenceType.MANAGED_COMPLEX
    ) {
      /*
       * 아파트·오피스텔 등 단지형 사용자는
       * AI가 인식한 바로 그 WasteItem의
       * 공식 일정을 조회합니다.
       */
      managedComplexSchedule =
          recycleScheduleQueryService
              .getMyWasteItemSchedule(
                  userId,
                  wasteItem.getId()
              );

    } else if (
        residenceType
            == ResidenceType.GENERAL_HOUSING
    ) {
      /*
       * 일반주택은 현재 공공데이터 구조가
       *
       * LIFE_WASTE
       * FOOD_WASTE
       * RECYCLABLE
       *
       * 세 가지 지역 수거 분류를 사용합니다.
       *
       * 현재 WasteItem 도메인에는
       * 개별 품목을 위 세 종류 중 하나로 연결하는
       * 명시적인 필드가 아직 존재하지 않습니다.
       *
       * 따라서 임의로 추측해 하나만 고르지 않고
       * 사용자의 주소에 적용되는 세 종류 일정을
       * 전체 반환합니다.
       */
      generalHousingSchedule =
          collectionAreaScheduleQueryService
              .getMyGeneralHousingSchedule(
                  userId
              );
    }

    return ImageAnalysisResultResponse.of(
        imageLog,
        guide,
        residenceType,
        managedComplexSchedule,
        generalHousingSchedule
    );
  }

  /**
   * AI가 인식한 WasteItem의
   * 분리배출 가이드와 체크리스트를 조회합니다.
   *
   * 가이드가 등록되지 않았다면 null을 반환합니다.
   */
  private RecycleGuideResponse buildGuide(
      Long wasteItemId
  ) {
    RecycleGuide recycleGuide =
        recycleGuideRepository
            .findByWasteItemId(
                wasteItemId
            )
            .orElse(null);

    if (recycleGuide == null) {
      return null;
    }

    List<RecycleGuideCheckItem> checkItems =
        recycleGuideCheckItemRepository
            .findAllByRecycleGuide_IdOrderBySortOrderAscIdAsc(
                recycleGuide.getId()
            );

    return RecycleGuideResponse.from(
        recycleGuide,
        checkItems
    );
  }
}
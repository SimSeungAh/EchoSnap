package com.echosnap.backend.domain.schedule.service;

import com.echosnap.backend.domain.apartment.entity.Apartment;
import com.echosnap.backend.domain.collectionarea.entity.CollectionArea;
import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
import com.echosnap.backend.domain.residence.entity.Residence;
import com.echosnap.backend.domain.residence.entity.ResidenceCollectionArea;
import com.echosnap.backend.domain.schedule.dto.request.CreateApartmentScheduleReportRequest;
import com.echosnap.backend.domain.schedule.dto.request.CreateGeneralHousingScheduleReportRequest;
import com.echosnap.backend.domain.schedule.dto.response.ScheduleReportResponse;
import com.echosnap.backend.domain.schedule.entity.CollectionAreaSchedule;
import com.echosnap.backend.domain.schedule.entity.RecycleSchedule;
import com.echosnap.backend.domain.schedule.entity.ScheduleReport;
import com.echosnap.backend.domain.schedule.entity.ScheduleReportType;
import com.echosnap.backend.domain.schedule.repository.CollectionAreaScheduleRepository;
import com.echosnap.backend.domain.schedule.repository.RecycleScheduleRepository;
import com.echosnap.backend.domain.schedule.repository.ScheduleReportRepository;
import com.echosnap.backend.domain.user.entity.ResidenceType;
import com.echosnap.backend.domain.user.entity.User;
import com.echosnap.backend.domain.user.repository.UserRepository;
import com.echosnap.backend.domain.waste.entity.WasteItem;
import com.echosnap.backend.domain.waste.repository.WasteItemRepository;
import com.echosnap.backend.global.exception.CustomException;
import com.echosnap.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleReportService {

  private final UserRepository userRepository;

  private final WasteItemRepository wasteItemRepository;

  private final RecycleScheduleRepository
      recycleScheduleRepository;

  private final CollectionAreaScheduleRepository
      collectionAreaScheduleRepository;

  private final ScheduleReportRepository
      scheduleReportRepository;

  /**
   * 공동주택 주민이 배출 일정 제보를 등록합니다.
   *
   * apartmentId는 Request에서 받지 않고
   * 로그인 사용자의 실제 Apartment를 사용합니다.
   */
  @Transactional
  public ScheduleReportResponse
  createApartmentReport(
      Long userId,
      CreateApartmentScheduleReportRequest request
  ) {
    User reporter =
        getUser(
            userId
        );

    Apartment apartment =
        getManagedComplexApartment(
            reporter
        );

    WasteItem wasteItem =
        wasteItemRepository
            .findById(
                request.wasteItemId()
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.WASTE_ITEM_NOT_FOUND
                    )
            );

    validateApartmentReport(
        apartment,
        wasteItem,
        request
    );

    ScheduleReport report =
        ScheduleReport.createForApartment(
            reporter,
            request.reportType(),
            apartment,
            wasteItem,
            request.referenceScheduleId(),
            request.reportedDayOfWeek(),
            request.reportedStartTime(),
            request.reportedEndTime(),
            request.reportedAlwaysAvailable(),
            request.effectiveDate(),
            request.temporaryUnavailable(),
            trimToNull(
                request.reportNote()
            )
        );

    ScheduleReport saved =
        scheduleReportRepository.save(
            report
        );

    return ScheduleReportResponse.from(
        saved
    );
  }

  /**
   * 일반주택 주민이 지역 배출 일정 제보를 등록합니다.
   *
   * collectionAreaId는 Request에서 받지 않고
   * 사용자의 Residence에 현재 연결된
   * 폐기물 종류별 CollectionArea를 사용합니다.
   */
  @Transactional
  public ScheduleReportResponse
  createGeneralHousingReport(
      Long userId,
      CreateGeneralHousingScheduleReportRequest request
  ) {
    User reporter =
        getUser(
            userId
        );

    Residence residence =
        getGeneralHousingResidence(
            reporter
        );

    ResidenceCollectionArea mapping =
        findCollectionAreaMapping(
            residence,
            request.wasteType()
        );

    CollectionArea collectionArea =
        mapping.getCollectionArea();

    validateGeneralHousingReport(
        collectionArea,
        request
    );

    ScheduleReport report =
        ScheduleReport.createForCollectionArea(
            reporter,
            request.reportType(),
            collectionArea,
            request.wasteType(),
            request.referenceScheduleId(),
            trimToNull(
                request.reportedEmissionDays()
            ),
            request.reportedStartTime(),
            request.reportedEndTime(),
            request.effectiveDate(),
            request.temporaryUnavailable(),
            trimToNull(
                request.reportNote()
            )
        );

    ScheduleReport saved =
        scheduleReportRepository.save(
            report
        );

    return ScheduleReportResponse.from(
        saved
    );
  }

  /*
   * =========================================================
   * 공동주택 검증
   * =========================================================
   */

  private void validateApartmentReport(
      Apartment apartment,
      WasteItem wasteItem,
      CreateApartmentScheduleReportRequest request
  ) {
    switch (request.reportType()) {

      case INITIAL_SCHEDULE ->
          validateApartmentInitialReport(
              apartment,
              wasteItem,
              request
          );

      case SCHEDULE_CORRECTION ->
          validateApartmentCorrectionReport(
              apartment,
              wasteItem,
              request
          );

      case TEMPORARY_CHANGE ->
          validateApartmentTemporaryReport(
              apartment,
              wasteItem,
              request
          );
    }
  }

  /**
   * 아직 공식 일정이 없는 경우에만
   * INITIAL_SCHEDULE을 등록할 수 있습니다.
   */
  private void validateApartmentInitialReport(
      Apartment apartment,
      WasteItem wasteItem,
      CreateApartmentScheduleReportRequest request
  ) {
    validateRegularOnlyFields(
        request.effectiveDate(),
        request.temporaryUnavailable()
    );

    if (request.referenceScheduleId() != null) {
      throw invalidReportRequest();
    }

    boolean officialScheduleExists =
        recycleScheduleRepository
            .existsByApartmentIdAndWasteItemId(
                apartment.getId(),
                wasteItem.getId()
            );

    if (officialScheduleExists) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_OFFICIAL_SCHEDULE_EXISTS
      );
    }

    validateApartmentRegularSchedule(
        request.reportedDayOfWeek(),
        request.reportedStartTime(),
        request.reportedEndTime(),
        request.reportedAlwaysAvailable()
    );
  }

  /**
   * 정기 일정 정정은 반드시
   * 기존 공식 일정 하나를 참조해야 합니다.
   */
  private void validateApartmentCorrectionReport(
      Apartment apartment,
      WasteItem wasteItem,
      CreateApartmentScheduleReportRequest request
  ) {
    validateRegularOnlyFields(
        request.effectiveDate(),
        request.temporaryUnavailable()
    );

    RecycleSchedule referenceSchedule =
        getRequiredApartmentReferenceSchedule(
            request.referenceScheduleId()
        );

    validateApartmentReferenceOwnership(
        referenceSchedule,
        apartment,
        wasteItem
    );

    validateApartmentRegularSchedule(
        request.reportedDayOfWeek(),
        request.reportedStartTime(),
        request.reportedEndTime(),
        request.reportedAlwaysAvailable()
    );
  }

  /**
   * 특정 날짜 변경 제보입니다.
   *
   * 특정 날짜에 아예 배출할 수 없는지,
   * 또는 그 날짜의 시간만 변경되는지를 구분합니다.
   */
  private void validateApartmentTemporaryReport(
      Apartment apartment,
      WasteItem wasteItem,
      CreateApartmentScheduleReportRequest request
  ) {
    if (
        request.effectiveDate() == null
            || request.temporaryUnavailable() == null
    ) {
      throw invalidReportRequest();
    }

    /*
     * referenceScheduleId는 선택입니다.
     *
     * 기존 일정 하나의 일시 변경이면
     * 해당 일정의 소유 범위를 반드시 검증합니다.
     */
    if (request.referenceScheduleId() != null) {
      RecycleSchedule referenceSchedule =
          getRequiredApartmentReferenceSchedule(
              request.referenceScheduleId()
          );

      validateApartmentReferenceOwnership(
          referenceSchedule,
          apartment,
          wasteItem
      );
    }

    if (
        Boolean.TRUE.equals(
            request.temporaryUnavailable()
        )
    ) {
      /*
       * "해당 날짜 배출 불가"라면
       * 새로운 시간 정보를 동시에 보내지 않습니다.
       */
      if (
          request.reportedDayOfWeek() != null
              || request.reportedStartTime() != null
              || request.reportedEndTime() != null
              || request.reportedAlwaysAvailable() != null
      ) {
        throw invalidReportRequest();
      }

      return;
    }

    validateApartmentTemporaryReplacement(
        request
    );
  }

  /**
   * 공동주택 정기 일정 입력 규칙입니다.
   *
   * 상시 배출이면 요일/시간이 없어야 하고,
   * 상시 배출이 아니면 요일과 시작/종료 시간이 모두 필요합니다.
   */
  private void validateApartmentRegularSchedule(
      java.time.DayOfWeek dayOfWeek,
      LocalTime startTime,
      LocalTime endTime,
      Boolean alwaysAvailable
  ) {
    if (alwaysAvailable == null) {
      throw invalidReportRequest();
    }

    if (Boolean.TRUE.equals(alwaysAvailable)) {
      if (
          dayOfWeek != null
              || startTime != null
              || endTime != null
      ) {
        throw invalidReportRequest();
      }

      return;
    }

    if (
        dayOfWeek == null
            || startTime == null
            || endTime == null
    ) {
      throw invalidReportRequest();
    }

    /*
     * 현재 공동주택 공식 일정 모델은
     * 기존 RecycleSchedule과 동일하게
     * 자정을 넘기는 일정을 허용하지 않습니다.
     */
    if (!startTime.isBefore(endTime)) {
      throw new CustomException(
          ErrorCode.INVALID_RECYCLE_SCHEDULE_TIME
      );
    }
  }

  /**
   * TEMPORARY_CHANGE에서
   * "수거 중단이 아닌 시간 변경"을 제보하는 경우입니다.
   */
  private void validateApartmentTemporaryReplacement(
      CreateApartmentScheduleReportRequest request
  ) {
    if (request.reportedAlwaysAvailable() == null) {
      throw invalidReportRequest();
    }

    if (
        Boolean.TRUE.equals(
            request.reportedAlwaysAvailable()
        )
    ) {
      if (
          request.reportedDayOfWeek() != null
              || request.reportedStartTime() != null
              || request.reportedEndTime() != null
      ) {
        throw invalidReportRequest();
      }

      return;
    }

    if (
        request.reportedStartTime() == null
            || request.reportedEndTime() == null
    ) {
      throw invalidReportRequest();
    }

    if (
        !request.reportedStartTime()
            .isBefore(
                request.reportedEndTime()
            )
    ) {
      throw new CustomException(
          ErrorCode.INVALID_RECYCLE_SCHEDULE_TIME
      );
    }

    /*
     * effectiveDate가 이미 날짜를 특정하므로
     * reportedDayOfWeek는 필수는 아닙니다.
     *
     * 다만 전달됐다면 실제 날짜의 요일과
     * 일치해야 합니다.
     */
    if (
        request.reportedDayOfWeek() != null
            && request.reportedDayOfWeek()
            != request.effectiveDate()
            .getDayOfWeek()
    ) {
      throw invalidReportRequest();
    }
  }

  /*
   * =========================================================
   * 일반주택 검증
   * =========================================================
   */

  private void validateGeneralHousingReport(
      CollectionArea collectionArea,
      CreateGeneralHousingScheduleReportRequest request
  ) {
    switch (request.reportType()) {

      case INITIAL_SCHEDULE ->
          validateGeneralHousingInitialReport(
              collectionArea,
              request
          );

      case SCHEDULE_CORRECTION ->
          validateGeneralHousingCorrectionReport(
              collectionArea,
              request
          );

      case TEMPORARY_CHANGE ->
          validateGeneralHousingTemporaryReport(
              collectionArea,
              request
          );
    }
  }

  /**
   * 일반주택 최초 일정 제보입니다.
   *
   * 해당 CollectionArea + wasteType에
   * 이미 공식 일정이 존재한다면
   * INITIAL_SCHEDULE이 아니라
   * SCHEDULE_CORRECTION을 사용해야 합니다.
   */
  private void validateGeneralHousingInitialReport(
      CollectionArea collectionArea,
      CreateGeneralHousingScheduleReportRequest request
  ) {
    validateRegularOnlyFields(
        request.effectiveDate(),
        request.temporaryUnavailable()
    );

    if (request.referenceScheduleId() != null) {
      throw invalidReportRequest();
    }

    boolean officialScheduleExists =
        collectionAreaScheduleRepository
            .findByCollectionAreaIdAndWasteType(
                collectionArea.getId(),
                request.wasteType()
            )
            .isPresent();

    if (officialScheduleExists) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_OFFICIAL_SCHEDULE_EXISTS
      );
    }

    validateGeneralHousingRegularSchedule(
        request.reportedEmissionDays(),
        request.reportedStartTime(),
        request.reportedEndTime()
    );
  }

  /**
   * 일반주택 정기 일정 정정입니다.
   */
  private void validateGeneralHousingCorrectionReport(
      CollectionArea collectionArea,
      CreateGeneralHousingScheduleReportRequest request
  ) {
    validateRegularOnlyFields(
        request.effectiveDate(),
        request.temporaryUnavailable()
    );

    CollectionAreaSchedule referenceSchedule =
        getRequiredCollectionAreaReferenceSchedule(
            request.referenceScheduleId()
        );

    validateCollectionAreaReferenceOwnership(
        referenceSchedule,
        collectionArea,
        request.wasteType()
    );

    validateGeneralHousingRegularSchedule(
        request.reportedEmissionDays(),
        request.reportedStartTime(),
        request.reportedEndTime()
    );
  }

  /**
   * 일반주택의 특정 날짜 변경 제보입니다.
   */
  private void validateGeneralHousingTemporaryReport(
      CollectionArea collectionArea,
      CreateGeneralHousingScheduleReportRequest request
  ) {
    if (
        request.effectiveDate() == null
            || request.temporaryUnavailable() == null
    ) {
      throw invalidReportRequest();
    }

    if (request.referenceScheduleId() != null) {
      CollectionAreaSchedule referenceSchedule =
          getRequiredCollectionAreaReferenceSchedule(
              request.referenceScheduleId()
          );

      validateCollectionAreaReferenceOwnership(
          referenceSchedule,
          collectionArea,
          request.wasteType()
      );
    }

    if (
        Boolean.TRUE.equals(
            request.temporaryUnavailable()
        )
    ) {
      if (
          hasText(
              request.reportedEmissionDays()
          )
              || request.reportedStartTime() != null
              || request.reportedEndTime() != null
      ) {
        throw invalidReportRequest();
      }

      return;
    }

    /*
     * 특정 날짜 자체는 effectiveDate가 표현하므로
     * reportedEmissionDays는 필요하지 않습니다.
     *
     * 수거 중단이 아니라면
     * 변경된 시작/종료 시간을 받아야 합니다.
     */
    if (
        request.reportedStartTime() == null
            || request.reportedEndTime() == null
    ) {
      throw invalidReportRequest();
    }

    validateGeneralHousingTimePair(
        request.reportedStartTime(),
        request.reportedEndTime()
    );
  }

  /**
   * 일반주택 정기 일정은
   * 최소한 명확한 요일 표현이 필요합니다.
   *
   * 시간은 공공데이터 특성상 없을 수 있으므로
   * 둘 다 null인 것은 허용하지만
   * 시작/종료 중 하나만 있는 것은 허용하지 않습니다.
   */
  private void validateGeneralHousingRegularSchedule(
      String emissionDays,
      LocalTime startTime,
      LocalTime endTime
  ) {
    if (!hasText(emissionDays)) {
      throw invalidReportRequest();
    }

    if (
        startTime == null
            && endTime == null
    ) {
      return;
    }

    if (
        startTime == null
            || endTime == null
    ) {
      throw invalidReportRequest();
    }

    validateGeneralHousingTimePair(
        startTime,
        endTime
    );
  }

  /**
   * 일반주택은 20:00 ~ 02:00처럼
   * 자정을 넘기는 시간을 허용합니다.
   *
   * 단, 시작과 종료가 같은 경우는
   * 24시간인지 잘못된 값인지 판단할 수 없기 때문에
   * 자동으로 해석하지 않습니다.
   */
  private void validateGeneralHousingTimePair(
      LocalTime startTime,
      LocalTime endTime
  ) {
    if (startTime.equals(endTime)) {
      throw new CustomException(
          ErrorCode.INVALID_SCHEDULE_REPORT_TIME
      );
    }
  }

  /*
   * =========================================================
   * 공식 일정 참조 검증
   * =========================================================
   */

  private RecycleSchedule
  getRequiredApartmentReferenceSchedule(
      Long referenceScheduleId
  ) {
    if (referenceScheduleId == null) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_REFERENCE_REQUIRED
      );
    }

    return recycleScheduleRepository
        .findDetailById(
            referenceScheduleId
        )
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.SCHEDULE_REPORT_REFERENCE_NOT_FOUND
                )
        );
  }

  /**
   * 기존 공동주택 일정이
   * 로그인 사용자의 실제 Apartment + WasteItem 일정인지 검증합니다.
   */
  private void validateApartmentReferenceOwnership(
      RecycleSchedule schedule,
      Apartment apartment,
      WasteItem wasteItem
  ) {
    boolean sameApartment =
        Objects.equals(
            schedule.getApartment()
                .getId(),
            apartment.getId()
        );

    boolean sameWasteItem =
        Objects.equals(
            schedule.getWasteItem()
                .getId(),
            wasteItem.getId()
        );

    if (
        !sameApartment
            || !sameWasteItem
    ) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_TARGET_MISMATCH
      );
    }
  }

  private CollectionAreaSchedule
  getRequiredCollectionAreaReferenceSchedule(
      Long referenceScheduleId
  ) {
    if (referenceScheduleId == null) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_REFERENCE_REQUIRED
      );
    }

    return collectionAreaScheduleRepository
        .findById(
            referenceScheduleId
        )
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.SCHEDULE_REPORT_REFERENCE_NOT_FOUND
                )
        );
  }

  /**
   * 기존 일반주택 일정이
   * 현재 Residence가 실제 사용하는 CollectionArea와
   * 동일한 폐기물 종류의 일정인지 검증합니다.
   */
  private void validateCollectionAreaReferenceOwnership(
      CollectionAreaSchedule schedule,
      CollectionArea collectionArea,
      CollectionWasteType wasteType
  ) {
    boolean sameCollectionArea =
        Objects.equals(
            schedule.getCollectionArea()
                .getId(),
            collectionArea.getId()
        );

    boolean sameWasteType =
        schedule.getWasteType()
            == wasteType;

    if (
        !sameCollectionArea
            || !sameWasteType
    ) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_TARGET_MISMATCH
      );
    }
  }

  /*
   * =========================================================
   * 거주지 검증
   * =========================================================
   */

  private User getUser(
      Long userId
  ) {
    return userRepository
        .findById(
            userId
        )
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.USER_NOT_FOUND
                )
        );
  }

  private Apartment getManagedComplexApartment(
      User user
  ) {
    if (
        user.getResidenceType()
            != ResidenceType.MANAGED_COMPLEX
            || user.getApartment() == null
    ) {
      throw new CustomException(
          ErrorCode.USER_APARTMENT_NOT_SET
      );
    }

    return user.getApartment();
  }

  private Residence getGeneralHousingResidence(
      User user
  ) {
    if (
        user.getResidenceType()
            != ResidenceType.GENERAL_HOUSING
            || user.getResidence() == null
    ) {
      throw new CustomException(
          ErrorCode.USER_RESIDENCE_NOT_SET
      );
    }

    return user.getResidence();
  }

  /**
   * 요청된 폐기물 종류가 실제로
   * 사용자의 Residence에 연결된 CollectionArea인지 확인합니다.
   */
  private ResidenceCollectionArea
  findCollectionAreaMapping(
      Residence residence,
      CollectionWasteType wasteType
  ) {
    return residence
        .getCollectionAreaMappings()
        .stream()
        .filter(
            mapping ->
                mapping.getWasteType()
                    == wasteType
        )
        .findFirst()
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.SCHEDULE_REPORT_COLLECTION_AREA_NOT_MATCHED
                )
        );
  }

  /*
   * =========================================================
   * 공통 검증
   * =========================================================
   */

  /**
   * INITIAL / CORRECTION 정기 일정 제보에는
   * 특정 날짜용 필드가 들어오면 안 됩니다.
   */
  private void validateRegularOnlyFields(
      java.time.LocalDate effectiveDate,
      Boolean temporaryUnavailable
  ) {
    if (
        effectiveDate != null
            || temporaryUnavailable != null
    ) {
      throw invalidReportRequest();
    }
  }

  private CustomException invalidReportRequest() {
    return new CustomException(
        ErrorCode.INVALID_SCHEDULE_REPORT
    );
  }

  private boolean hasText(
      String value
  ) {
    return value != null
        && !value.isBlank();
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
}
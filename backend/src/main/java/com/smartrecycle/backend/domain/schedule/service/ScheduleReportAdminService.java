package com.smartrecycle.backend.domain.schedule.service;

import com.smartrecycle.backend.domain.schedule.dto.request.ApproveScheduleReportRequest;
import com.smartrecycle.backend.domain.schedule.dto.request.RejectScheduleReportRequest;
import com.smartrecycle.backend.domain.schedule.dto.response.AdminScheduleReportResponse;
import com.smartrecycle.backend.domain.schedule.entity.CollectionAreaSchedule;
import com.smartrecycle.backend.domain.schedule.entity.RecycleSchedule;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleConfirmation;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleConfirmationValue;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleException;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReport;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReportStatus;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReportType;
import com.smartrecycle.backend.domain.schedule.repository.CollectionAreaScheduleRepository;
import com.smartrecycle.backend.domain.schedule.repository.RecycleScheduleRepository;
import com.smartrecycle.backend.domain.schedule.repository.ScheduleConfirmationRepository;
import com.smartrecycle.backend.domain.schedule.repository.ScheduleExceptionRepository;
import com.smartrecycle.backend.domain.schedule.repository.ScheduleReportRepository;
import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleReportAdminService {

  private final UserRepository userRepository;

  private final ScheduleReportRepository
      scheduleReportRepository;

  private final ScheduleConfirmationRepository
      scheduleConfirmationRepository;

  private final RecycleScheduleRepository
      recycleScheduleRepository;

  private final CollectionAreaScheduleRepository
      collectionAreaScheduleRepository;

  private final ScheduleExceptionRepository
      scheduleExceptionRepository;

  /**
   * 관리자가 상태별 주민 일정 제보를 조회합니다.
   *
   * status가 null이면 PENDING으로 처리합니다.
   */
  public List<AdminScheduleReportResponse>
  getReports(
      Long adminId,
      ScheduleReportStatus status
  ) {
    getAdmin(
        adminId
    );

    ScheduleReportStatus targetStatus =
        status == null
            ? ScheduleReportStatus.PENDING
            : status;

    List<ScheduleReport> reports;

    if (
        targetStatus
            == ScheduleReportStatus.PENDING
    ) {
      reports =
          scheduleReportRepository
              .findAllByStatusOrderByCreatedAtAsc(
                  targetStatus
              );
    } else {
      reports =
          scheduleReportRepository
              .findAllByStatusOrderByCreatedAtDesc(
                  targetStatus
              );
    }

    return buildResponses(
        reports
    );
  }

  /**
   * 관리자 주민 제보 상세 조회
   */
  public AdminScheduleReportResponse
  getReportDetail(
      Long adminId,
      Long reportId
  ) {
    getAdmin(
        adminId
    );

    ScheduleReport report =
        getReport(
            reportId
        );

    return buildResponse(
        report
    );
  }

  /**
   * 관리자가 주민 일정 제보를 승인합니다.
   *
   * 핵심:
   *
   * 공식 일정 반영과
   * ScheduleReport APPROVED 변경을
   * 하나의 Transaction에서 처리합니다.
   *
   * 중간에 하나라도 실패하면
   * 전체 Transaction이 rollback됩니다.
   */
  @Transactional
  public AdminScheduleReportResponse
  approveReport(
      Long adminId,
      Long reportId,
      ApproveScheduleReportRequest request
  ) {
    User admin =
        getAdmin(
            adminId
        );

    ScheduleReport report =
        getReport(
            reportId
        );

    validatePending(
        report
    );

    /*
     * 제보 유형별로
     * 실제 공식 일정 데이터를 먼저 반영합니다.
     */
    applyApprovedReport(
        report,
        request
    );

    /*
     * 공식 일정 반영이 모두 성공한 다음에만
     * ScheduleReport를 APPROVED로 변경합니다.
     */
    report.approve(
        admin,
        trimToNull(
            request.reviewNote()
        )
    );

    return buildResponse(
        report
    );
  }

  /**
   * 관리자 제보 거절
   */
  @Transactional
  public AdminScheduleReportResponse
  rejectReport(
      Long adminId,
      Long reportId,
      RejectScheduleReportRequest request
  ) {
    User admin =
        getAdmin(
            adminId
        );

    ScheduleReport report =
        getReport(
            reportId
        );

    validatePending(
        report
    );

    report.reject(
        admin,
        request.reviewNote()
            .trim()
    );

    return buildResponse(
        report
    );
  }

  /*
   * =========================================================
   * 승인 유형 분기
   * =========================================================
   */

  private void applyApprovedReport(
      ScheduleReport report,
      ApproveScheduleReportRequest request
  ) {
    ScheduleReportType reportType =
        report.getReportType();

    switch (reportType) {

      case INITIAL_SCHEDULE ->
          applyInitialSchedule(
              report
          );

      case SCHEDULE_CORRECTION ->
          applyScheduleCorrection(
              report
          );

      case TEMPORARY_CHANGE ->
          applyTemporaryChange(
              report,
              request
          );
    }
  }

  /*
   * =========================================================
   * INITIAL_SCHEDULE
   * =========================================================
   */

  /**
   * 공식 일정이 없던 곳에
   * 주민 제보를 바탕으로 최초 일정을 생성합니다.
   */
  private void applyInitialSchedule(
      ScheduleReport report
  ) {
    if (report.isApartmentReport()) {

      applyApartmentInitialSchedule(
          report
      );

      return;
    }

    if (report.isCollectionAreaReport()) {

      applyCollectionAreaInitialSchedule(
          report
      );

      return;
    }

    throw invalidApproval();
  }

  /**
   * 공동주택 최초 공식 일정 생성
   */
  private void applyApartmentInitialSchedule(
      ScheduleReport report
  ) {
    boolean scheduleExists =
        recycleScheduleRepository
            .existsByApartmentIdAndWasteItemId(
                report.getApartment()
                    .getId(),
                report.getWasteItem()
                    .getId()
            );

    /*
     * 제보 등록 당시에는 일정이 없었더라도
     * 관리자가 승인하기 전 다른 관리자가
     * 공식 일정을 만들었을 수 있으므로
     * 승인 시점에도 다시 검사합니다.
     */
    if (scheduleExists) {
      throw new CustomException(
          ErrorCode.RECYCLE_SCHEDULE_ALREADY_EXISTS
      );
    }

    Boolean alwaysAvailable =
        report.getReportedAlwaysAvailable();

    if (alwaysAvailable == null) {
      throw invalidApproval();
    }

    RecycleSchedule schedule;

    if (
        Boolean.TRUE.equals(
            alwaysAvailable
        )
    ) {
      schedule =
          RecycleSchedule
              .createAlwaysAvailable(
                  report.getApartment(),
                  report.getWasteItem()
              );
    } else {
      validateApartmentWeeklyFields(
          report
      );

      schedule =
          RecycleSchedule
              .createWeekly(
                  report.getApartment(),
                  report.getWasteItem(),
                  report.getReportedDayOfWeek(),
                  report.getReportedStartTime(),
                  report.getReportedEndTime()
              );
    }

    recycleScheduleRepository.save(
        schedule
    );
  }

  /**
   * 일반주택 최초 공식 일정 생성
   */
  private void applyCollectionAreaInitialSchedule(
      ScheduleReport report
  ) {
    boolean scheduleExists =
        collectionAreaScheduleRepository
            .findByCollectionAreaIdAndWasteType(
                report.getCollectionArea()
                    .getId(),
                report.getCollectionWasteType()
            )
            .isPresent();

    if (scheduleExists) {
      throw new CustomException(
          ErrorCode.RECYCLE_SCHEDULE_ALREADY_EXISTS
      );
    }

    if (!hasText(
        report.getReportedEmissionDays()
    )) {
      throw invalidApproval();
    }

    validateGeneralHousingTimePair(
        report.getReportedStartTime(),
        report.getReportedEndTime()
    );

    CollectionAreaSchedule schedule =
        CollectionAreaSchedule
            .createFromApprovedReport(
                report.getCollectionArea(),
                report.getCollectionWasteType(),
                report.getReportedEmissionDays()
                    .trim(),
                report.getReportedStartTime(),
                report.getReportedEndTime()
            );

    collectionAreaScheduleRepository.save(
        schedule
    );
  }

  /*
   * =========================================================
   * SCHEDULE_CORRECTION
   * =========================================================
   */

  /**
   * 기존 공식 정기 일정을 수정합니다.
   */
  private void applyScheduleCorrection(
      ScheduleReport report
  ) {
    if (report.isApartmentReport()) {

      applyApartmentCorrection(
          report
      );

      return;
    }

    if (report.isCollectionAreaReport()) {

      applyCollectionAreaCorrection(
          report
      );

      return;
    }

    throw invalidApproval();
  }

  /**
   * 공동주택 기존 일정 정정
   */
  private void applyApartmentCorrection(
      ScheduleReport report
  ) {
    RecycleSchedule schedule =
        getApartmentReferenceSchedule(
            report
        );

    validateApartmentReferenceTarget(
        report,
        schedule
    );

    Boolean alwaysAvailable =
        report.getReportedAlwaysAvailable();

    if (alwaysAvailable == null) {
      throw invalidApproval();
    }

    if (
        Boolean.TRUE.equals(
            alwaysAvailable
        )
    ) {
      /*
       * 상시 배출 일정은
       * 같은 품목의 다른 요일 일정과
       * 동시에 존재하면 안 됩니다.
       */
      boolean anotherScheduleExists =
          recycleScheduleRepository
              .existsByApartmentIdAndWasteItemIdAndIdNot(
                  report.getApartment()
                      .getId(),
                  report.getWasteItem()
                      .getId(),
                  schedule.getId()
              );

      if (anotherScheduleExists) {
        throw new CustomException(
            ErrorCode.RECYCLE_SCHEDULE_ALREADY_EXISTS
        );
      }

      schedule.updateAlwaysAvailable();

      return;
    }

    validateApartmentWeeklyFields(
        report
    );

    /*
     * 다른 상시 일정이 이미 존재하면
     * 일반 요일 일정으로 수정할 수 없습니다.
     */
    boolean anotherAlwaysSchedule =
        recycleScheduleRepository
            .existsByApartmentIdAndWasteItemIdAndAlwaysAvailableTrueAndIdNot(
                report.getApartment()
                    .getId(),
                report.getWasteItem()
                    .getId(),
                schedule.getId()
            );

    if (anotherAlwaysSchedule) {
      throw new CustomException(
          ErrorCode.RECYCLE_SCHEDULE_ALREADY_EXISTS
      );
    }

    /*
     * 수정하려는 요일에
     * 다른 공식 일정이 이미 존재하는지도 검사합니다.
     */
    boolean duplicateDay =
        recycleScheduleRepository
            .existsByApartmentIdAndWasteItemIdAndDayOfWeekAndIdNot(
                report.getApartment()
                    .getId(),
                report.getWasteItem()
                    .getId(),
                report.getReportedDayOfWeek(),
                schedule.getId()
            );

    if (duplicateDay) {
      throw new CustomException(
          ErrorCode.RECYCLE_SCHEDULE_ALREADY_EXISTS
      );
    }

    schedule.updateWeekly(
        report.getReportedDayOfWeek(),
        report.getReportedStartTime(),
        report.getReportedEndTime()
    );
  }

  /**
   * 일반주택 기존 일정 정정
   */
  private void applyCollectionAreaCorrection(
      ScheduleReport report
  ) {
    CollectionAreaSchedule schedule =
        getCollectionAreaReferenceSchedule(
            report
        );

    validateCollectionAreaReferenceTarget(
        report,
        schedule
    );

    if (!hasText(
        report.getReportedEmissionDays()
    )) {
      throw invalidApproval();
    }

    validateGeneralHousingTimePair(
        report.getReportedStartTime(),
        report.getReportedEndTime()
    );

    /*
     * 관리자 승인 주민 제보로 공식 일정 규칙을 수정합니다.
     *
     * sourceType은 내부에서
     * ADMIN_APPROVED_REPORT로 변경됩니다.
     */
    schedule.updateFromApprovedReport(
        report.getReportedEmissionDays()
            .trim(),
        report.getReportedStartTime(),
        report.getReportedEndTime()
    );
  }

  /*
   * =========================================================
   * TEMPORARY_CHANGE
   * =========================================================
   */

  /**
   * 특정 날짜에만 적용되는
   * ScheduleException을 생성합니다.
   */
  private void applyTemporaryChange(
      ScheduleReport report,
      ApproveScheduleReportRequest request
  ) {
    if (report.getEffectiveDate() == null) {
      throw invalidApproval();
    }

    if (
        report.getTemporaryUnavailable()
            == null
    ) {
      throw invalidApproval();
    }

    String publicReason =
        trimToNull(
            request.publicReason()
        );

    /*
     * TEMPORARY_CHANGE는 실제 사용자에게
     * 왜 일정이 달라졌는지 안내해야 하므로
     * 공개 사유를 필수로 사용합니다.
     */
    if (publicReason == null) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_PUBLIC_REASON_REQUIRED
      );
    }

    /*
     * 동일 제보가 이미 예외 일정으로
     * 반영된 적이 있는지 한 번 더 방어합니다.
     */
    if (
        scheduleExceptionRepository
            .existsBySourceReportId(
                report.getId()
            )
    ) {
      throw new CustomException(
          ErrorCode.SCHEDULE_EXCEPTION_ALREADY_EXISTS
      );
    }

    if (report.isApartmentReport()) {

      applyApartmentTemporaryChange(
          report,
          publicReason
      );

      return;
    }

    if (report.isCollectionAreaReport()) {

      applyCollectionAreaTemporaryChange(
          report,
          publicReason
      );

      return;
    }

    throw invalidApproval();
  }

  /**
   * 공동주택 특정 날짜 예외
   */
  private void applyApartmentTemporaryChange(
      ScheduleReport report,
      String publicReason
  ) {
    boolean sameDateExceptionExists =
        scheduleExceptionRepository
            .findByApartmentIdAndWasteItemIdAndEffectiveDate(
                report.getApartment()
                    .getId(),
                report.getWasteItem()
                    .getId(),
                report.getEffectiveDate()
            )
            .isPresent();

    if (sameDateExceptionExists) {
      throw new CustomException(
          ErrorCode.SCHEDULE_EXCEPTION_ALREADY_EXISTS
      );
    }

    boolean unavailable =
        Boolean.TRUE.equals(
            report.getTemporaryUnavailable()
        );

    if (!unavailable) {
      validateApartmentTemporaryFields(
          report
      );
    }

    ScheduleException exception =
        ScheduleException
            .createForApartment(
                report.getApartment(),
                report.getWasteItem(),
                report,
                report.getEffectiveDate(),
                unavailable,
                unavailable
                    ? null
                    : report.getReportedStartTime(),
                unavailable
                    ? null
                    : report.getReportedEndTime(),
                unavailable
                    ? null
                    : report.getReportedAlwaysAvailable(),
                publicReason
            );

    scheduleExceptionRepository.save(
        exception
    );
  }

  /**
   * 일반주택 특정 날짜 예외
   */
  private void applyCollectionAreaTemporaryChange(
      ScheduleReport report,
      String publicReason
  ) {
    boolean sameDateExceptionExists =
        scheduleExceptionRepository
            .findByCollectionAreaIdAndCollectionWasteTypeAndEffectiveDate(
                report.getCollectionArea()
                    .getId(),
                report.getCollectionWasteType(),
                report.getEffectiveDate()
            )
            .isPresent();

    if (sameDateExceptionExists) {
      throw new CustomException(
          ErrorCode.SCHEDULE_EXCEPTION_ALREADY_EXISTS
      );
    }

    boolean unavailable =
        Boolean.TRUE.equals(
            report.getTemporaryUnavailable()
        );

    if (!unavailable) {
      validateGeneralHousingTemporaryFields(
          report
      );
    }

    ScheduleException exception =
        ScheduleException
            .createForCollectionArea(
                report.getCollectionArea(),
                report.getCollectionWasteType(),
                report,
                report.getEffectiveDate(),
                unavailable,
                unavailable
                    ? null
                    : report.getReportedStartTime(),
                unavailable
                    ? null
                    : report.getReportedEndTime(),
                publicReason
            );

    scheduleExceptionRepository.save(
        exception
    );
  }

  /*
   * =========================================================
   * 기존 공식 일정 참조 검증
   * =========================================================
   */

  private RecycleSchedule
  getApartmentReferenceSchedule(
      ScheduleReport report
  ) {
    if (
        report.getReferenceScheduleId()
            == null
    ) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_REFERENCE_REQUIRED
      );
    }

    return recycleScheduleRepository
        .findDetailById(
            report.getReferenceScheduleId()
        )
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.SCHEDULE_REPORT_REFERENCE_NOT_FOUND
                )
        );
  }

  private void validateApartmentReferenceTarget(
      ScheduleReport report,
      RecycleSchedule schedule
  ) {
    boolean sameApartment =
        Objects.equals(
            report.getApartment()
                .getId(),
            schedule.getApartment()
                .getId()
        );

    boolean sameWasteItem =
        Objects.equals(
            report.getWasteItem()
                .getId(),
            schedule.getWasteItem()
                .getId()
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
  getCollectionAreaReferenceSchedule(
      ScheduleReport report
  ) {
    if (
        report.getReferenceScheduleId()
            == null
    ) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_REFERENCE_REQUIRED
      );
    }

    return collectionAreaScheduleRepository
        .findById(
            report.getReferenceScheduleId()
        )
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.SCHEDULE_REPORT_REFERENCE_NOT_FOUND
                )
        );
  }

  private void validateCollectionAreaReferenceTarget(
      ScheduleReport report,
      CollectionAreaSchedule schedule
  ) {
    boolean sameCollectionArea =
        Objects.equals(
            report.getCollectionArea()
                .getId(),
            schedule.getCollectionArea()
                .getId()
        );

    boolean sameWasteType =
        report.getCollectionWasteType()
            == schedule.getWasteType();

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
   * 승인 데이터 재검증
   * =========================================================
   */

  /**
   * 공동주택 정기 일정 필드 검증
   */
  private void validateApartmentWeeklyFields(
      ScheduleReport report
  ) {
    if (
        report.getReportedDayOfWeek()
            == null
            || report.getReportedStartTime()
            == null
            || report.getReportedEndTime()
            == null
    ) {
      throw invalidApproval();
    }

    if (
        !report.getReportedStartTime()
            .isBefore(
                report.getReportedEndTime()
            )
    ) {
      throw new CustomException(
          ErrorCode.INVALID_RECYCLE_SCHEDULE_TIME
      );
    }
  }

  /**
   * 공동주택 특정 날짜 변경 검증
   */
  private void validateApartmentTemporaryFields(
      ScheduleReport report
  ) {
    Boolean alwaysAvailable =
        report.getReportedAlwaysAvailable();

    if (alwaysAvailable == null) {
      throw invalidApproval();
    }

    if (
        Boolean.TRUE.equals(
            alwaysAvailable
        )
    ) {
      if (
          report.getReportedStartTime()
              != null
              || report.getReportedEndTime()
              != null
      ) {
        throw invalidApproval();
      }

      return;
    }

    if (
        report.getReportedStartTime()
            == null
            || report.getReportedEndTime()
            == null
    ) {
      throw invalidApproval();
    }

    if (
        !report.getReportedStartTime()
            .isBefore(
                report.getReportedEndTime()
            )
    ) {
      throw new CustomException(
          ErrorCode.INVALID_RECYCLE_SCHEDULE_TIME
      );
    }
  }

  /**
   * 일반주택 정기 일정의 시간 정보 검증
   *
   * 공공데이터 특성상
   * 시작/종료 시간이 모두 없는 것은 허용합니다.
   */
  private void validateGeneralHousingTimePair(
      LocalTime startTime,
      LocalTime endTime
  ) {
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
      throw invalidApproval();
    }

    /*
     * 20:00 → 02:00은 정상입니다.
     *
     * 같은 시간만 애매하므로 막습니다.
     */
    if (startTime.equals(endTime)) {
      throw new CustomException(
          ErrorCode.INVALID_SCHEDULE_REPORT_TIME
      );
    }
  }

  /**
   * 일반주택 특정 날짜 시간 변경은
   * 시작/종료 시간 모두 필요합니다.
   */
  private void validateGeneralHousingTemporaryFields(
      ScheduleReport report
  ) {
    LocalTime startTime =
        report.getReportedStartTime();

    LocalTime endTime =
        report.getReportedEndTime();

    if (
        startTime == null
            || endTime == null
    ) {
      throw invalidApproval();
    }

    if (startTime.equals(endTime)) {
      throw new CustomException(
          ErrorCode.INVALID_SCHEDULE_REPORT_TIME
      );
    }
  }

  /*
   * =========================================================
   * 관리자 / 제보 검증
   * =========================================================
   */

  private User getAdmin(
      Long adminId
  ) {
    User user =
        userRepository
            .findById(
                adminId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.USER_NOT_FOUND
                    )
            );

    if (
        user.getRole()
            != Role.ADMIN
    ) {
      throw new CustomException(
          ErrorCode.FORBIDDEN
      );
    }

    return user;
  }

  private ScheduleReport getReport(
      Long reportId
  ) {
    return scheduleReportRepository
        .findById(
            reportId
        )
        .orElseThrow(
            () ->
                new CustomException(
                    ErrorCode.SCHEDULE_REPORT_NOT_FOUND
                )
        );
  }

  private void validatePending(
      ScheduleReport report
  ) {
    if (!report.isPending()) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_NOT_PENDING
      );
    }
  }

  /*
   * =========================================================
   * 응답
   * =========================================================
   */

  private AdminScheduleReportResponse
  buildResponse(
      ScheduleReport report
  ) {
    long confirmedCount =
        scheduleConfirmationRepository
            .countByScheduleReportIdAndValue(
                report.getId(),
                ScheduleConfirmationValue.CONFIRMED
            );

    long differentCount =
        scheduleConfirmationRepository
            .countByScheduleReportIdAndValue(
                report.getId(),
                ScheduleConfirmationValue.DIFFERENT
            );

    return AdminScheduleReportResponse.of(
        report,
        confirmedCount,
        differentCount
    );
  }

  private List<AdminScheduleReportResponse>
  buildResponses(
      List<ScheduleReport> reports
  ) {
    if (reports.isEmpty()) {
      return List.of();
    }

    List<Long> reportIds =
        reports.stream()
            .map(
                ScheduleReport::getId
            )
            .toList();

    List<ScheduleConfirmation> confirmations =
        scheduleConfirmationRepository
            .findAllByScheduleReportIdIn(
                reportIds
            );

    Map<Long, ConfirmationCount> countMap =
        new HashMap<>();

    for (
        ScheduleConfirmation confirmation
        : confirmations
    ) {
      Long reportId =
          confirmation
              .getScheduleReport()
              .getId();

      ConfirmationCount count =
          countMap.computeIfAbsent(
              reportId,
              ignored ->
                  new ConfirmationCount()
          );

      count.add(
          confirmation.getValue()
      );
    }

    List<AdminScheduleReportResponse> responses =
        new ArrayList<>();

    for (ScheduleReport report : reports) {

      ConfirmationCount count =
          countMap.getOrDefault(
              report.getId(),
              new ConfirmationCount()
          );

      responses.add(
          AdminScheduleReportResponse.of(
              report,
              count.confirmedCount,
              count.differentCount
          )
      );
    }

    return List.copyOf(
        responses
    );
  }

  /*
   * =========================================================
   * 공통 Helper
   * =========================================================
   */

  private CustomException invalidApproval() {
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

  private static class ConfirmationCount {

    private long confirmedCount;

    private long differentCount;

    private void add(
        ScheduleConfirmationValue value
    ) {
      if (
          value
              == ScheduleConfirmationValue.CONFIRMED
      ) {
        confirmedCount++;
        return;
      }

      if (
          value
              == ScheduleConfirmationValue.DIFFERENT
      ) {
        differentCount++;
      }
    }
  }
}
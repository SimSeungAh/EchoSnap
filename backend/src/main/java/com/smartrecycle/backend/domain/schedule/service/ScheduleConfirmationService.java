package com.smartrecycle.backend.domain.schedule.service;

import com.smartrecycle.backend.domain.residence.entity.Residence;
import com.smartrecycle.backend.domain.residence.entity.ResidenceCollectionArea;
import com.smartrecycle.backend.domain.schedule.dto.request.UpdateScheduleConfirmationRequest;
import com.smartrecycle.backend.domain.schedule.dto.response.ScheduleConfirmationResponse;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleConfirmation;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleConfirmationValue;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReport;
import com.smartrecycle.backend.domain.schedule.repository.ScheduleConfirmationRepository;
import com.smartrecycle.backend.domain.schedule.repository.ScheduleReportRepository;
import com.smartrecycle.backend.domain.user.entity.ResidenceType;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleConfirmationService {

  private final UserRepository userRepository;

  private final ScheduleReportRepository
      scheduleReportRepository;

  private final ScheduleConfirmationRepository
      scheduleConfirmationRepository;

  /**
   * 일정 제보에 주민 확인 값을 등록하거나 변경합니다.
   *
   * 같은 사용자가 같은 제보에 여러 행을 만드는 것이 아니라
   * 기존 확인이 존재하면 값을 변경합니다.
   */
  @Transactional
  public ScheduleConfirmationResponse
  confirmReport(
      Long userId,
      Long reportId,
      UpdateScheduleConfirmationRequest request
  ) {
    User confirmer =
        getUser(
            userId
        );

    ScheduleReport report =
        getReport(
            reportId
        );

    validateReportIsPending(
        report
    );

    validateNotOwnReport(
        report,
        confirmer
    );

    validateSameScheduleScope(
        report,
        confirmer
    );

    ScheduleConfirmation confirmation =
        scheduleConfirmationRepository
            .findByScheduleReportIdAndConfirmerId(
                reportId,
                userId
            )
            .map(
                existing -> {
                  existing.changeValue(
                      request.value()
                  );

                  return existing;
                }
            )
            .orElseGet(
                () ->
                    ScheduleConfirmation.create(
                        report,
                        confirmer,
                        request.value()
                    )
            );

    ScheduleConfirmation saved =
        scheduleConfirmationRepository
            .save(
                confirmation
            );

    long confirmedCount =
        scheduleConfirmationRepository
            .countByScheduleReportIdAndValue(
                reportId,
                ScheduleConfirmationValue.CONFIRMED
            );

    long differentCount =
        scheduleConfirmationRepository
            .countByScheduleReportIdAndValue(
                reportId,
                ScheduleConfirmationValue.DIFFERENT
            );

    return ScheduleConfirmationResponse.of(
        saved,
        confirmedCount,
        differentCount
    );
  }

  /**
   * 관리자가 이미 승인 또는 거절한 제보는
   * 주민 확인을 새로 남기거나 변경할 수 없습니다.
   */
  private void validateReportIsPending(
      ScheduleReport report
  ) {
    if (!report.isPending()) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_NOT_PENDING
      );
    }
  }

  /**
   * 제보자가 자신의 제보에
   * 직접 "맞아요"를 누르는 것을 막습니다.
   */
  private void validateNotOwnReport(
      ScheduleReport report,
      User confirmer
  ) {
    if (
        Objects.equals(
            report.getReporter()
                .getId(),
            confirmer.getId()
        )
    ) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_SELF_CONFIRMATION_NOT_ALLOWED
      );
    }
  }

  /**
   * 제보와 동일한 일정 적용 범위에
   * 거주하는 사용자만 확인할 수 있습니다.
   */
  private void validateSameScheduleScope(
      ScheduleReport report,
      User confirmer
  ) {
    if (report.isApartmentReport()) {
      validateApartmentScope(
          report,
          confirmer
      );

      return;
    }

    if (report.isCollectionAreaReport()) {
      validateCollectionAreaScope(
          report,
          confirmer
      );

      return;
    }

    /*
     * Apartment와 CollectionArea가 모두 없는 제보는
     * 정상적으로 만들어질 수 없는 데이터입니다.
     */
    throw new CustomException(
        ErrorCode.SCHEDULE_REPORT_CONFIRMATION_SCOPE_MISMATCH
    );
  }

  /**
   * 공동주택 제보는
   * 실제 같은 Apartment 주민만 확인할 수 있습니다.
   */
  private void validateApartmentScope(
      ScheduleReport report,
      User confirmer
  ) {
    if (
        confirmer.getResidenceType()
            != ResidenceType.MANAGED_COMPLEX
            || confirmer.getApartment() == null
    ) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_CONFIRMATION_SCOPE_MISMATCH
      );
    }

    boolean sameApartment =
        Objects.equals(
            confirmer.getApartment()
                .getId(),
            report.getApartment()
                .getId()
        );

    if (!sameApartment) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_CONFIRMATION_SCOPE_MISMATCH
      );
    }
  }

  /**
   * 일반주택 제보는 단순히 같은 시군구에 사는 것만으로
   * 확인할 수 있는 것이 아닙니다.
   *
   * 제보 대상 폐기물 종류에 대해
   * 사용자의 Residence에 실제 연결된 CollectionArea가
   * 제보의 CollectionArea와 같아야 합니다.
   */
  private void validateCollectionAreaScope(
      ScheduleReport report,
      User confirmer
  ) {
    if (
        confirmer.getResidenceType()
            != ResidenceType.GENERAL_HOUSING
            || confirmer.getResidence() == null
    ) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_CONFIRMATION_SCOPE_MISMATCH
      );
    }

    Residence residence =
        confirmer.getResidence();

    boolean sameScheduleScope =
        residence
            .getCollectionAreaMappings()
            .stream()
            .anyMatch(
                mapping ->
                    isSameCollectionAreaMapping(
                        mapping,
                        report
                    )
            );

    if (!sameScheduleScope) {
      throw new CustomException(
          ErrorCode.SCHEDULE_REPORT_CONFIRMATION_SCOPE_MISMATCH
      );
    }
  }

  /**
   * CollectionArea뿐 아니라 wasteType도 함께 확인합니다.
   *
   * 같은 주소라도
   * 생활쓰레기 / 음식물 / 재활용의 수거구역이
   * 서로 다를 수 있기 때문입니다.
   */
  private boolean isSameCollectionAreaMapping(
      ResidenceCollectionArea mapping,
      ScheduleReport report
  ) {
    boolean sameWasteType =
        mapping.getWasteType()
            == report.getCollectionWasteType();

    boolean sameCollectionArea =
        Objects.equals(
            mapping
                .getCollectionArea()
                .getId(),
            report
                .getCollectionArea()
                .getId()
        );

    return sameWasteType
        && sameCollectionArea;
  }

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
}
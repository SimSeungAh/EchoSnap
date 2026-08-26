package com.smartrecycle.backend.domain.schedule.service;

import com.smartrecycle.backend.domain.residence.entity.Residence;
import com.smartrecycle.backend.domain.residence.entity.ResidenceCollectionArea;
import com.smartrecycle.backend.domain.schedule.dto.response.ScheduleReportViewResponse;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleConfirmation;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleConfirmationValue;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReport;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReportStatus;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleReportQueryService {

  private final UserRepository userRepository;

  private final ScheduleReportRepository
      scheduleReportRepository;

  private final ScheduleConfirmationRepository
      scheduleConfirmationRepository;

  /**
   * 로그인 사용자가 직접 작성한
   * 주민 일정 제보 전체를 최신순으로 조회합니다.
   */
  public List<ScheduleReportViewResponse>
  getMyReports(
      Long userId
  ) {
    User user =
        getUser(
            userId
        );

    List<ScheduleReport> reports =
        scheduleReportRepository
            .findAllByReporterIdOrderByCreatedAtDesc(
                userId
            );

    return buildResponses(
        reports,
        user
    );
  }

  /**
   * 현재 로그인 사용자가
   * 확인에 참여할 수 있는 PENDING 제보를 조회합니다.
   */
  public List<ScheduleReportViewResponse>
  getConfirmableReports(
      Long userId
  ) {
    User user =
        getUser(
            userId
        );

    List<ScheduleReport> reports =
        findReportsForUserScope(
            user
        );

    Map<Long, ScheduleReport> uniqueReports =
        new LinkedHashMap<>();

    for (ScheduleReport report : reports) {

      if (
          Objects.equals(
              report.getReporter()
                  .getId(),
              userId
          )
      ) {
        continue;
      }

      uniqueReports.put(
          report.getId(),
          report
      );
    }

    List<ScheduleReport> result =
        new ArrayList<>(
            uniqueReports.values()
        );

    result.sort(
        (first, second) ->
            second.getCreatedAt()
                .compareTo(
                    first.getCreatedAt()
                )
    );

    return buildResponses(
        result,
        user
    );
  }

  /**
   * 주민 제보 상세 조회
   */
  public ScheduleReportViewResponse
  getReportDetail(
      Long userId,
      Long reportId
  ) {
    User user =
        getUser(
            userId
        );

    ScheduleReport report =
        getReport(
            reportId
        );

    boolean ownReport =
        Objects.equals(
            report.getReporter()
                .getId(),
            userId
        );

    if (
        !ownReport
            && !isSameScheduleScope(
            report,
            user
        )
    ) {
      throw new CustomException(
          ErrorCode.FORBIDDEN
      );
    }

    List<ScheduleReportViewResponse> responses =
        buildResponses(
            List.of(report),
            user
        );

    return responses.get(0);
  }

  /**
   * 사용자의 현재 거주 형태를 기준으로
   * 확인 가능한 PENDING 제보를 찾습니다.
   */
  private List<ScheduleReport>
  findReportsForUserScope(
      User user
  ) {
    if (
        user.getResidenceType()
            == ResidenceType.MANAGED_COMPLEX
    ) {
      if (user.getApartment() == null) {
        return List.of();
      }

      return scheduleReportRepository
          .findAllByApartmentIdAndStatusOrderByCreatedAtDesc(
              user.getApartment()
                  .getId(),
              ScheduleReportStatus.PENDING
          );
    }

    if (
        user.getResidenceType()
            == ResidenceType.GENERAL_HOUSING
    ) {
      return findGeneralHousingReports(
          user
      );
    }

    return List.of();
  }

  /**
   * 일반주택의 폐기물 종류별
   * CollectionArea에 해당하는 제보를 조회합니다.
   */
  private List<ScheduleReport>
  findGeneralHousingReports(
      User user
  ) {
    Residence residence =
        user.getResidence();

    if (residence == null) {
      return List.of();
    }

    List<ScheduleReport> result =
        new ArrayList<>();

    for (
        ResidenceCollectionArea mapping
        : residence.getCollectionAreaMappings()
    ) {
      List<ScheduleReport> areaReports =
          scheduleReportRepository
              .findAllByCollectionAreaIdAndStatusOrderByCreatedAtDesc(
                  mapping
                      .getCollectionArea()
                      .getId(),
                  ScheduleReportStatus.PENDING
              );

      for (
          ScheduleReport report
          : areaReports
      ) {
        if (
            report.getCollectionWasteType()
                == mapping.getWasteType()
        ) {
          result.add(
              report
          );
        }
      }
    }

    return result;
  }

  /**
   * 여러 제보의 주민 확인 정보를
   * 한 번에 가져와 집계합니다.
   */
  private List<ScheduleReportViewResponse>
  buildResponses(
      List<ScheduleReport> reports,
      User currentUser
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

    Map<Long, ConfirmationSummary> summaries =
        new HashMap<>();

    for (
        ScheduleConfirmation confirmation
        : confirmations
    ) {
      Long reportId =
          confirmation
              .getScheduleReport()
              .getId();

      ConfirmationSummary summary =
          summaries.computeIfAbsent(
              reportId,
              ignored ->
                  new ConfirmationSummary()
          );

      summary.add(
          confirmation,
          currentUser.getId()
      );
    }

    List<ScheduleReportViewResponse> responses =
        new ArrayList<>();

    for (ScheduleReport report : reports) {

      ConfirmationSummary summary =
          summaries.getOrDefault(
              report.getId(),
              new ConfirmationSummary()
          );

      boolean ownReport =
          Objects.equals(
              report.getReporter()
                  .getId(),
              currentUser.getId()
          );

      boolean canConfirm =
          report.isPending()
              && !ownReport
              && isSameScheduleScope(
              report,
              currentUser
          );

      responses.add(
          ScheduleReportViewResponse.of(
              report,
              summary.confirmedCount,
              summary.differentCount,
              summary.myConfirmationValue,
              canConfirm
          )
      );
    }

    return List.copyOf(
        responses
    );
  }

  private boolean isSameScheduleScope(
      ScheduleReport report,
      User user
  ) {
    if (report.isApartmentReport()) {
      return isSameApartmentScope(
          report,
          user
      );
    }

    if (report.isCollectionAreaReport()) {
      return isSameCollectionAreaScope(
          report,
          user
      );
    }

    return false;
  }

  private boolean isSameApartmentScope(
      ScheduleReport report,
      User user
  ) {
    if (
        user.getResidenceType()
            != ResidenceType.MANAGED_COMPLEX
            || user.getApartment() == null
    ) {
      return false;
    }

    return Objects.equals(
        user.getApartment()
            .getId(),
        report.getApartment()
            .getId()
    );
  }

  private boolean isSameCollectionAreaScope(
      ScheduleReport report,
      User user
  ) {
    if (
        user.getResidenceType()
            != ResidenceType.GENERAL_HOUSING
            || user.getResidence() == null
    ) {
      return false;
    }

    return user.getResidence()
        .getCollectionAreaMappings()
        .stream()
        .anyMatch(
            mapping ->
                Objects.equals(
                    mapping
                        .getCollectionArea()
                        .getId(),
                    report
                        .getCollectionArea()
                        .getId()
                )
                    && mapping
                    .getWasteType()
                    == report
                    .getCollectionWasteType()
        );
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

  private static class ConfirmationSummary {

    private long confirmedCount;

    private long differentCount;

    private ScheduleConfirmationValue
        myConfirmationValue;

    private void add(
        ScheduleConfirmation confirmation,
        Long currentUserId
    ) {
      if (
          confirmation.getValue()
              == ScheduleConfirmationValue.CONFIRMED
      ) {
        confirmedCount++;
      }

      if (
          confirmation.getValue()
              == ScheduleConfirmationValue.DIFFERENT
      ) {
        differentCount++;
      }

      if (
          Objects.equals(
              confirmation
                  .getConfirmer()
                  .getId(),
              currentUserId
          )
      ) {
        myConfirmationValue =
            confirmation.getValue();
      }
    }
  }
}
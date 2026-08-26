package com.smartrecycle.backend.domain.schedule.dto.response;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReport;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReportStatus;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReportType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 주민 일정 제보 응답입니다.
 *
 * 공동주택과 일반주택 제보를
 * 하나의 응답 형태로 반환합니다.
 *
 * 대상 유형에 따라 사용하지 않는 필드는 null입니다.
 */
public record ScheduleReportResponse(

    Long id,

    ScheduleReportType reportType,

    ScheduleReportStatus status,

    /**
     * APARTMENT
     * 또는
     * COLLECTION_AREA
     */
    String targetType,

    /*
     * ---------------------------------------------------------
     * 제보자
     * ---------------------------------------------------------
     */

    Long reporterId,

    String reporterNickname,

    /*
     * ---------------------------------------------------------
     * 공동주택 대상
     * ---------------------------------------------------------
     */

    Long apartmentId,

    String apartmentName,

    Long wasteItemId,

    String wasteItemName,

    /*
     * ---------------------------------------------------------
     * 일반주택 대상
     * ---------------------------------------------------------
     */

    Long collectionAreaId,

    String collectionAreaName,

    String targetAreaName,

    CollectionWasteType collectionWasteType,

    /*
     * ---------------------------------------------------------
     * 기존 공식 일정
     * ---------------------------------------------------------
     */

    Long referenceScheduleId,

    /*
     * ---------------------------------------------------------
     * 제보 일정
     * ---------------------------------------------------------
     */

    DayOfWeek reportedDayOfWeek,

    String reportedEmissionDays,

    LocalTime reportedStartTime,

    LocalTime reportedEndTime,

    Boolean reportedAlwaysAvailable,

    /*
     * ---------------------------------------------------------
     * 특정 날짜 변경
     * ---------------------------------------------------------
     */

    LocalDate effectiveDate,

    Boolean temporaryUnavailable,

    String reportNote,

    /*
     * ---------------------------------------------------------
     * 관리자 검토
     * ---------------------------------------------------------
     */

    Long reviewedById,

    String reviewedByNickname,

    String reviewNote,

    LocalDateTime reviewedAt,

    /*
     * ---------------------------------------------------------
     * 생성/수정 일시
     * ---------------------------------------------------------
     */

    LocalDateTime createdAt,

    LocalDateTime updatedAt

) {

  public static ScheduleReportResponse from(
      ScheduleReport report
  ) {
    boolean apartmentReport =
        report.isApartmentReport();

    return new ScheduleReportResponse(

        report.getId(),

        report.getReportType(),

        report.getStatus(),

        apartmentReport
            ? "APARTMENT"
            : "COLLECTION_AREA",

        /*
         * 제보자
         */
        report.getReporter().getId(),

        report.getReporter().getNickname(),

        /*
         * 공동주택
         */
        apartmentReport
            ? report.getApartment().getId()
            : null,

        apartmentReport
            ? report.getApartment().getName()
            : null,

        apartmentReport
            ? report.getWasteItem().getId()
            : null,

        apartmentReport
            ? report.getWasteItem().getName()
            : null,

        /*
         * 일반주택
         */
        report.isCollectionAreaReport()
            ? report.getCollectionArea().getId()
            : null,

        report.isCollectionAreaReport()
            ? report.getCollectionArea().getAreaName()
            : null,

        report.isCollectionAreaReport()
            ? report.getCollectionArea().getTargetAreaName()
            : null,

        report.getCollectionWasteType(),

        /*
         * 기존 공식 일정
         */
        report.getReferenceScheduleId(),

        /*
         * 주민 제보 내용
         */
        report.getReportedDayOfWeek(),

        report.getReportedEmissionDays(),

        report.getReportedStartTime(),

        report.getReportedEndTime(),

        report.getReportedAlwaysAvailable(),

        /*
         * 일시 변경
         */
        report.getEffectiveDate(),

        report.getTemporaryUnavailable(),

        report.getReportNote(),

        /*
         * 관리자 검토
         */
        report.getReviewedBy() != null
            ? report.getReviewedBy().getId()
            : null,

        report.getReviewedBy() != null
            ? report.getReviewedBy().getNickname()
            : null,

        report.getReviewNote(),

        report.getReviewedAt(),

        /*
         * BaseEntity
         */
        report.getCreatedAt(),

        report.getUpdatedAt()
    );
  }
}
package com.smartrecycle.backend.domain.schedule.repository;

import com.smartrecycle.backend.domain.schedule.entity.ScheduleReport;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleReportRepository
    extends JpaRepository<ScheduleReport, Long> {

  /**
   * 사용자가 자신이 등록한 제보 목록을
   * 최신순으로 조회합니다.
   */
  List<ScheduleReport>
  findAllByReporterIdOrderByCreatedAtDesc(
      Long reporterId
  );

  /**
   * 자신의 특정 제보를 조회합니다.
   */
  Optional<ScheduleReport>
  findByIdAndReporterId(
      Long reportId,
      Long reporterId
  );

  /**
   * 관리자 검토 대기 목록에서 사용합니다.
   *
   * 먼저 접수된 제보를 먼저 검토할 수 있도록
   * 오래된 순으로 조회합니다.
   */
  List<ScheduleReport>
  findAllByStatusOrderByCreatedAtAsc(
      ScheduleReportStatus status
  );

  /**
   * 관리자 승인/거절 이력을 조회할 때 사용합니다.
   *
   * 완료된 제보는 최근 처리 대상부터
   * 볼 수 있도록 최신순으로 조회합니다.
   */
  List<ScheduleReport>
  findAllByStatusOrderByCreatedAtDesc(
      ScheduleReportStatus status
  );

  /**
   * 특정 공동주택에서
   * 해당 상태의 주민 제보를 조회합니다.
   */
  List<ScheduleReport>
  findAllByApartmentIdAndStatusOrderByCreatedAtDesc(
      Long apartmentId,
      ScheduleReportStatus status
  );

  /**
   * 특정 일반주택 수거구역에서
   * 해당 상태의 주민 제보를 조회합니다.
   */
  List<ScheduleReport>
  findAllByCollectionAreaIdAndStatusOrderByCreatedAtDesc(
      Long collectionAreaId,
      ScheduleReportStatus status
  );
}
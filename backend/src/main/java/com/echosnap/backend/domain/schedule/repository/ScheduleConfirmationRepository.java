package com.echosnap.backend.domain.schedule.repository;

import com.echosnap.backend.domain.schedule.entity.ScheduleConfirmation;
import com.echosnap.backend.domain.schedule.entity.ScheduleConfirmationValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScheduleConfirmationRepository
    extends JpaRepository<ScheduleConfirmation, Long> {

  /**
   * 특정 사용자가 해당 제보에 남긴
   * 주민 확인 값을 조회합니다.
   */
  Optional<ScheduleConfirmation>
  findByScheduleReportIdAndConfirmerId(
      Long scheduleReportId,
      Long confirmerId
  );

  /**
   * 특정 제보의 전체 주민 확인 내역입니다.
   */
  List<ScheduleConfirmation>
  findAllByScheduleReportId(
      Long scheduleReportId
  );

  /**
   * 여러 제보의 주민 확인 내역을
   * 한 번에 조회합니다.
   *
   * 목록 화면에서 제보마다 DB를 반복 조회하지 않고
   * 한 번에 Confirmation을 가져오기 위해 사용합니다.
   */
  List<ScheduleConfirmation>
  findAllByScheduleReportIdIn(
      Collection<Long> scheduleReportIds
  );

  /**
   * 특정 확인 값의 개수를 집계합니다.
   */
  long countByScheduleReportIdAndValue(
      Long scheduleReportId,
      ScheduleConfirmationValue value
  );

  /**
   * 사용자가 이미 해당 제보를 확인했는지 조회합니다.
   */
  boolean existsByScheduleReportIdAndConfirmerId(
      Long scheduleReportId,
      Long confirmerId
  );
}
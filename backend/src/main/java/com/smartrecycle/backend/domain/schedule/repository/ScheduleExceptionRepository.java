package com.smartrecycle.backend.domain.schedule.repository;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.schedule.entity.ScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScheduleExceptionRepository
    extends JpaRepository<ScheduleException, Long> {

  /**
   * 공동주택의 특정 품목에 대해
   * 특정 날짜 예외를 조회합니다.
   */
  Optional<ScheduleException>
  findByApartmentIdAndWasteItemIdAndEffectiveDate(
      Long apartmentId,
      Long wasteItemId,
      LocalDate effectiveDate
  );

  /**
   * 일반주택의 특정 폐기물 종류에 대해
   * 특정 날짜 예외를 조회합니다.
   */
  Optional<ScheduleException>
  findByCollectionAreaIdAndCollectionWasteTypeAndEffectiveDate(
      Long collectionAreaId,
      CollectionWasteType collectionWasteType,
      LocalDate effectiveDate
  );

  /**
   * 공동주택 특정 품목의
   * 기준 날짜 이후 예외 전체 조회
   */
  List<ScheduleException>
  findAllByApartmentIdAndWasteItemIdAndEffectiveDateGreaterThanEqualOrderByEffectiveDateAsc(
      Long apartmentId,
      Long wasteItemId,
      LocalDate effectiveDate
  );

  /**
   * 공동주택 전체 품목의
   * 기준 날짜 이후 예외를 한 번에 조회
   */
  List<ScheduleException>
  findAllByApartmentIdAndEffectiveDateGreaterThanEqualOrderByEffectiveDateAsc(
      Long apartmentId,
      LocalDate effectiveDate
  );

  /**
   * 일반주택 사용자의 여러 CollectionArea에 대한
   * 예외 일정을 한 번에 조회합니다.
   *
   * Residence에 생활쓰레기 / 음식물 / 재활용품의
   * CollectionArea가 각각 연결되어 있어도
   * 폐기물 종류마다 별도 DB 조회를 하지 않도록 합니다.
   */
  List<ScheduleException>
  findAllByCollectionAreaIdInAndEffectiveDateGreaterThanEqualOrderByEffectiveDateAsc(
      Collection<Long> collectionAreaIds,
      LocalDate effectiveDate
  );

  /**
   * 공동주택 기간 내 예외 조회
   */
  List<ScheduleException>
  findAllByApartmentIdAndWasteItemIdAndEffectiveDateBetweenOrderByEffectiveDateAsc(
      Long apartmentId,
      Long wasteItemId,
      LocalDate startDate,
      LocalDate endDate
  );

  /**
   * 일반주택 기간 내 예외 조회
   */
  List<ScheduleException>
  findAllByCollectionAreaIdAndCollectionWasteTypeAndEffectiveDateBetweenOrderByEffectiveDateAsc(
      Long collectionAreaId,
      CollectionWasteType collectionWasteType,
      LocalDate startDate,
      LocalDate endDate
  );

  /**
   * 동일 주민 제보가 이미
   * ScheduleException으로 반영됐는지 확인
   */
  boolean existsBySourceReportId(
      Long sourceReportId
  );

  /**
   * 특정 날짜 전체 예외 조회
   */
  List<ScheduleException>
  findAllByEffectiveDateOrderByIdAsc(
      LocalDate effectiveDate
  );
}
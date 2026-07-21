package com.smartrecycle.backend.domain.schedule.repository;

import com.smartrecycle.backend.domain.schedule.entity.RecycleSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface RecycleScheduleRepository
        extends JpaRepository<RecycleSchedule, Long> {

    /**
     * 일정 상세 조회
     * 일정과 연결된 아파트, 폐기물 품목, 카테고리를 한 번에 조회
     */
    @Query("""
            select schedule
            from RecycleSchedule schedule
            join fetch schedule.apartment apartment
            join fetch schedule.wasteItem wasteItem
            join fetch wasteItem.category category
            where schedule.id = :scheduleId
            """)
    Optional<RecycleSchedule> findDetailById(
            @Param("scheduleId") Long scheduleId
    );

    /**
     * 특정 아파트의 전체 공식 배출 일정을 조회합니다.
     * 관리자 일정 목록과 사용자 주간 일정 화면에서 사용
     */
    @Query("""
            select schedule
            from RecycleSchedule schedule
            join fetch schedule.wasteItem wasteItem
            join fetch wasteItem.category category
            where schedule.apartment.id = :apartmentId
            order by schedule.alwaysAvailable desc,
                     wasteItem.name asc,
                     schedule.startTime asc
            """)
    List<RecycleSchedule> findAllByApartmentId(
            @Param("apartmentId") Long apartmentId
    );

    /**
     * 특정 아파트와 폐기물 품목에 등록된 모든 배출 일정을 조회
     * 품목 상세 화면에서 해당 품목의 요일별 일정을 표시할 때 사용
     */
    @Query("""
            select schedule
            from RecycleSchedule schedule
            join fetch schedule.wasteItem wasteItem
            join fetch wasteItem.category category
            where schedule.apartment.id = :apartmentId
              and schedule.wasteItem.id = :wasteItemId
            order by schedule.alwaysAvailable desc,
                     schedule.startTime asc
            """)
    List<RecycleSchedule> findAllByApartmentIdAndWasteItemId(
            @Param("apartmentId") Long apartmentId,
            @Param("wasteItemId") Long wasteItemId
    );

    /**
     * 특정 아파트에서 지정한 요일에 배출할 수 있는 일정을 조회
     * 지정된 요일 일정뿐 아니라 상시 배출 일정도 함께 반환
     */
    @Query("""
            select schedule
            from RecycleSchedule schedule
            join fetch schedule.wasteItem wasteItem
            join fetch wasteItem.category category
            where schedule.apartment.id = :apartmentId
              and (
                    schedule.alwaysAvailable = true
                    or schedule.dayOfWeek = :dayOfWeek
              )
            order by schedule.alwaysAvailable desc,
                     schedule.startTime asc,
                     wasteItem.name asc
            """)
    List<RecycleSchedule> findAvailableSchedulesByDay(
            @Param("apartmentId") Long apartmentId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek
    );

    /**
     * 해당 아파트와 품목에 일정이 하나라도 존재하는지 확인
     * 상시 배출 일정 등록 시 기존 요일 일정과 동시에 존재하지 않도록 검사할 때 사용
     */
    boolean existsByApartmentIdAndWasteItemId(
            Long apartmentId,
            Long wasteItemId
    );

    /**
     * 해당 아파트와 품목에 상시 배출 일정이 존재하는지 확인
     * 상시 배출 일정이 있으면 별도의 요일 일정을 추가할 수 없도록 검사
     */
    boolean existsByApartmentIdAndWasteItemIdAndAlwaysAvailableTrue(
            Long apartmentId,
            Long wasteItemId
    );

    /**
     * 같은 아파트, 같은 품목, 같은 요일의 일정이 존재하는지 확인
     */
    boolean existsByApartmentIdAndWasteItemIdAndDayOfWeek(
            Long apartmentId,
            Long wasteItemId,
            DayOfWeek dayOfWeek
    );

    /**
     * 수정 중인 일정 자신을 제외하고 같은 아파트와 품목의 일정이 존재하는지 확인
     */
    boolean existsByApartmentIdAndWasteItemIdAndIdNot(
            Long apartmentId,
            Long wasteItemId,
            Long scheduleId
    );

    /**
     * 수정 중인 일정 자신을 제외하고 상시 배출 일정이 존재하는지 확인
     */
    boolean existsByApartmentIdAndWasteItemIdAndAlwaysAvailableTrueAndIdNot(
            Long apartmentId,
            Long wasteItemId,
            Long scheduleId
    );

    /**
     * 수정 중인 일정 자신을 제외하고 같은 요일의 일정이 존재하는지 확인
     */
    boolean existsByApartmentIdAndWasteItemIdAndDayOfWeekAndIdNot(
            Long apartmentId,
            Long wasteItemId,
            DayOfWeek dayOfWeek,
            Long scheduleId
    );
}
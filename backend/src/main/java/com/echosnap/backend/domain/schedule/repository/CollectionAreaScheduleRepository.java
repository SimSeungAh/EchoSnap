package com.echosnap.backend.domain.schedule.repository;

import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
import com.echosnap.backend.domain.schedule.entity.CollectionAreaSchedule;
import com.echosnap.backend.domain.schedule.entity.CollectionAreaScheduleSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CollectionAreaScheduleRepository
    extends JpaRepository<CollectionAreaSchedule, Long> {

  /**
   * 특정 CollectionArea의
   * 전체 생활폐기물 일정을 조회합니다.
   */
  List<CollectionAreaSchedule>
  findAllByCollectionAreaId(
      Long collectionAreaId
  );

  /**
   * 특정 CollectionArea와 폐기물 종류의
   * 일정을 조회합니다.
   *
   * DB UniqueConstraint에 의해
   * 한 종류당 하나의 공식 일정만 존재합니다.
   */
  Optional<CollectionAreaSchedule>
  findByCollectionAreaIdAndWasteType(
      Long collectionAreaId,
      CollectionWasteType wasteType
  );

  /**
   * 여러 CollectionArea의 일정을
   * 한 번에 조회합니다.
   */
  List<CollectionAreaSchedule>
  findAllByCollectionAreaIdIn(
      Collection<Long> collectionAreaIds
  );

  /**
   * 공공데이터 중복 판별용 조회입니다.
   *
   * 같은 지역의 CollectionArea 후보 여러 개를 대상으로
   * CollectionAreaSchedule과 CollectionArea를
   * 한 번에 로딩합니다.
   *
   * Service에서
   *
   * 지역 + 폐기물종류 + 요일 + 시간 + 방법 + 장소
   *
   * 를 모두 비교하기 위해 사용합니다.
   */
  @Query("""
      select schedule
      from CollectionAreaSchedule schedule
      join fetch schedule.collectionArea area
      where area.id in :collectionAreaIds
      """)
  List<CollectionAreaSchedule>
  findAllWithCollectionAreaByCollectionAreaIdIn(
      @Param("collectionAreaIds")
      Collection<Long> collectionAreaIds
  );

  /**
   * 특정 CollectionArea의 일정 전체를 삭제합니다.
   */
  void deleteAllByCollectionAreaId(
      Long collectionAreaId
  );

  /**
   * 관리자 일반주택 배출 일정 검색
   *
   * keyword:
   * - 관리구역명
   * - 대상지역명
   * - 시/도
   * - 시/군/구
   *
   * collectionAreaId:
   * 특정 수거구역만 조회
   *
   * wasteType:
   * LIFE_WASTE / FOOD_WASTE / RECYCLABLE
   *
   * sourceType:
   * PUBLIC_DATA / ADMIN_APPROVED_REPORT
   */
  @Query(
      value = """
                    select schedule
                    from CollectionAreaSchedule schedule
                    join schedule.collectionArea area
                    where (
                        :collectionAreaId is null
                        or area.id = :collectionAreaId
                    )
                    and (
                        :wasteType is null
                        or schedule.wasteType = :wasteType
                    )
                    and (
                        :sourceType is null
                        or schedule.sourceType = :sourceType
                    )
                    and (
                        :keyword = ''
                        or lower(area.areaName)
                            like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(area.targetAreaName, ''))
                            like lower(concat('%', :keyword, '%'))
                        or lower(area.sido)
                            like lower(concat('%', :keyword, '%'))
                        or lower(area.sigungu)
                            like lower(concat('%', :keyword, '%'))
                    )
                    """,
      countQuery = """
                    select count(schedule)
                    from CollectionAreaSchedule schedule
                    join schedule.collectionArea area
                    where (
                        :collectionAreaId is null
                        or area.id = :collectionAreaId
                    )
                    and (
                        :wasteType is null
                        or schedule.wasteType = :wasteType
                    )
                    and (
                        :sourceType is null
                        or schedule.sourceType = :sourceType
                    )
                    and (
                        :keyword = ''
                        or lower(area.areaName)
                            like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(area.targetAreaName, ''))
                            like lower(concat('%', :keyword, '%'))
                        or lower(area.sido)
                            like lower(concat('%', :keyword, '%'))
                        or lower(area.sigungu)
                            like lower(concat('%', :keyword, '%'))
                    )
                    """
  )
  Page<CollectionAreaSchedule>
  searchAdminSchedules(
      @Param("keyword")
      String keyword,

      @Param("collectionAreaId")
      Long collectionAreaId,

      @Param("wasteType")
      CollectionWasteType wasteType,

      @Param("sourceType")
      CollectionAreaScheduleSourceType sourceType,

      Pageable pageable
  );
}
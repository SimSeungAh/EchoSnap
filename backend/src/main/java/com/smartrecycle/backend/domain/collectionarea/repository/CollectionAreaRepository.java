package com.smartrecycle.backend.domain.collectionarea.repository;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollectionAreaRepository
    extends JpaRepository<CollectionArea, Long> {

  /**
   * 같은 공공데이터 관리번호가 다시 들어왔을 때
   * 기존 CollectionArea를 찾습니다.
   */
  Optional<CollectionArea>
  findBySourceTypeAndExternalManagementNumber(
      CollectionAreaSourceType sourceType,
      String externalManagementNumber
  );

  /**
   * 실제 지역정보가 같은 공공데이터 CollectionArea 후보를
   * 모두 조회합니다.
   *
   * 공공데이터 중복 판별에서는
   * 이 후보들의 실제 일정까지 비교합니다.
   */
  List<CollectionArea>
  findAllBySourceTypeAndSidoAndSigunguAndAreaNameAndTargetAreaNameOrderByIdAsc(
      CollectionAreaSourceType sourceType,
      String sido,
      String sigungu,
      String areaName,
      String targetAreaName
  );

  /**
   * 주소가 속한 시/도와 시/군/구를 기준으로
   * 현재 사용 가능한 수거구역 후보를 조회합니다.
   */
  List<CollectionArea>
  findAllBySidoAndSigunguAndActiveTrue(
      String sido,
      String sigungu
  );

  /**
   * 기존 관리자 원본 단위 검색.
   *
   * 기존 코드와 다른 기능의 호환성을 위해 유지합니다.
   *
   * 새 수거구역 관리자 목록에서는
   * searchAdminCollectionAreaGroups()를 사용합니다.
   */
  @Query(
      value = """
                    select area
                    from CollectionArea area
                    where (
                        :sourceType is null
                        or area.sourceType = :sourceType
                    )
                    and (
                        :active is null
                        or area.active = :active
                    )
                    and (
                        :keyword = ''
                        or lower(area.sido)
                            like lower(concat('%', :keyword, '%'))
                        or lower(area.sigungu)
                            like lower(concat('%', :keyword, '%'))
                        or lower(area.areaName)
                            like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(area.targetAreaName, ''))
                            like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(area.externalManagementNumber, ''))
                            like lower(concat('%', :keyword, '%'))
                    )
                    """,
      countQuery = """
                    select count(area)
                    from CollectionArea area
                    where (
                        :sourceType is null
                        or area.sourceType = :sourceType
                    )
                    and (
                        :active is null
                        or area.active = :active
                    )
                    and (
                        :keyword = ''
                        or lower(area.sido)
                            like lower(concat('%', :keyword, '%'))
                        or lower(area.sigungu)
                            like lower(concat('%', :keyword, '%'))
                        or lower(area.areaName)
                            like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(area.targetAreaName, ''))
                            like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(area.externalManagementNumber, ''))
                            like lower(concat('%', :keyword, '%'))
                    )
                    """
  )
  Page<CollectionArea> searchAdminAreas(
      @Param("keyword")
      String keyword,

      @Param("sourceType")
      CollectionAreaSourceType sourceType,

      @Param("active")
      Boolean active,

      Pageable pageable
  );

  /**
   * 수거구역 관리자 목록용 지역 그룹 조회.
   *
   * 실제 화면 표시 지역:
   *
   * target_area_name이 존재하면 target_area_name
   * target_area_name이 없으면 area_name
   *
   * 을 사용합니다.
   *
   * CollectionArea 원본을 페이지네이션한 후
   * 프론트에서 묶는 것이 아니라,
   * DB에서 지역 그룹을 먼저 생성한 후
   * 그 그룹을 페이지네이션합니다.
   */
  @Query(
      value = """
          select
              ca.sido as sido,
              ca.sigungu as sigungu,
              coalesce(
                  nullif(trim(ca.target_area_name), ''),
                  ca.area_name
              ) as targetAreaName,
              ca.source_type as sourceTypeName,
              ca.active as active,
              count(*) as originalCount
          from collection_areas ca
          where (
              :sourceType is null
              or ca.source_type = :sourceType
          )
          and (
              :active is null
              or ca.active = :active
          )
          and (
              :keyword = ''
              or lower(ca.sido)
                  like lower(concat('%', :keyword, '%'))
              or lower(ca.sigungu)
                  like lower(concat('%', :keyword, '%'))
              or lower(ca.area_name)
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(ca.target_area_name, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(ca.external_management_number, ''))
                  like lower(concat('%', :keyword, '%'))
          )
          group by
              ca.sido,
              ca.sigungu,
              coalesce(
                  nullif(trim(ca.target_area_name), ''),
                  ca.area_name
              ),
              ca.source_type,
              ca.active
          order by
              ca.sido asc,
              ca.sigungu asc,
              coalesce(
                  nullif(trim(ca.target_area_name), ''),
                  ca.area_name
              ) asc,
              ca.source_type asc,
              ca.active desc
          """,
      countQuery = """
          select count(*)
          from (
              select
                  ca.sido,
                  ca.sigungu,
                  coalesce(
                      nullif(trim(ca.target_area_name), ''),
                      ca.area_name
                  ) as target_area_group,
                  ca.source_type,
                  ca.active
              from collection_areas ca
              where (
                  :sourceType is null
                  or ca.source_type = :sourceType
              )
              and (
                  :active is null
                  or ca.active = :active
              )
              and (
                  :keyword = ''
                  or lower(ca.sido)
                      like lower(concat('%', :keyword, '%'))
                  or lower(ca.sigungu)
                      like lower(concat('%', :keyword, '%'))
                  or lower(ca.area_name)
                      like lower(concat('%', :keyword, '%'))
                  or lower(coalesce(ca.target_area_name, ''))
                      like lower(concat('%', :keyword, '%'))
                  or lower(coalesce(ca.external_management_number, ''))
                      like lower(concat('%', :keyword, '%'))
              )
              group by
                  ca.sido,
                  ca.sigungu,
                  coalesce(
                      nullif(trim(ca.target_area_name), ''),
                      ca.area_name
                  ),
                  ca.source_type,
                  ca.active
          ) grouped_collection_areas
          """,
      nativeQuery = true
  )
  Page<AdminCollectionAreaGroupProjection>
  searchAdminCollectionAreaGroups(
      @Param("keyword")
      String keyword,

      @Param("sourceType")
      String sourceType,

      @Param("active")
      Boolean active,

      Pageable pageable
  );

  /**
   * 수거구역 관리자 지역 그룹에 속하는
   * 실제 CollectionArea 원본 전체 조회.
   *
   * 목록과 동일한 대상지역 계산 규칙을 사용합니다.
   */
  @Query(
      value = """
          select ca.*
          from collection_areas ca
          where ca.sido = :sido
          and ca.sigungu = :sigungu
          and coalesce(
              nullif(trim(ca.target_area_name), ''),
              ca.area_name
          ) = :targetAreaName
          and ca.source_type = :sourceType
          and ca.active = :active
          order by ca.id asc
          """,
      nativeQuery = true
  )
  List<CollectionArea>
  findAllByAdminCollectionAreaGroup(
      @Param("sido")
      String sido,

      @Param("sigungu")
      String sigungu,

      @Param("targetAreaName")
      String targetAreaName,

      @Param("sourceType")
      String sourceType,

      @Param("active")
      boolean active
  );

  /**
   * 기존 배출 일정 관리 화면용 지역 그룹입니다.
   *
   * 이 메서드는 기존 일정 관리 기능에서 사용하므로
   * 이번 작업에서는 그대로 보존합니다.
   */
  @Query("""
            select
                area.sido as sido,
                area.sigungu as sigungu,
                area.areaName as areaName,
                area.targetAreaName as targetAreaName,
                area.sourceType as sourceType,
                area.active as active
            from CollectionArea area
            where (
                :sourceType is null
                or area.sourceType = :sourceType
            )
            and (
                :active is null
                or area.active = :active
            )
            and (
                :keyword = ''
                or lower(area.sido)
                    like lower(concat('%', :keyword, '%'))
                or lower(area.sigungu)
                    like lower(concat('%', :keyword, '%'))
                or lower(area.areaName)
                    like lower(concat('%', :keyword, '%'))
                or lower(coalesce(area.targetAreaName, ''))
                    like lower(concat('%', :keyword, '%'))
                or lower(coalesce(area.externalManagementNumber, ''))
                    like lower(concat('%', :keyword, '%'))
            )
            group by
                area.sido,
                area.sigungu,
                area.areaName,
                area.targetAreaName,
                area.sourceType,
                area.active
            order by
                area.sido asc,
                area.sigungu asc,
                area.areaName asc,
                area.targetAreaName asc,
                area.sourceType asc,
                area.active desc
            """)
  List<AdminAreaGroupProjection>
  searchAdminAreaGroups(
      @Param("keyword")
      String keyword,

      @Param("sourceType")
      CollectionAreaSourceType sourceType,

      @Param("active")
      Boolean active
  );

  /**
   * 기존 배출 일정 지역 그룹의
   * 실제 CollectionArea 원본 조회.
   *
   * 일정 관리 코드와의 호환성을 위해 그대로 유지합니다.
   */
  @Query("""
            select area
            from CollectionArea area
            where area.sido = :sido
            and area.sigungu = :sigungu
            and area.areaName = :areaName
            and (
                (
                    :targetAreaName is null
                    and area.targetAreaName is null
                )
                or area.targetAreaName = :targetAreaName
            )
            and area.sourceType = :sourceType
            and area.active = :active
            order by area.id asc
            """)
  List<CollectionArea>
  findAllByAdminAreaGroup(
      @Param("sido")
      String sido,

      @Param("sigungu")
      String sigungu,

      @Param("areaName")
      String areaName,

      @Param("targetAreaName")
      String targetAreaName,

      @Param("sourceType")
      CollectionAreaSourceType sourceType,

      @Param("active")
      boolean active
  );

  /**
   * 수거구역 관리자 목록용 Projection.
   */
  interface AdminCollectionAreaGroupProjection {

    String getSido();

    String getSigungu();

    String getTargetAreaName();

    String getSourceTypeName();

    Boolean getActive();

    Long getOriginalCount();
  }

  /**
   * 기존 배출 일정 관리용 Projection.
   */
  interface AdminAreaGroupProjection {

    String getSido();

    String getSigungu();

    String getAreaName();

    String getTargetAreaName();

    CollectionAreaSourceType getSourceType();

    Boolean getActive();
  }
}
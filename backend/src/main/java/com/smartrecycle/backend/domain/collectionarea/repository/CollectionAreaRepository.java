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
   * 기존 관리자 수거구역 관리 화면용 검색.
   *
   * 이 API는 CollectionArea 원본 단위로 페이지네이션합니다.
   * 수거구역 관리 화면에서는 그대로 사용합니다.
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
   * 배출 일정 관리 화면용 "실제 표시 지역" 그룹입니다.
   *
   * 같은
   *
   * 시도 + 시군구 + 관리구역명 + 대상지역명 + 출처 + 활성상태
   *
   * 를 하나의 관리자 화면 행으로 묶습니다.
   *
   * 여기서는 CollectionArea 원본 개수를 페이지네이션하지 않고
   * 먼저 지역 그룹 목록을 생성합니다.
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
   * 지역 그룹 하나에 포함되는 실제 CollectionArea 원본 전체 조회.
   *
   * 상세 화면에서는 이 데이터를 펼쳐서
   * 배출 방법·장소 등 실제 차이를 확인합니다.
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
   * Spring Data interface projection.
   *
   * Entity를 전부 가져오지 않고
   * 관리자 배출일정 목록에 필요한 지역 그룹 키만 조회합니다.
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
package com.smartrecycle.backend.domain.collectionarea.repository;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionAreaSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionAreaRepository
    extends JpaRepository<CollectionArea, Long> {

  /**
   * 공공데이터를 다시 동기화할 때
   * 외부 관리번호로 기존 수거구역을 찾습니다.
   */
  Optional<CollectionArea>
  findBySourceTypeAndExternalManagementNumber(
      CollectionAreaSourceType sourceType,
      String externalManagementNumber
  );

  /**
   * 주소가 속한 시/도와 시/군/구를 기준으로
   * 현재 사용 가능한 수거구역 후보를 조회합니다.
   *
   * 이후 Residence의 행정동/법정동 정보와
   * targetAreaName을 비교해 최종 수거구역을 결정합니다.
   */
  List<CollectionArea>
  findAllBySidoAndSigunguAndActiveTrue(
      String sido,
      String sigungu
  );
}
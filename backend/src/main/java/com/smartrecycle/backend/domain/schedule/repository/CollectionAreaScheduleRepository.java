package com.smartrecycle.backend.domain.schedule.repository;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.schedule.entity.CollectionAreaSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

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
   * 한 종류당 하나의 원본 일정만 존재합니다.
   */
  Optional<CollectionAreaSchedule>
  findByCollectionAreaIdAndWasteType(
      Long collectionAreaId,
      CollectionWasteType wasteType
  );

  /**
   * 여러 CollectionArea의 일정을
   * 한 번에 조회할 때 사용합니다.
   *
   * 일반주택 Residence는 생활쓰레기,
   * 음식물쓰레기, 재활용품마다
   * 서로 다른 CollectionArea를 가질 수 있으므로
   * 이후 사용자 일정 조회에서 사용합니다.
   */
  List<CollectionAreaSchedule>
  findAllByCollectionAreaIdIn(
      Collection<Long> collectionAreaIds
  );

  /**
   * 특정 CollectionArea의 일정 전체를 삭제합니다.
   *
   * 공공데이터 재동기화 과정에서
   * 필요할 경우 사용할 수 있습니다.
   */
  void deleteAllByCollectionAreaId(
      Long collectionAreaId
  );
}
package com.echosnap.backend.domain.waste.repository;

import com.echosnap.backend.domain.waste.entity.RecycleGuideCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecycleGuideCheckItemRepository
        extends JpaRepository<RecycleGuideCheckItem, Long> {

    /**
     * 특정 분리배출 가이드의 체크리스트를 표시 순서와 ID 순서로 조회
     * sortOrder가 같은 항목이 있더라도 ID를 두 번째 정렬 기준으로 사용해 항상 일정한 순서로 반환
     */
    List<RecycleGuideCheckItem>
    findAllByRecycleGuide_IdOrderBySortOrderAscIdAsc(
            Long recycleGuideId
    );

    /**
     * 특정 분리배출 가이드에 속한 모든 체크리스트 항목을 삭제
     * 관리자 가이드 수정 시 기존 체크리스트를 지우고 요청으로 들어온 새 체크리스트를 저장할 때 사용
     */
    void deleteAllByRecycleGuide_Id(
            Long recycleGuideId
    );
}
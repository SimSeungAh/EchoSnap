package com.smartrecycle.backend.domain.waste.repository;

import com.smartrecycle.backend.domain.waste.entity.RecycleGuide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecycleGuideRepository
        extends JpaRepository<RecycleGuide, Long> {

    /**
     * 폐기물 품목 ID로 분리배출 가이드를 조회
     * 일반 사용자용 품목 상세 조회와 관리자용 가이드 등록·수정에서 사용
     */
    Optional<RecycleGuide> findByWasteItemId(
            Long wasteItemId
    );

    /**
     * 해당 폐기물 품목에 가이드가 이미 등록되어 있는지 확인
     * 하나의 품목에는 가이드를 하나만 등록할 수 있음
     */
    boolean existsByWasteItemId(
            Long wasteItemId
    );

    /**
     * 폐기물 품목 ID를 기준으로 가이드를 삭제
     * 현재 5단계에서는 주로 등록 또는 수정 방식을 사용하지만, 이후 관리자 기능 확장 시 가이드 초기화에 사용할 수 있음
     */
    void deleteByWasteItemId(
            Long wasteItemId
    );
}
package com.smartrecycle.backend.domain.waste.repository;

import com.smartrecycle.backend.domain.waste.entity.WasteCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WasteCategoryRepository
        extends JpaRepository<WasteCategory, Long> {

    /**
     * 활성화된 폐기물 카테고리를 표시 순서대로 조회
     * 일반 사용자용 카테고리 목록 API에서 사용
     */
    List<WasteCategory>
    findAllByActiveTrueOrderBySortOrderAscNameAsc();

    /**
     * 활성화된 카테고리를 ID로 조회
     * 일반 사용자에게 비활성화된 카테고리가 노출되는 것을 방지할 때 사용
     */
    Optional<WasteCategory> findByIdAndActiveTrue(
            Long id
    );

    /**
     * 카테고리 코드로 조회
     * 영문 대소문자를 구분하지 않음
     */
    Optional<WasteCategory> findByCodeIgnoreCase(
            String code
    );

    /**
     * 동일한 카테고리 코드가 이미 존재하는지 확인
     * 신규 카테고리 등록 시 사용
     */
    boolean existsByCodeIgnoreCase(
            String code
    );

    /**
     * 현재 수정 중인 카테고리를 제외하고  동일한 코드가 존재하는지 확인
     * 카테고리 수정 시 자기 자신의 코드를 중복으로 판단하지 않도록 사용
     */
    boolean existsByCodeIgnoreCaseAndIdNot(
            String code,
            Long id
    );
}
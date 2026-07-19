package com.smartrecycle.backend.domain.waste.repository;

import com.smartrecycle.backend.domain.waste.entity.WasteItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WasteItemRepository
        extends JpaRepository<WasteItem, Long> {

    /**
     * 일반 사용자용 폐기물 품목 목록을 검색
     * 검색 조건:
     * - 활성화된 품목만 조회
     * - categoryId가 없으면 전체 카테고리 조회
     * - keyword가 없으면 전체 품목 조회
     * - 품목명 또는 추가 검색 키워드에서 검색
     */
    @Query(
            value = """
                    select item
                    from WasteItem item
                    join fetch item.category category
                    where item.active = true
                      and category.active = true
                      and (
                          :categoryId is null
                          or category.id = :categoryId
                      )
                      and (
                          :keyword = ''
                          or lower(item.name)
                              like lower(concat('%', :keyword, '%'))
                          or lower(coalesce(item.searchKeywords, ''))
                              like lower(concat('%', :keyword, '%'))
                      )
                    """,
            countQuery = """
                    select count(item)
                    from WasteItem item
                    join item.category category
                    where item.active = true
                      and category.active = true
                      and (
                          :categoryId is null
                          or category.id = :categoryId
                      )
                      and (
                          :keyword = ''
                          or lower(item.name)
                              like lower(concat('%', :keyword, '%'))
                          or lower(coalesce(item.searchKeywords, ''))
                              like lower(concat('%', :keyword, '%'))
                      )
                    """
    )
    Page<WasteItem> searchActiveItems(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    /**
     * 관리자용 폐기물 품목 목록을 검색
     * 일반 사용자용 조회와 달리 비활성화된 품목도 조회할 수 있음
     * active:
     * - null  : 활성·비활성 전체
     * - true  : 활성 품목만
     * - false : 비활성 품목만
     */
    @Query(
            value = """
                    select item
                    from WasteItem item
                    join fetch item.category category
                    where (
                          :categoryId is null
                          or category.id = :categoryId
                      )
                      and (
                          :active is null
                          or item.active = :active
                      )
                      and (
                          :keyword = ''
                          or lower(item.name)
                              like lower(concat('%', :keyword, '%'))
                          or lower(coalesce(item.searchKeywords, ''))
                              like lower(concat('%', :keyword, '%'))
                      )
                    """,
            countQuery = """
                    select count(item)
                    from WasteItem item
                    join item.category category
                    where (
                          :categoryId is null
                          or category.id = :categoryId
                      )
                      and (
                          :active is null
                          or item.active = :active
                      )
                      and (
                          :keyword = ''
                          or lower(item.name)
                              like lower(concat('%', :keyword, '%'))
                          or lower(coalesce(item.searchKeywords, ''))
                              like lower(concat('%', :keyword, '%'))
                      )
                    """
    )
    Page<WasteItem> searchAdminItems(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    /**
     * 활성화된 품목을 ID로 조회
     * 일반 사용자용 상세 조회 API에서 사용
     */
    Optional<WasteItem> findByIdAndActiveTrue(
            Long id
    );

    /**
     * 같은 카테고리 안에 동일한 품목명이 존재하는지 확인
     * 관리자 품목 신규 등록 시 사용
     */
    boolean existsByCategoryIdAndNameIgnoreCase(
            Long categoryId,
            String name
    );

    /**
     * 수정 중인 품목을 제외하고 동일한 품목명이 존재하는지 확인
     * 관리자 품목 수정 시 자기 자신을 중복으로 판단하지 않도록 함
     */
    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(
            Long categoryId,
            String name,
            Long id
    );
}
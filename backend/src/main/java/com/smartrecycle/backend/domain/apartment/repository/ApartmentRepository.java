package com.smartrecycle.backend.domain.apartment.repository;

import com.smartrecycle.backend.domain.apartment.entity.Apartment;
import com.smartrecycle.backend.domain.apartment.entity.ApartmentStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApartmentRepository
        extends JpaRepository<Apartment, Long> {

    /**
     * 건물관리번호 중복 여부를 확인합니다.
     */
    boolean existsByBuildingManagementNumber(
            String buildingManagementNumber
    );

    /**
     * 건물관리번호로 아파트를 조회
     */
    Optional<Apartment> findByBuildingManagementNumber(
            String buildingManagementNumber
    );

    /**
     * 특정 상태의 아파트만 ID로 조회
     * 일반 사용자가 승인된 아파트만 조회하거나 선택할 때 사용
     */
    Optional<Apartment> findByIdAndStatus(
            Long id,
            ApartmentStatus status
    );

    /**
     * 승인 상태와 검색어를 기준으로 아파트를 검색합니다.
     * 검색 대상:
     * - 아파트 이름
     * - 도로명 주소
     * - 지번 주소
     *
     * 검색어가 빈 문자열이면 해당 상태의 아파트를 전체 조회
     */
    @Query("""
      select apartment
      from Apartment apartment
      where apartment.status = :status
        and (
          :keyword = ''
          or lower(apartment.name)
              like lower(concat('%', :keyword, '%'))
          or lower(apartment.roadAddress)
              like lower(concat('%', :keyword, '%'))
          or lower(coalesce(apartment.jibunAddress, ''))
              like lower(concat('%', :keyword, '%'))
        )
      """)
    Page<Apartment> searchByStatusAndKeyword(
            @Param("status") ApartmentStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
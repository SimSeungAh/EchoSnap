package com.smartrecycle.backend.domain.user.repository;

import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository
    extends JpaRepository<User, Long>,
    JpaSpecificationExecutor<User> {

  Optional<User> findByEmail(
      String email
  );

  boolean existsByEmail(
      String email
  );

  long countByRole(
      Role role
  );

  long countByStatus(
      UserStatus status
  );

  /**
   * 관리자 사용자 목록 검색.
   *
   * 검색 대상:
   * - 이메일
   * - 닉네임
   * - 공동주택명
   * - 공동주택 도로명/지번 주소
   * - 일반주택 주소/도로명/지번 주소
   *
   * apartment, residence는 User에서 LAZY 관계이므로
   * 목록 DTO 변환 시 N+1 조회를 피하기 위해 EntityGraph로
   * 함께 조회합니다.
   */
  @EntityGraph(
      attributePaths = {
          "apartment",
          "residence"
      }
  )
  @Query(
      value = """
          select u
          from User u
          left join u.apartment apartment
          left join u.residence residence
          where (
              :keyword = ''
              or lower(u.email)
                  like lower(concat('%', :keyword, '%'))
              or lower(u.nickname)
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(apartment.name, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(apartment.roadAddress, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(apartment.jibunAddress, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(residence.addressName, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(residence.roadAddress, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(residence.jibunAddress, ''))
                  like lower(concat('%', :keyword, '%'))
          )
          """,
      countQuery = """
          select count(u)
          from User u
          left join u.apartment apartment
          left join u.residence residence
          where (
              :keyword = ''
              or lower(u.email)
                  like lower(concat('%', :keyword, '%'))
              or lower(u.nickname)
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(apartment.name, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(apartment.roadAddress, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(apartment.jibunAddress, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(residence.addressName, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(residence.roadAddress, ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(coalesce(residence.jibunAddress, ''))
                  like lower(concat('%', :keyword, '%'))
          )
          """
  )
  Page<User> searchAdminUsers(
      @Param("keyword")
      String keyword,
      Pageable pageable
  );

  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
      update User u
         set u.role = :role
       where u.id = :userId
      """)
  int updateRole(
      @Param("userId")
      Long userId,

      @Param("role")
      Role role
  );
}

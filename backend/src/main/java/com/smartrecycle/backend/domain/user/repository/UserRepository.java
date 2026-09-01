package com.smartrecycle.backend.domain.user.repository;

import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.entity.UserStatus;
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
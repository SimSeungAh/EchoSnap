package com.smartrecycle.backend.domain.image.repository;

import com.smartrecycle.backend.domain.image.entity.ImageLog;
import com.smartrecycle.backend.domain.image.entity.ImageReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageLogRepository
    extends JpaRepository<ImageLog, Long> {

  /**
   * 사용자가 자신의 이미지 분석 이력을
   * 최신순으로 조회합니다.
   */
  Page<ImageLog>
  findAllByUserIdOrderByCreatedAtDesc(
      Long userId,
      Pageable pageable
  );

  /**
   * 사용자가 자신의 특정 이미지 분석 이력을 조회합니다.
   *
   * imageLogId뿐 아니라 userId까지 함께 조회하여
   * 다른 사용자의 ImageLog 접근을 차단합니다.
   */
  Optional<ImageLog>
  findByIdAndUserId(
      Long imageLogId,
      Long userId
  );

  /**
   * 실제 저장 파일명과 로그인 사용자를 함께 사용하여
   * 자신의 이미지 파일만 조회합니다.
   *
   * storedFileName이 UUID라 하더라도
   * UUID를 알고 있다는 사실만으로
   * 파일 접근 권한을 부여하지 않습니다.
   */
  Optional<ImageLog>
  findByStoredFileNameAndUserId(
      String storedFileName,
      Long userId
  );

  /**
   * 관리자 검수 화면에서
   * 상태별 이미지 로그를 조회합니다.
   */
  Page<ImageLog>
  findAllByReviewStatusOrderByCreatedAtAsc(
      ImageReviewStatus reviewStatus,
      Pageable pageable
  );
}
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
   */
  Optional<ImageLog>
  findByStoredFileNameAndUserId(
      String storedFileName,
      Long userId
  );

  /**
   * 관리자 검수 화면:
   * 특정 검수 상태 데이터 조회
   *
   * 최신 사용자 정정부터 표시합니다.
   */
  Page<ImageLog>
  findAllByReviewStatusOrderByUserCorrectedAtDesc(
      ImageReviewStatus reviewStatus,
      Pageable pageable
  );

  /**
   * 관리자 검수 화면:
   * 상태와 관계없이 사용자 정정이 존재하는
   * 전체 데이터를 조회합니다.
   */
  Page<ImageLog>
  findAllByUserCorrectedWasteItemIsNotNullOrderByUserCorrectedAtDesc(
      Pageable pageable
  );

  /**
   * 기존 관리자 대시보드 호환용입니다.
   */
  Page<ImageLog>
  findAllByReviewStatusOrderByCreatedAtAsc(
      ImageReviewStatus reviewStatus,
      Pageable pageable
  );
}
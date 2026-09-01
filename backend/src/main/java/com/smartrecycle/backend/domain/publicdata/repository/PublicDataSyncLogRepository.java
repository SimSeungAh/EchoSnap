package com.smartrecycle.backend.domain.publicdata.repository;

import com.smartrecycle.backend.domain.publicdata.entity.PublicDataSyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicDataSyncLogRepository
    extends JpaRepository<
    PublicDataSyncLog,
    Long
    > {

  /**
   * 관리자 공공데이터 화면:
   * 최신 동기화부터 조회
   */
  Page<PublicDataSyncLog>
  findAllByOrderByStartedAtDesc(
      Pageable pageable
  );

  /**
   * 관리자 대시보드:
   * 최근 동기화 이력
   */
  List<PublicDataSyncLog>
  findTop5ByOrderByStartedAtDesc();
}
package com.echosnap.backend.domain.image.repository;

import com.echosnap.backend.domain.image.entity.AiWasteItemMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiWasteItemMappingRepository
    extends JpaRepository<AiWasteItemMapping, Long> {

  /**
   * 서버 AI 결과의 modelName + label로
   * 활성화된 WasteItem 매핑을 조회합니다.
   */
  Optional<AiWasteItemMapping>
  findByModelNameIgnoreCaseAndModelLabelIgnoreCaseAndActiveTrue(
      String modelName,
      String modelLabel
  );

  /**
   * 동일 모델에서 같은 label이
   * 이미 등록되어 있는지 확인합니다.
   */
  boolean existsByModelNameIgnoreCaseAndModelLabelIgnoreCase(
      String modelName,
      String modelLabel
  );
}
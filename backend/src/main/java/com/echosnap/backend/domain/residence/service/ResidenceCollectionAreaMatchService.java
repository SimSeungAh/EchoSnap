package com.echosnap.backend.domain.residence.service;

import com.echosnap.backend.domain.collectionarea.entity.CollectionArea;
import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
import com.echosnap.backend.domain.collectionarea.repository.CollectionAreaRepository;
import com.echosnap.backend.domain.residence.entity.GeneralHousingType;
import com.echosnap.backend.domain.residence.entity.Residence;
import com.echosnap.backend.domain.residence.repository.ResidenceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 일반주택 Residence의 주소 정보를 기준으로
 * 행정안전부 생활쓰레기배출정보에서 동기화된 CollectionArea를
 * 자동으로 연결하는 Service입니다.
 *
 * 매칭 기준:
 *
 * 1. 시도 / 시군구
 * 2. 주거 형태 조건
 * 3. 행정동
 * 4. 법정동
 * 5. 시군구 전체
 *
 * 매칭이 확실한 경우에만 자동 연결하고,
 * 동일한 우선순위의 후보가 여러 개인 경우에는
 * 잘못된 일정 연결을 막기 위해 자동 확정하지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class ResidenceCollectionAreaMatchService {

  private static final Logger log =
      LoggerFactory.getLogger(
          ResidenceCollectionAreaMatchService.class
      );

  /**
   * 행정동 직접 매칭을 가장 높은 우선순위로 사용합니다.
   */
  private static final int ADMINISTRATIVE_DONG_SCORE =
      300;

  /**
   * 행정동 매칭이 불가능한 경우
   * 법정동을 두 번째 우선순위로 사용합니다.
   */
  private static final int LEGAL_DONG_SCORE =
      200;

  /**
   * 공공데이터가 시군구 전체 범위라고 명시한 경우
   * 가장 낮은 우선순위로 사용합니다.
   */
  private static final int WHOLE_REGION_SCORE =
      100;

  /**
   * 사용자의 주거 형태와 공공데이터의
   * 주거 형태 조건이 명확하게 일치하는 경우
   * 같은 주소 범위의 일반 후보보다 우선하도록
   * 추가 점수를 부여합니다.
   */
  private static final int HOUSING_QUALIFIER_BONUS =
      50;

  /**
   * 주거형태가 명확하게 맞지 않는 후보를
   * 표현하기 위한 내부 점수입니다.
   */
  private static final int HOUSING_INCOMPATIBLE =
      -1;

  /**
   * 숫자가 포함된 행정동을 분리하기 위한 패턴입니다.
   *
   * 예:
   *
   * 침산2동
   *
   * → base = 침산
   * → number = 2
   */
  private static final Pattern NUMBERED_DONG_PATTERN =
      Pattern.compile(
          "^(.+?)(\\d+)동$"
      );

  private final CollectionAreaRepository
      collectionAreaRepository;

  private final ResidenceRepository
      residenceRepository;

  /**
   * Residence에 적용 가능한 CollectionArea를
   * 폐기물 종류별로 찾아 자동 연결합니다.
   */
  @Transactional
  public void matchAndAssign(
      Residence residence
  ) {
    if (residence == null) {
      log.warn(
          "CollectionArea 매칭 생략: Residence가 null입니다."
      );
      return;
    }

    /*
     * 재매칭 시 이전 결과가 남아 있지 않도록
     * 기존 연결을 먼저 제거합니다.
     */
    residence.clearCollectionAreas();

    List<CollectionArea> candidates =
        collectionAreaRepository
            .findAllBySidoAndSigunguAndActiveTrue(
                residence.getSido(),
                residence.getSigungu()
            );

    log.info(
        "CollectionArea 매칭 시작. "
            + "residenceId={}, sido={}, sigungu={}, "
            + "administrativeDong={}, legalDong={}, "
            + "generalHousingType={}, candidateCount={}",
        residence.getId(),
        residence.getSido(),
        residence.getSigungu(),
        residence.getAdministrativeDong(),
        residence.getLegalDong(),
        residence.getGeneralHousingType(),
        candidates.size()
    );

    if (candidates.isEmpty()) {
      /*
       * 매칭 결과가 0건이어도
       * 기존 연결 제거 상태를 확실히 반영합니다.
       */
      residenceRepository.saveAndFlush(
          residence
      );

      log.warn(
          "CollectionArea 후보가 없습니다. "
              + "sido={}, sigungu={}",
          residence.getSido(),
          residence.getSigungu()
      );

      return;
    }

    /*
     * 생활쓰레기 / 음식물쓰레기 / 재활용품을
     * 각각 독립적으로 매칭합니다.
     */
    for (
        CollectionWasteType wasteType
        : CollectionWasteType.values()
    ) {
      Optional<CollectionArea> matchedArea =
          findSingleBestMatch(
              residence,
              candidates,
              wasteType
          );

      if (matchedArea.isPresent()) {
        CollectionArea collectionArea =
            matchedArea.get();

        residence.assignCollectionArea(
            collectionArea,
            wasteType
        );

        log.info(
            "CollectionArea 매칭 성공. "
                + "wasteType={}, areaId={}, areaName={}, "
                + "targetAreaName={}",
            wasteType,
            collectionArea.getId(),
            collectionArea.getAreaName(),
            collectionArea.getTargetAreaName()
        );

      } else {
        log.warn(
            "CollectionArea 매칭 실패. "
                + "wasteType={}, residenceId={}",
            wasteType,
            residence.getId()
        );
      }
    }

    /*
     * Residence는 User 조회 트랜잭션 안에서 보통 managed 상태지만,
     * 매핑 자식 엔티티의 INSERT를 개발 단계에서 명확히 보장하고
     * 바로 DB에서 확인할 수 있도록 saveAndFlush 합니다.
     *
     * OneToMany의 CascadeType.ALL / orphanRemoval 설정 때문에
     * 새 매핑 생성 및 기존 매핑 제거도 함께 반영됩니다.
     */
    residenceRepository.saveAndFlush(
        residence
    );

    log.info(
        "CollectionArea 매핑 저장 완료. "
            + "residenceId={}, mappingCount={}",
        residence.getId(),
        residence
            .getCollectionAreaMappings()
            .size()
    );
  }

  /**
   * 특정 폐기물 종류에 대해
   * 가장 적합한 수거구역 하나를 찾습니다.
   *
   * 최고 점수 후보가 2개 이상이면
   * 어느 후보가 맞는지 확실하지 않으므로
   * 자동 연결하지 않습니다.
   */
  private Optional<CollectionArea>
  findSingleBestMatch(
      Residence residence,
      List<CollectionArea> candidates,
      CollectionWasteType wasteType
  ) {
    CollectionArea bestArea = null;

    int bestScore = 0;

    boolean ambiguous = false;

    for (
        CollectionArea candidate
        : candidates
    ) {
      boolean supported =
          candidate.supports(
              wasteType
          );

      if (!supported) {
        log.info(
            "CollectionArea 후보 제외. "
                + "wasteType={}, areaId={}, areaName={}, "
                + "reason=unsupported",
            wasteType,
            candidate.getId(),
            candidate.getAreaName()
        );

        continue;
      }

      int score =
          calculateMatchScore(
              residence,
              candidate
          );

      log.info(
          "CollectionArea 후보 점수. "
              + "wasteType={}, areaId={}, areaName={}, "
              + "targetAreaName={}, score={}",
          wasteType,
          candidate.getId(),
          candidate.getAreaName(),
          candidate.getTargetAreaName(),
          score
      );

      if (score <= 0) {
        continue;
      }

      if (score > bestScore) {
        bestScore = score;
        bestArea = candidate;
        ambiguous = false;

        continue;
      }

      /*
       * 동일한 최고 점수 후보가 여러 개라면
       * 어느 후보가 정확한지 확정할 수 없습니다.
       */
      if (score == bestScore) {
        ambiguous = true;
      }
    }

    if (
        bestArea == null
            || ambiguous
    ) {
      return Optional.empty();
    }

    return Optional.of(
        bestArea
    );
  }

  /**
   * Residence와 CollectionArea의
   * 최종 매칭 점수를 계산합니다.
   *
   * 주소 점수:
   *
   * 행정동 = 300
   * 법정동 = 200
   * 시군구 전체 = 100
   *
   * 여기에 주거형태가 명확하게 일치하면
   * 50점을 추가합니다.
   */
  private int calculateMatchScore(
      Residence residence,
      CollectionArea collectionArea
  ) {
    String targetAreaName =
        collectionArea.getTargetAreaName();

    if (isBlank(targetAreaName)) {
      return 0;
    }

    int housingScore =
        calculateHousingQualifierScore(
            residence,
            targetAreaName
        );

    /*
     * 주거 형태가 명확하게 맞지 않는 후보는
     * 주소가 맞더라도 연결하지 않습니다.
     */
    if (
        housingScore
            == HOUSING_INCOMPATIBLE
    ) {
      return 0;
    }

    /*
     * 주소 매칭을 할 때는
     * "단독주택", "공동주택" 같은 표현을
     * 잠시 제거합니다.
     *
     * 예:
     *
     * 부전동(단독주택)
     * → 부전동()
     *
     * 단독주택-부전동
     * → -부전동
     */
    String addressTarget =
        removeHousingQualifierWords(
            targetAreaName
        );

    int addressScore = 0;

    if (
        matchesDong(
            addressTarget,
            residence.getAdministrativeDong()
        )
    ) {
      addressScore =
          ADMINISTRATIVE_DONG_SCORE;

    } else if (
        matchesDong(
            addressTarget,
            residence.getLegalDong()
        )
    ) {
      addressScore =
          LEGAL_DONG_SCORE;

    } else if (
        isWholeRegionTarget(
            addressTarget,
            residence.getSigungu()
        )
    ) {
      addressScore =
          WHOLE_REGION_SCORE;
    }

    if (addressScore <= 0) {
      return 0;
    }

    return addressScore
        + housingScore;
  }

  /**
   * 공공데이터 대상지역에 포함된
   * 주거 형태 조건이 현재 Residence와
   * 호환되는지 확인합니다.
   *
   * 반환값:
   *
   * -1 : 명확하게 불일치
   *  0 : 별도 주거형태 조건 없음
   * 50 : 명확하게 일치
   */
  private int calculateHousingQualifierScore(
      Residence residence,
      String targetAreaName
  ) {
    if (
        !containsHousingQualifier(
            targetAreaName
        )
    ) {
      return 0;
    }

    GeneralHousingType housingType =
        residence.getGeneralHousingType();

    /*
     * 기존 Residence 데이터처럼
     * 세부 주거형태가 아직 없는 경우에는
     * 주거형태 조건이 붙은 데이터를
     * 추측해서 연결하지 않습니다.
     */
    if (housingType == null) {
      return HOUSING_INCOMPATIBLE;
    }

    String normalized =
        removeWhitespace(
            targetAreaName
        );

    if (normalized == null) {
      return HOUSING_INCOMPATIBLE;
    }

    return switch (housingType) {

      /*
       * 단독주택
       */
      case DETACHED_HOUSE ->
          evaluateDetachedHouse(
              normalized
          );

      /*
       * 다가구주택
       *
       * 행정상 단독주택 계열이기 때문에
       * "단독주택"이라는 포괄 조건도 허용합니다.
       */
      case MULTI_FAMILY_HOUSE ->
          evaluateMultiFamilyHouse(
              normalized
          );

      /*
       * 연립주택
       *
       * 일반적인 "공동주택" 조건과
       * 명시적인 "연립주택" 조건을 허용합니다.
       */
      case ROW_HOUSE ->
          evaluateRowHouse(
              normalized
          );

      /*
       * 다세대주택
       *
       * 일반적인 "공동주택" 조건과
       * 명시적인 "다세대주택" 조건을 허용합니다.
       */
      case MULTI_UNIT_HOUSE ->
          evaluateMultiUnitHouse(
              normalized
          );
    };
  }

  /**
   * 단독주택 사용자와
   * 공공데이터의 주거 형태 조건을 비교합니다.
   */
  private int evaluateDetachedHouse(
      String value
  ) {
    if (
        isExcluded(
            value,
            "단독주택"
        )
    ) {
      return HOUSING_INCOMPATIBLE;
    }

    if (
        hasPositiveQualifier(
            value,
            "다가구주택"
        )
    ) {
      /*
       * "다가구주택"이라고 정확히 한정된 경우
       * 일반 단독주택에는 연결하지 않습니다.
       */
      return HOUSING_INCOMPATIBLE;
    }

    if (
        hasPositiveQualifier(
            value,
            "단독주택"
        )
    ) {
      return HOUSING_QUALIFIER_BONUS;
    }

    /*
     * 공동주택을 제외한다고 명시된 경우는
     * 단독주택 계열과 호환되는 조건으로 봅니다.
     */
    if (
        isExcluded(
            value,
            "공동주택"
        )
    ) {
      return HOUSING_QUALIFIER_BONUS;
    }

    if (
        containsPositiveSharedQualifier(
            value
        )
            || containsPositiveManagedQualifier(
            value
        )
    ) {
      return HOUSING_INCOMPATIBLE;
    }

    return 0;
  }

  /**
   * 다가구주택 사용자의 조건을 판별합니다.
   */
  private int evaluateMultiFamilyHouse(
      String value
  ) {
    if (
        isExcluded(
            value,
            "단독주택"
        )
            || isExcluded(
            value,
            "다가구주택"
        )
    ) {
      return HOUSING_INCOMPATIBLE;
    }

    if (
        hasPositiveQualifier(
            value,
            "다가구주택"
        )
            || hasPositiveQualifier(
            value,
            "단독주택"
        )
    ) {
      return HOUSING_QUALIFIER_BONUS;
    }

    if (
        isExcluded(
            value,
            "공동주택"
        )
    ) {
      return HOUSING_QUALIFIER_BONUS;
    }

    if (
        containsPositiveSharedQualifier(
            value
        )
            || containsPositiveManagedQualifier(
            value
        )
    ) {
      return HOUSING_INCOMPATIBLE;
    }

    return 0;
  }

  /**
   * 연립주택 사용자의 조건을 판별합니다.
   */
  private int evaluateRowHouse(
      String value
  ) {
    if (
        isExcluded(
            value,
            "공동주택"
        )
            || isExcluded(
            value,
            "연립주택"
        )
    ) {
      return HOUSING_INCOMPATIBLE;
    }

    /*
     * 다른 일반 공동주택 세부 유형인
     * 다세대주택으로만 한정된 데이터는 제외합니다.
     */
    if (
        hasPositiveQualifier(
            value,
            "다세대주택"
        )
            && !hasPositiveQualifier(
            value,
            "연립주택"
        )
            && !hasPositiveQualifier(
            value,
            "공동주택"
        )
    ) {
      return HOUSING_INCOMPATIBLE;
    }

    if (
        hasPositiveQualifier(
            value,
            "연립주택"
        )
            || hasPositiveQualifier(
            value,
            "공동주택"
        )
    ) {
      /*
       * "아파트" 또는 "오피스텔"로만
       * 구체화된 공동주택 데이터라면
       * 일반주택인 연립주택에 연결하지 않습니다.
       */
      if (
          containsPositiveManagedQualifier(
              value
          )
              && !hasPositiveQualifier(
              value,
              "연립주택"
          )
      ) {
        return HOUSING_INCOMPATIBLE;
      }

      return HOUSING_QUALIFIER_BONUS;
    }

    /*
     * 단독주택 제외라고 명시된
     * 공동주택 계열 데이터도 허용합니다.
     */
    if (
        isExcluded(
            value,
            "단독주택"
        )
    ) {
      return HOUSING_QUALIFIER_BONUS;
    }

    if (
        containsPositiveDetachedQualifier(
            value
        )
            || containsPositiveManagedQualifier(
            value
        )
    ) {
      return HOUSING_INCOMPATIBLE;
    }

    return 0;
  }

  /**
   * 다세대주택 사용자의 조건을 판별합니다.
   */
  private int evaluateMultiUnitHouse(
      String value
  ) {
    if (
        isExcluded(
            value,
            "공동주택"
        )
            || isExcluded(
            value,
            "다세대주택"
        )
    ) {
      return HOUSING_INCOMPATIBLE;
    }

    /*
     * 연립주택으로만 한정된 데이터는
     * 다세대주택에 연결하지 않습니다.
     */
    if (
        hasPositiveQualifier(
            value,
            "연립주택"
        )
            && !hasPositiveQualifier(
            value,
            "다세대주택"
        )
            && !hasPositiveQualifier(
            value,
            "공동주택"
        )
    ) {
      return HOUSING_INCOMPATIBLE;
    }

    if (
        hasPositiveQualifier(
            value,
            "다세대주택"
        )
            || hasPositiveQualifier(
            value,
            "공동주택"
        )
    ) {
      if (
          containsPositiveManagedQualifier(
              value
          )
              && !hasPositiveQualifier(
              value,
              "다세대주택"
          )
      ) {
        return HOUSING_INCOMPATIBLE;
      }

      return HOUSING_QUALIFIER_BONUS;
    }

    if (
        isExcluded(
            value,
            "단독주택"
        )
    ) {
      return HOUSING_QUALIFIER_BONUS;
    }

    if (
        containsPositiveDetachedQualifier(
            value
        )
            || containsPositiveManagedQualifier(
            value
        )
    ) {
      return HOUSING_INCOMPATIBLE;
    }

    return 0;
  }

  /**
   * "단독주택", "다가구주택" 중
   * 실제 적용 대상으로 언급된 조건이 있는지 확인합니다.
   */
  private boolean containsPositiveDetachedQualifier(
      String value
  ) {
    return hasPositiveQualifier(
        value,
        "단독주택"
    )
        || hasPositiveQualifier(
        value,
        "다가구주택"
    );
  }

  /**
   * 일반주택의 공동주택 계열 조건입니다.
   *
   * 연립 / 다세대뿐 아니라
   * 공공데이터의 보다 넓은 표현인
   * "공동주택"도 포함합니다.
   */
  private boolean containsPositiveSharedQualifier(
      String value
  ) {
    return hasPositiveQualifier(
        value,
        "공동주택"
    )
        || hasPositiveQualifier(
        value,
        "연립주택"
    )
        || hasPositiveQualifier(
        value,
        "다세대주택"
    );
  }

  /**
   * EchoSnap에서 MANAGED_COMPLEX로 처리하는
   * 명확한 관리형 주거 조건입니다.
   *
   * 일반주택 Residence에는 자동 연결하지 않습니다.
   */
  private boolean containsPositiveManagedQualifier(
      String value
  ) {
    return hasPositiveQualifier(
        value,
        "아파트"
    )
        || hasPositiveQualifier(
        value,
        "오피스텔"
    );
  }

  /**
   * 특정 주거 형태가
   * "제외" 조건이 아닌 실제 적용 조건인지 확인합니다.
   *
   * 예:
   *
   * 단독주택
   * → true
   *
   * 단독주택제외
   * → false
   */
  private boolean hasPositiveQualifier(
      String normalizedValue,
      String qualifier
  ) {
    if (
        normalizedValue == null
            || qualifier == null
    ) {
      return false;
    }

    if (
        !normalizedValue.contains(
            qualifier
        )
    ) {
      return false;
    }

    return !isExcluded(
        normalizedValue,
        qualifier
    );
  }

  /**
   * "단독주택 제외"와 같은 표현을 판별합니다.
   *
   * calculateHousingQualifierScore 호출 전에
   * 모든 공백을 제거하기 때문에
   *
   * 단독주택 제외
   * 단독주택제외
   *
   * 둘 다 같은 방식으로 처리됩니다.
   */
  private boolean isExcluded(
      String normalizedValue,
      String qualifier
  ) {
    if (
        normalizedValue == null
            || qualifier == null
    ) {
      return false;
    }

    return normalizedValue.contains(
        qualifier + "제외"
    );
  }

  /**
   * 공공데이터 대상지역 문자열에
   * 해당 동이 명확하게 포함되어 있는지 확인합니다.
   *
   * 지원 예:
   *
   * 산격1동+산격2동
   * 관문동(매천)
   * (구)대현1동
   * 침산1동~3동
   * 침산1동~침산3동
   */
  private boolean matchesDong(
      String targetAreaName,
      String dong
  ) {
    String normalizedTarget =
        removeWhitespace(
            targetAreaName
        );

    String normalizedDong =
        removeWhitespace(
            dong
        );

    if (
        normalizedTarget == null
            || normalizedDong == null
    ) {
      return false;
    }

    /*
     * 침산1동~3동 같은 숫자 범위를 먼저 확인합니다.
     */
    if (
        matchesNumberedDongRange(
            normalizedTarget,
            normalizedDong
        )
    ) {
      return true;
    }

    /*
     * 부산진구 공공데이터처럼
     *
     * 부전1·2동
     * 부암1·3동
     *
     * 형태로 여러 행정동 번호를 축약한 표현을 처리합니다.
     *
     * "부암1·3동"은 1~3동이라는 범위가 아니라
     * 1동과 3동을 각각 의미하므로
     * 범위 로직과 별도로 처리합니다.
     */
    if (
        matchesNumberedDongList(
            normalizedTarget,
            normalizedDong
        )
    ) {
      return true;
    }

    /*
     * + , / · 등의 구분자를 기준으로
     * 대상지역 표현을 분리합니다.
     */
    String[] tokens =
        normalizedTarget.split(
            "[+,/·ㆍ;|]"
        );

    for (String token : tokens) {

      String cleanedToken =
          stripLeadingParenthetical(
              token
          );

      cleanedToken =
          stripEdgeSeparators(
              cleanedToken
          );

      if (cleanedToken == null) {
        continue;
      }

      /*
       * 산격2동
       */
      if (
          cleanedToken.equals(
              normalizedDong
          )
      ) {
        return true;
      }

      /*
       * 관문동(매천)
       * 산격2동(일부)
       */
      if (
          cleanedToken.startsWith(
              normalizedDong + "("
          )
      ) {
        return true;
      }

      /*
       * 대괄호 설명이 붙는 데이터도 대응합니다.
       */
      if (
          cleanedToken.startsWith(
              normalizedDong + "["
          )
      ) {
        return true;
      }
    }

    return false;
  }

  /**
   * 같은 이름의 숫자 행정동 여러 개를
   * 가운데점 등으로 축약해서 작성한 표현을 처리합니다.
   *
   * 예:
   *
   * Residence = 부전2동
   *
   * 공공데이터:
   * 부전1·2동
   *
   * → true
   *
   * Residence = 부암2동
   *
   * 공공데이터:
   * 부암1·3동
   *
   * → false
   *
   * 즉 숫자 사이를 범위로 해석하지 않고,
   * 실제 명시된 번호만 비교합니다.
   */
  private boolean matchesNumberedDongList(
      String targetAreaName,
      String dong
  ) {
    Matcher dongMatcher =
        NUMBERED_DONG_PATTERN.matcher(
            dong
        );

    if (!dongMatcher.matches()) {
      return false;
    }

    String baseName =
        dongMatcher.group(1);

    int targetDongNumber;

    try {
      targetDongNumber =
          Integer.parseInt(
              dongMatcher.group(2)
          );
    } catch (NumberFormatException e) {
      return false;
    }

    /*
     * 지원 예:
     *
     * 부전1·2동
     * 부암1·3동
     * 양정1ㆍ2동
     * 가야1,2동
     *
     * 첫 번째 그룹에는
     * "1·2", "1·3" 같은 숫자 목록이 들어옵니다.
     */
    Pattern compactListPattern =
        Pattern.compile(
            Pattern.quote(baseName)
                + "((?:\\d+[·ㆍ,])+\\d+)동"
        );

    Matcher matcher =
        compactListPattern.matcher(
            targetAreaName
        );

    while (matcher.find()) {
      String numberList =
          matcher.group(1);

      String[] numbers =
          numberList.split(
              "[·ㆍ,]"
          );

      for (String number : numbers) {
        try {
          int candidateNumber =
              Integer.parseInt(
                  number
              );

          if (
              candidateNumber
                  == targetDongNumber
          ) {
            return true;
          }

        } catch (NumberFormatException ignored) {
          /*
           * 이상한 표현 하나 때문에
           * 전체 주소 매칭을 실패시키지 않습니다.
           */
        }
      }
    }

    return false;
  }

  /**
   * 숫자가 붙은 동의 범위 표현을 처리합니다.
   *
   * 예:
   *
   * Residence = 침산2동
   *
   * 공공데이터:
   * 침산1동~3동
   *
   * → 매칭 성공
   */
  private boolean matchesNumberedDongRange(
      String targetAreaName,
      String dong
  ) {
    Matcher dongMatcher =
        NUMBERED_DONG_PATTERN.matcher(
            dong
        );

    if (!dongMatcher.matches()) {
      return false;
    }

    String baseName =
        dongMatcher.group(1);

    int dongNumber;

    try {
      dongNumber =
          Integer.parseInt(
              dongMatcher.group(2)
          );
    } catch (NumberFormatException e) {
      return false;
    }

    /*
     * 침산1동~3동
     * 침산1동~침산3동
     */
    Pattern fullDongRangePattern =
        Pattern.compile(
            Pattern.quote(baseName)
                + "(\\d+)동"
                + "[~∼～-]"
                + "(?:"
                + Pattern.quote(baseName)
                + ")?"
                + "(\\d+)동"
        );

    if (
        isNumberInsideRange(
            fullDongRangePattern,
            targetAreaName,
            dongNumber
        )
    ) {
      return true;
    }

    /*
     * 침산1~3동처럼 작성된 경우도 대응합니다.
     */
    Pattern shortDongRangePattern =
        Pattern.compile(
            Pattern.quote(baseName)
                + "(\\d+)"
                + "[~∼～-]"
                + "(?:"
                + Pattern.quote(baseName)
                + ")?"
                + "(\\d+)동"
        );

    return isNumberInsideRange(
        shortDongRangePattern,
        targetAreaName,
        dongNumber
    );
  }

  /**
   * 정규식에서 찾은 시작 번호와 끝 번호 사이에
   * 현재 Residence의 동 번호가 포함되는지 확인합니다.
   */
  private boolean isNumberInsideRange(
      Pattern rangePattern,
      String targetAreaName,
      int dongNumber
  ) {
    Matcher matcher =
        rangePattern.matcher(
            targetAreaName
        );

    while (matcher.find()) {
      try {
        int start =
            Integer.parseInt(
                matcher.group(1)
            );

        int end =
            Integer.parseInt(
                matcher.group(2)
            );

        int min =
            Math.min(
                start,
                end
            );

        int max =
            Math.max(
                start,
                end
            );

        if (
            dongNumber >= min
                && dongNumber <= max
        ) {
          return true;
        }

      } catch (NumberFormatException ignored) {
        /*
         * 잘못된 범위 표현 하나 때문에
         * 전체 매칭이 실패하지 않도록
         * 해당 표현만 건너뜁니다.
         */
      }
    }

    return false;
  }

  /**
   * 공공데이터 대상지역이
   * 시군구 전체를 의미하는지 확인합니다.
   *
   * 주거형태 단어를 제거한 뒤
   * 괄호 등의 추가 설명이 남아 있을 수 있으므로
   * startsWith도 함께 사용합니다.
   */
  private boolean isWholeRegionTarget(
      String targetAreaName,
      String sigungu
  ) {
    String target =
        removeWhitespace(
            targetAreaName
        );

    String region =
        removeWhitespace(
            sigungu
        );

    if (
        target == null
            || region == null
    ) {
      return false;
    }

    target =
        stripEdgeSeparators(
            target
        );

    if (target == null) {
      return false;
    }

    return target.equals(
        region + "전체"
    )
        || target.startsWith(
        region + "전체("
    )
        || target.equals(
        region + "전지역"
    )
        || target.startsWith(
        region + "전지역("
    )
        || target.equals("전체")
        || target.startsWith(
        "전체("
    )
        || target.equals("전지역")
        || target.startsWith(
        "전지역("
    );
  }

  /**
   * 공공데이터 대상지역에
   * 주거 형태 조건이 들어 있는지 확인합니다.
   */
  private boolean containsHousingQualifier(
      String value
  ) {
    String normalized =
        removeWhitespace(
            value
        );

    if (normalized == null) {
      return false;
    }

    return normalized.contains(
        "단독주택"
    )
        || normalized.contains(
        "다가구주택"
    )
        || normalized.contains(
        "공동주택"
    )
        || normalized.contains(
        "연립주택"
    )
        || normalized.contains(
        "다세대주택"
    )
        || normalized.contains(
        "아파트"
    )
        || normalized.contains(
        "오피스텔"
    );
  }

  /**
   * 주소 매칭에 방해가 되는
   * 주거 형태 단어만 제거합니다.
   *
   * 주거형태 적합성 판단은 이 메서드 호출 전에
   * 원본 문자열로 이미 완료합니다.
   */
  private String removeHousingQualifierWords(
      String value
  ) {
    if (value == null) {
      return null;
    }

    return value
        .replace(
            "단독주택",
            ""
        )
        .replace(
            "다가구주택",
            ""
        )
        .replace(
            "공동주택",
            ""
        )
        .replace(
            "연립주택",
            ""
        )
        .replace(
            "다세대주택",
            ""
        )
        .replace(
            "아파트",
            ""
        )
        .replace(
            "오피스텔",
            ""
        );
  }

  /**
   * "(구)대현1동"처럼
   * 앞에 설명용 괄호가 붙은 경우 제거합니다.
   */
  private String stripLeadingParenthetical(
      String value
  ) {
    if (value == null) {
      return null;
    }

    String result =
        value;

    while (result.startsWith("(")) {
      int closingIndex =
          result.indexOf(")");

      if (closingIndex < 0) {
        break;
      }

      result =
          result.substring(
              closingIndex + 1
          );
    }

    return result.isEmpty()
        ? null
        : result;
  }

  /**
   * 주거 형태 단어를 제거한 뒤 남을 수 있는
   * 앞뒤 구분 기호를 제거합니다.
   *
   * 예:
   *
   * -부전동
   * → 부전동
   */
  private String stripEdgeSeparators(
      String value
  ) {
    if (value == null) {
      return null;
    }

    String result =
        value.replaceAll(
            "^[\\-:：·ㆍ_()\\[\\]{}]+",
            ""
        );

    result =
        result.replaceAll(
            "[\\-:：·ㆍ_()\\[\\]{}]+$",
            ""
        );

    return result.isEmpty()
        ? null
        : result;
  }

  /**
   * 주소 비교 시 공백 차이 때문에
   * 매칭이 실패하지 않도록 모든 공백을 제거합니다.
   */
  private String removeWhitespace(
      String value
  ) {
    if (isBlank(value)) {
      return null;
    }

    return value.replaceAll(
        "\\s+",
        ""
    );
  }

  private boolean isBlank(
      String value
  ) {
    return value == null
        || value.isBlank();
  }
}

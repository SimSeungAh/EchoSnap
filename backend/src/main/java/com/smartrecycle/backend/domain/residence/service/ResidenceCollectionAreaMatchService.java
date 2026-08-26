package com.smartrecycle.backend.domain.residence.service;

import com.smartrecycle.backend.domain.collectionarea.entity.CollectionArea;
import com.smartrecycle.backend.domain.collectionarea.entity.CollectionWasteType;
import com.smartrecycle.backend.domain.collectionarea.repository.CollectionAreaRepository;
import com.smartrecycle.backend.domain.residence.entity.Residence;
import lombok.RequiredArgsConstructor;
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
 * 매칭이 확실한 경우에만 자동 연결하고,
 * 동일한 우선순위의 후보가 여러 개인 경우에는
 * 잘못된 일정 연결을 막기 위해 자동 확정하지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class ResidenceCollectionAreaMatchService {

  /**
   * 행정동 직접 매칭을 가장 높은 우선순위로 사용합니다.
   */
  private static final int ADMINISTRATIVE_DONG_SCORE = 300;

  /**
   * 행정동 매칭이 불가능한 경우
   * 법정동을 두 번째 우선순위로 사용합니다.
   */
  private static final int LEGAL_DONG_SCORE = 200;

  /**
   * 공공데이터가 시군구 전체 범위라고 명시한 경우
   * 가장 낮은 우선순위로 사용합니다.
   */
  private static final int WHOLE_REGION_SCORE = 100;

  /**
   * 숫자가 포함된 행정동을 분리하기 위한 패턴입니다.
   *
   * 예:
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

  /**
   * Residence에 적용 가능한 CollectionArea를
   * 폐기물 종류별로 찾아 자동 연결합니다.
   */
  @Transactional
  public void matchAndAssign(
      Residence residence
  ) {
    if (residence == null) {
      return;
    }

    /*
     * 재매칭 시 기존 결과가 남아 있지 않도록
     * 먼저 기존 수거구역 연결을 제거합니다.
     */
    residence.clearCollectionAreas();

    List<CollectionArea> candidates =
        collectionAreaRepository
            .findAllBySidoAndSigunguAndActiveTrue(
                residence.getSido(),
                residence.getSigungu()
            );

    if (candidates.isEmpty()) {
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

      matchedArea.ifPresent(
          collectionArea ->
              residence.assignCollectionArea(
                  collectionArea,
                  wasteType
              )
      );
    }
  }

  /**
   * 특정 폐기물 종류에 대해
   * 가장 적합한 수거구역 하나를 찾습니다.
   *
   * 최고 점수 후보가 2개 이상이면
   * 어느 쪽이 맞는지 확실하지 않으므로
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
      if (!candidate.supports(wasteType)) {
        continue;
      }

      int score =
          calculateMatchScore(
              residence,
              candidate
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
       * 자동으로 어느 하나를 선택하지 않습니다.
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
   * 주소 매칭 점수를 계산합니다.
   *
   * 우선순위:
   *
   * 1. 행정동
   * 2. 법정동
   * 3. 시군구 전체
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

    /*
     * 공동주택/단독주택 같은 주거형태 조건이
     * 공공데이터 문자열에 들어간 경우
     * 현재 Residence만으로는 정확히 판별할 수 없습니다.
     *
     * 잘못된 일정 자동 연결을 막기 위해
     * 현재 단계에서는 자동 매칭 대상에서 제외합니다.
     */
    if (
        containsHousingQualifier(
            targetAreaName
        )
    ) {
      return 0;
    }

    if (
        matchesDong(
            targetAreaName,
            residence.getAdministrativeDong()
        )
    ) {
      return ADMINISTRATIVE_DONG_SCORE;
    }

    if (
        matchesDong(
            targetAreaName,
            residence.getLegalDong()
        )
    ) {
      return LEGAL_DONG_SCORE;
    }

    if (
        isWholeRegionTarget(
            targetAreaName,
            residence.getSigungu()
        )
    ) {
      return WHOLE_REGION_SCORE;
    }

    return 0;
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
       * 대괄호 설명이 붙는 데이터도
       * 안전하게 대응합니다.
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
     * 일부 지자체가
     * 침산1~3동처럼 작성하는 경우도 대응합니다.
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

    return target.equals(
        region + "전체"
    )
        || target.equals(
        region + "전지역"
    )
        || target.equals("전체")
        || target.equals("전지역");
  }

  /**
   * 주거형태 조건이 포함된 데이터는
   * 현재 Residence 정보만으로 확정하기 어렵기 때문에
   * 자동 매칭에서 제외합니다.
   */
  private boolean containsHousingQualifier(
      String value
  ) {
    if (value == null) {
      return false;
    }

    String normalized =
        removeWhitespace(
            value
        );

    return normalized != null
        && (
        normalized.contains(
            "공동주택"
        )
            || normalized.contains(
            "단독주택"
        )
            || normalized.contains(
            "아파트"
        )
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
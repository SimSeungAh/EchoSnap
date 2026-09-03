package com.echosnap.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * EchoSnap에서 AI가 직접 인식하지 않더라도
 * 사용자가 검색을 통해 배출 방법을 확인할 수 있어야 하는
 * 폐전지 / 소형 전자제품 품목을 local DB에 준비합니다.
 *
 * 현재 AI 모델은 대표 재활용품 6종을 중심으로 동작하고,
 * 아래 품목들은 직접 검색 fallback을 통해 안내합니다.
 *
 * 이 초기화 클래스는 기존 LocalWasteSeedInitializer와 분리해서
 * 기존 기본 품목 시드를 수정하지 않고 확장 품목만 관리합니다.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalSpecialWasteSeedInitializer implements CommandLineRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(String... args) {
    seedCategories();
    seedItems();
    seedGuides();
  }

  /**
   * 폐전지와 소형 전자제품은
   * 일반 재활용품과 배출 체계가 다르므로
   * 별도 카테고리로 관리합니다.
   */
  private void seedCategories() {

    insertCategory(
        "BATTERY",
        "폐전지류",
        "일회용 건전지, 충전식 배터리, 보조배터리 등 별도 수거가 필요한 전지류",
        8
    );

    insertCategory(
        "SMALL_ELECTRONICS",
        "소형 폐가전·전자제품",
        "마우스, 키보드, 충전기, 케이블, 이어폰 등 별도 수거가 필요한 소형 전자제품",
        9
    );
  }

  private void seedItems() {

    /*
     * =========================================================
     * 폐전지류
     * =========================================================
     */

    insertItem(
        "폐건전지",
        "건전지 AA AAA 알카라인 망간 배터리 battery dry cell",
        "BATTERY"
    );

    insertItem(
        "충전식 배터리·보조배터리",
        "충전지 리튬이온 리튬 배터리 보조배터리 power bank rechargeable battery",
        "BATTERY"
    );

    /*
     * =========================================================
     * 소형 폐가전·전자제품
     * =========================================================
     */

    insertItem(
        "전선·케이블",
        "전선 케이블 USB선 USB케이블 충전선 HDMI LAN 랜선 cable wire",
        "SMALL_ELECTRONICS"
    );

    insertItem(
        "마우스",
        "마우스 컴퓨터마우스 무선마우스 유선마우스 mouse",
        "SMALL_ELECTRONICS"
    );

    insertItem(
        "키보드",
        "키보드 컴퓨터키보드 무선키보드 유선키보드 keyboard",
        "SMALL_ELECTRONICS"
    );

    insertItem(
        "이어폰·헤드폰",
        "이어폰 헤드폰 헤드셋 블루투스이어폰 무선이어폰 earphone headphone headset",
        "SMALL_ELECTRONICS"
    );

    insertItem(
        "충전기·어댑터",
        "충전기 어댑터 전원어댑터 USB충전기 charger adapter",
        "SMALL_ELECTRONICS"
    );

    insertItem(
        "휴대폰",
        "휴대폰 스마트폰 핸드폰 mobile phone smartphone",
        "SMALL_ELECTRONICS"
    );

    insertItem(
        "소형 폐가전",
        "소형가전 폐가전 전자제품 전기제품 small appliance electronics",
        "SMALL_ELECTRONICS"
    );
  }

  /**
   * 폐전지류와 소형 폐가전은 지역마다
   * 실제 수거 장소 및 방식이 달라질 수 있으므로,
   *
   * "일반쓰레기에 버리세요"처럼 단정하지 않고
   * 전용 수거함 / 소형 폐가전 수거처 / 지자체 기준을
   * 확인하도록 안내합니다.
   */
  private void seedGuides() {

    seedGuide(
        "폐건전지",
        "일반 종량제 봉투에 넣지 말고 폐건전지 전용 수거함이나 지정 수거처에 배출해요.",
        "사용이 끝난 일반 건전지는 공동주택, 주민센터, 행정복지센터 또는 지역에서 운영하는 폐건전지 전용 수거함 등 지정된 장소에 배출합니다. 실제 수거 위치와 운영 방식은 거주 지역의 안내를 함께 확인해주세요.",
        "건전지를 일반 종량제 봉투나 일반 재활용품에 섞어 배출하지 마세요. 누액이 있거나 파손된 전지는 맨손으로 내용물을 만지지 말고 지역의 안전한 배출 방법을 확인해주세요.",
        List.of(
            new GuideCheckSeed(
                "일반쓰레기나 일반 재활용품에 섞지 않았나요?",
                0,
                true
            ),
            new GuideCheckSeed(
                "폐건전지 전용 수거함 또는 지정 수거처를 확인했나요?",
                1,
                true
            ),
            new GuideCheckSeed(
                "누액이나 파손 여부를 확인했나요?",
                2,
                false
            )
        )
    );

    seedGuide(
        "충전식 배터리·보조배터리",
        "충전식 배터리와 보조배터리는 일반쓰레기에 버리지 말고 지정된 배터리 수거처를 이용해요.",
        "리튬이온 배터리, 충전식 배터리, 보조배터리 등은 일반 종량제 봉투나 일반 재활용품에 넣지 않고 지역에서 안내하는 폐전지 또는 소형 폐가전 수거처에 배출합니다. 분리 가능한 배터리라면 제품에서 분리한 뒤 해당 배터리의 수거 기준을 확인해주세요.",
        "리튬 계열 배터리는 눌림, 충격, 천공, 열 등에 의해 화재 위험이 있을 수 있습니다. 부풀었거나 파손된 배터리는 임의로 분해하지 말고 지역 또는 제조사의 안전한 회수 방법을 확인해주세요.",
        List.of(
            new GuideCheckSeed(
                "일반쓰레기에 넣지 않았나요?",
                0,
                true
            ),
            new GuideCheckSeed(
                "배터리가 부풀거나 파손되지 않았는지 확인했나요?",
                1,
                true
            ),
            new GuideCheckSeed(
                "지역의 지정 배터리 수거처를 확인했나요?",
                2,
                true
            )
        )
    );

    seedGuide(
        "전선·케이블",
        "전선과 케이블은 일반 재활용품에 섞기보다 지역의 소형 폐가전 수거 기준을 확인해 배출해요.",
        "USB 케이블, 충전선, 전원선 등은 금속과 플라스틱이 결합된 복합 제품이므로 임의로 피복을 벗겨 재질별로 나누기보다 지역의 소형 폐가전 또는 전자제품 수거 기준에 따라 배출하는 것이 좋습니다.",
        "전선 내부의 금속을 분리하기 위해 태우거나 위험한 도구로 절단하지 마세요. 제품과 연결된 배터리가 있다면 배터리는 별도 기준을 확인해주세요.",
        List.of(
            new GuideCheckSeed(
                "연결된 전원이나 기기에서 케이블을 분리했나요?",
                0,
                true
            ),
            new GuideCheckSeed(
                "배터리가 함께 붙어 있는 제품은 아닌지 확인했나요?",
                1,
                false
            ),
            new GuideCheckSeed(
                "지역의 소형 폐가전 수거 방법을 확인했나요?",
                2,
                true
            )
        )
    );

    seedGuide(
        "마우스",
        "마우스는 일반 재활용품에 섞지 말고 소형 폐가전 수거 기준에 맞게 배출해요.",
        "유선 또는 무선 마우스는 플라스틱, 금속, 전자회로 등이 결합된 전자제품입니다. 지역에서 운영하는 소형 폐가전 수거함이나 폐전자제품 수거 기준을 확인한 뒤 배출해주세요.",
        "무선 마우스에 건전지나 충전식 배터리가 들어 있다면 가능한 경우 먼저 분리하고, 배터리는 폐전지 기준에 따라 별도로 배출해주세요.",
        List.of(
            new GuideCheckSeed(
                "제품 안에 건전지나 배터리가 있는지 확인했나요?",
                0,
                true
            ),
            new GuideCheckSeed(
                "분리 가능한 배터리는 별도로 분리했나요?",
                1,
                true
            ),
            new GuideCheckSeed(
                "소형 폐가전 수거처를 확인했나요?",
                2,
                true
            )
        )
    );

    seedGuide(
        "키보드",
        "키보드는 지역의 소형 폐가전 또는 폐전자제품 수거 기준에 따라 배출해요.",
        "유선 또는 무선 키보드는 여러 재질과 전자 부품이 결합된 제품이므로 일반 플라스틱이나 금속류에 임의로 섞지 않고 지역에서 운영하는 소형 폐가전 수거 방식에 따라 배출합니다.",
        "무선 키보드에 건전지나 충전식 배터리가 있다면 가능한 경우 배터리를 먼저 분리하고 폐전지 수거 기준에 맞게 별도로 배출해주세요.",
        List.of(
            new GuideCheckSeed(
                "건전지나 배터리가 들어 있는지 확인했나요?",
                0,
                true
            ),
            new GuideCheckSeed(
                "분리 가능한 배터리를 제거했나요?",
                1,
                true
            ),
            new GuideCheckSeed(
                "지역의 소형 폐가전 수거 기준을 확인했나요?",
                2,
                true
            )
        )
    );

    seedGuide(
        "이어폰·헤드폰",
        "이어폰과 헤드폰은 소형 전자제품 수거 기준을 확인해 배출해요.",
        "유선 이어폰, 헤드폰, 헤드셋 등은 플라스틱, 금속, 전선 등이 결합된 전자제품이므로 지역의 소형 폐가전 또는 폐전자제품 수거 기준에 맞게 배출해주세요.",
        "무선 이어폰과 무선 헤드폰에는 충전식 배터리가 포함되어 있을 수 있습니다. 배터리가 내장된 제품을 억지로 분해하지 말고 제품 단위의 폐전자제품 회수 방법을 확인해주세요.",
        List.of(
            new GuideCheckSeed(
                "유선 제품인지 배터리가 내장된 무선 제품인지 확인했나요?",
                0,
                true
            ),
            new GuideCheckSeed(
                "배터리 내장 제품을 임의로 분해하지 않았나요?",
                1,
                true
            ),
            new GuideCheckSeed(
                "소형 폐가전 수거 방법을 확인했나요?",
                2,
                true
            )
        )
    );

    seedGuide(
        "충전기·어댑터",
        "충전기와 전원 어댑터는 소형 폐가전 수거 기준에 맞게 배출해요.",
        "휴대폰 충전기, USB 충전기, 전원 어댑터 등은 플라스틱과 금속, 전자회로가 결합된 제품이므로 일반 재활용품에 섞지 않고 지역의 소형 폐가전 또는 폐전자제품 수거 방식에 따라 배출해주세요.",
        "충전기를 임의로 분해하거나 내부 부품을 꺼내지 마세요. 연결된 케이블이 분리되는 제품이라면 케이블도 지역의 전자제품 배출 기준을 확인해주세요.",
        List.of(
            new GuideCheckSeed(
                "전원에서 완전히 분리했나요?",
                0,
                true
            ),
            new GuideCheckSeed(
                "제품을 임의로 분해하지 않았나요?",
                1,
                true
            ),
            new GuideCheckSeed(
                "소형 폐가전 수거처를 확인했나요?",
                2,
                true
            )
        )
    );

    seedGuide(
        "휴대폰",
        "사용하지 않는 휴대폰은 폐휴대폰 또는 소형 폐가전 회수 방법을 이용해 배출해요.",
        "사용하지 않는 휴대폰과 스마트폰은 제조사·판매점 회수, 지역의 폐휴대폰 또는 소형 폐가전 수거 체계 등 이용 가능한 방법을 확인해 배출합니다. 배출 전에는 개인정보 보호를 위해 필요한 데이터를 백업하고 기기의 개인정보를 삭제해주세요.",
        "배터리가 부풀거나 손상된 휴대폰은 임의로 분해하지 마세요. 저장된 개인정보와 계정이 남아 있지 않도록 확인하고, 실제 회수 방법은 지역 또는 제조사 안내를 확인해주세요.",
        List.of(
            new GuideCheckSeed(
                "필요한 데이터를 백업했나요?",
                0,
                false
            ),
            new GuideCheckSeed(
                "개인정보와 계정을 삭제했나요?",
                1,
                true
            ),
            new GuideCheckSeed(
                "배터리가 부풀거나 파손되지 않았는지 확인했나요?",
                2,
                true
            ),
            new GuideCheckSeed(
                "폐휴대폰 또는 소형 폐가전 회수처를 확인했나요?",
                3,
                true
            )
        )
    );

    seedGuide(
        "소형 폐가전",
        "소형 전자제품은 일반쓰레기보다 지역에서 운영하는 폐가전 수거 방식을 먼저 확인해요.",
        "크기가 작은 전자제품도 플라스틱, 금속, 회로, 배터리 등 여러 재질이 결합되어 있을 수 있습니다. 지역에서 운영하는 소형 폐가전 수거함, 폐전자제품 회수 서비스 또는 지정 배출 방법이 있는지 확인한 뒤 배출해주세요.",
        "배터리가 포함된 제품은 배터리 종류와 분리 가능 여부를 먼저 확인해주세요. 배터리를 억지로 분해하거나 전자제품을 임의로 파손하지 마세요.",
        List.of(
            new GuideCheckSeed(
                "제품에 배터리가 포함되어 있는지 확인했나요?",
                0,
                true
            ),
            new GuideCheckSeed(
                "분리 가능한 배터리는 별도 배출 기준을 확인했나요?",
                1,
                true
            ),
            new GuideCheckSeed(
                "지역의 소형 폐가전 수거 방법을 확인했나요?",
                2,
                true
            )
        )
    );
  }

  private void insertCategory(
      String code,
      String name,
      String description,
      int sortOrder
  ) {
    jdbcTemplate.update(
        """
        INSERT IGNORE INTO waste_categories
            (active, code, description, name, sort_order)
        VALUES
            (?, ?, ?, ?, ?)
        """,
        true,
        code,
        description,
        name,
        sortOrder
    );
  }

  private void insertItem(
      String name,
      String searchKeywords,
      String categoryCode
  ) {
    jdbcTemplate.update(
        """
        INSERT INTO waste_items
            (active, image_url, name, search_keywords, category_id)
        SELECT
            ?, NULL, ?, ?, c.id
        FROM waste_categories c
        WHERE c.code = ?
          AND NOT EXISTS (
              SELECT 1
              FROM waste_items wi
              WHERE wi.name = ?
          )
        """,
        true,
        name,
        searchKeywords,
        categoryCode,
        name
    );
  }

  /**
   * 이미 관리자가 직접 작성한 가이드가 있다면
   * 해당 가이드를 절대 덮어쓰지 않습니다.
   */
  private void seedGuide(
      String wasteItemName,
      String summary,
      String disposalMethod,
      String caution,
      List<GuideCheckSeed> checkItems
  ) {

    Integer existingGuideCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM recycle_guides g
            JOIN waste_items wi
              ON wi.id = g.waste_item_id
            WHERE wi.name = ?
            """,
            Integer.class,
            wasteItemName
        );

    if (
        existingGuideCount != null
            && existingGuideCount > 0
    ) {
      return;
    }

    int inserted =
        jdbcTemplate.update(
            """
            INSERT INTO recycle_guides
                (summary, disposal_method, caution, waste_item_id)
            SELECT
                ?, ?, ?, wi.id
            FROM waste_items wi
            WHERE wi.name = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM recycle_guides g
                  WHERE g.waste_item_id = wi.id
              )
            """,
            summary,
            disposalMethod,
            caution,
            wasteItemName
        );

    if (inserted == 0) {
      return;
    }

    Long guideId =
        jdbcTemplate.queryForObject(
            """
            SELECT g.id
            FROM recycle_guides g
            JOIN waste_items wi
              ON wi.id = g.waste_item_id
            WHERE wi.name = ?
            ORDER BY g.id
            LIMIT 1
            """,
            Long.class,
            wasteItemName
        );

    if (guideId == null) {
      return;
    }

    for (GuideCheckSeed checkItem : checkItems) {
      jdbcTemplate.update(
          """
          INSERT INTO recycle_guide_check_items
              (content, sort_order, `required`, recycle_guide_id)
          VALUES
              (?, ?, ?, ?)
          """,
          checkItem.content(),
          checkItem.sortOrder(),
          checkItem.required(),
          guideId
      );
    }
  }

  private record GuideCheckSeed(
      String content,
      int sortOrder,
      boolean required
  ) {
  }
}
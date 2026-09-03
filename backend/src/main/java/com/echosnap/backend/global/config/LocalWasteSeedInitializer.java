package com.echosnap.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalWasteSeedInitializer implements CommandLineRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(String... args) {
    seedCategories();
    seedItems();
    seedGuides();
  }

  private void seedCategories() {
    insertCategory(
        "PAPER",
        "종이류",
        "신문, 잡지, 종이상자 등 종이 재활용 품목",
        1
    );

    insertCategory(
        "PLASTIC",
        "플라스틱류",
        "페트병, 플라스틱 용기 등 플라스틱 재활용 품목",
        2
    );

    insertCategory(
        "VINYL",
        "비닐류",
        "비닐봉투와 비닐 포장재 등",
        3
    );

    insertCategory(
        "METAL",
        "캔·고철류",
        "음료 캔, 통조림 캔 등 금속 재활용 품목",
        4
    );

    insertCategory(
        "GLASS",
        "유리류",
        "음료병 등 유리 재활용 품목",
        5
    );

    insertCategory(
        "STYROFOAM",
        "스티로폼류",
        "포장용 스티로폼과 완충재 등",
        6
    );

    insertCategory(
        "GENERAL",
        "일반쓰레기",
        "재활용이 어려워 종량제 봉투로 배출하는 품목",
        7
    );
  }

  private void seedItems() {
    insertItem(
        "종이",
        "신문 잡지 전단지 종이 paper",
        "PAPER"
    );

    insertItem(
        "종이박스",
        "박스 택배박스 골판지 cardboard box",
        "PAPER"
    );

    insertItem(
        "페트병",
        "페트 PET 생수병 음료병 plastic bottle",
        "PLASTIC"
    );

    insertItem(
        "플라스틱 용기",
        "플라스틱 용기 통 컵 plastic container",
        "PLASTIC"
    );

    insertItem(
        "비닐",
        "비닐 봉투 포장지 plastic bag vinyl",
        "VINYL"
    );

    insertItem(
        "캔",
        "캔 알루미늄캔 철캔 음료캔 can metal",
        "METAL"
    );

    insertItem(
        "유리병",
        "유리병 음료병 glass bottle",
        "GLASS"
    );

    insertItem(
        "스티로폼",
        "스티로폼 완충재 EPS styrofoam",
        "STYROFOAM"
    );

    insertItem(
        "일반쓰레기",
        "일반쓰레기 종량제 trash garbage",
        "GENERAL"
    );
  }

  /**
   * local 프로필에서 기본 품목의 분리배출 가이드를 준비합니다.
   *
   * 이미 관리자가 등록한 가이드가 있는 품목은 절대 덮어쓰지 않습니다.
   * 따라서 Swagger나 관리자 웹에서 수정한 내용이 서버 재시작 때문에
   * 초기화되지 않습니다.
   */
  private void seedGuides() {
    seedGuide(
        "종이",
        "오염되지 않은 종이는 다른 재질을 제거한 뒤 종이류로 배출해요.",
        "신문, 잡지, 전단지 등은 물기에 젖지 않게 모으고 비닐 코팅, 스프링, 테이프처럼 분리 가능한 다른 재질을 제거한 뒤 종이류 수거 기준에 맞게 배출합니다.",
        "음식물이나 기름 등으로 심하게 오염된 종이, 감열지처럼 재활용이 어려운 종이는 지역 기준에 따라 일반쓰레기로 분류될 수 있습니다.",
        List.of(
            new GuideCheckSeed("물기나 음식물 오염이 없는지 확인했나요?", 0, true),
            new GuideCheckSeed("비닐·스프링 등 다른 재질을 제거했나요?", 1, true),
            new GuideCheckSeed("흩어지지 않도록 한데 모았나요?", 2, false)
        )
    );

    seedGuide(
        "종이박스",
        "상자를 펼치고 테이프와 송장 등 다른 재질을 제거해 배출해요.",
        "택배박스와 골판지 상자는 내용물을 비운 뒤 테이프, 운송장, 비닐 완충재 등 종이가 아닌 부분을 가능한 범위에서 제거합니다. 상자는 납작하게 펼쳐 부피를 줄이고 종이류 배출 기준에 맞게 묶거나 모아서 배출합니다.",
        "물이나 기름에 심하게 젖거나 오염되어 재활용이 어려운 상자는 지역의 일반쓰레기 배출 기준을 확인해주세요.",
        List.of(
            new GuideCheckSeed("상자 안의 내용물을 모두 비웠나요?", 0, true),
            new GuideCheckSeed("테이프와 운송장을 제거했나요?", 1, true),
            new GuideCheckSeed("상자를 납작하게 펼쳤나요?", 2, true)
        )
    );

    seedGuide(
        "페트병",
        "내용물을 비우고 라벨 등 다른 재질을 분리한 뒤 페트병 수거 기준에 맞게 배출해요.",
        "페트병은 내용물을 완전히 비우고 가능한 경우 물로 가볍게 헹굽니다. 라벨과 뚜껑 등 분리 가능한 다른 재질은 지역 배출 기준에 맞춰 분리하고, 투명 페트병 별도배출 제도를 운영하는 지역에서는 해당 수거함에 배출합니다.",
        "색상이 있거나 여러 재질이 결합된 용기, 내용물이나 오염을 제거하기 어려운 용기는 지역별 분류 기준이 다를 수 있으므로 거주지 안내를 함께 확인해주세요.",
        List.of(
            new GuideCheckSeed("내용물을 완전히 비웠나요?", 0, true),
            new GuideCheckSeed("오염이 남아 있다면 가볍게 헹궜나요?", 1, true),
            new GuideCheckSeed("라벨 등 분리 가능한 다른 재질을 제거했나요?", 2, true),
            new GuideCheckSeed("지역의 투명 페트병 별도배출 기준을 확인했나요?", 3, false)
        )
    );

    seedGuide(
        "플라스틱 용기",
        "내용물을 비우고 이물질과 다른 재질을 제거한 뒤 플라스틱류로 배출해요.",
        "플라스틱 용기와 통은 내용물을 완전히 비운 뒤 음식물이나 이물질을 제거합니다. 뚜껑, 펌프, 금속 부품 등 쉽게 분리할 수 있는 다른 재질은 분리한 뒤 지역의 플라스틱류 배출 기준에 맞게 배출합니다.",
        "복합재질 제품이나 이물질 제거가 어려운 용기는 재활용이 어려울 수 있습니다. 재질표시와 거주지의 세부 분리배출 기준을 함께 확인해주세요.",
        List.of(
            new GuideCheckSeed("내용물을 완전히 비웠나요?", 0, true),
            new GuideCheckSeed("음식물이나 이물질을 제거했나요?", 1, true),
            new GuideCheckSeed("분리 가능한 금속·펌프·뚜껑 등을 확인했나요?", 2, true)
        )
    );

    seedGuide(
        "비닐",
        "내용물을 비우고 이물질을 제거한 뒤 비닐류로 배출해요.",
        "비닐봉투와 비닐 포장재는 내용물을 완전히 비우고 음식물 등 이물질을 제거한 뒤 배출합니다. 다른 재질이 붙어 있다면 가능한 범위에서 분리하고, 흩날리지 않도록 모아서 지역의 비닐류 수거 기준에 맞게 배출합니다.",
        "이물질 제거가 어렵거나 여러 재질이 붙어 분리가 어려운 경우에는 재활용이 어려울 수 있습니다. 세부 배출 기준은 지역마다 다를 수 있으므로 거주지의 배출 안내도 함께 확인해주세요.",
        List.of(
            new GuideCheckSeed("내용물을 완전히 비웠나요?", 0, true),
            new GuideCheckSeed("음식물 등 이물질을 제거했나요?", 1, true),
            new GuideCheckSeed("분리 가능한 다른 재질을 제거했나요?", 2, true),
            new GuideCheckSeed("내 지역의 비닐류 배출 일정도 확인했나요?", 3, false)
        )
    );

    seedGuide(
        "캔",
        "내용물을 비우고 다른 재질을 제거한 뒤 캔·고철류 기준에 맞게 배출해요.",
        "음료캔과 통조림캔은 내용물을 완전히 비우고 가능한 경우 내부를 가볍게 헹굽니다. 플라스틱 뚜껑이나 빨대 등 분리 가능한 다른 재질은 제거하고, 캔류 수거 기준에 맞게 배출합니다.",
        "내용물이 남아 있거나 위험한 물질이 들어 있던 용기, 압축가스가 남을 수 있는 제품은 일반 캔과 처리 방법이 다를 수 있으므로 제품 및 지역 안내를 확인해주세요.",
        List.of(
            new GuideCheckSeed("캔 안의 내용물을 완전히 비웠나요?", 0, true),
            new GuideCheckSeed("이물질이 남아 있다면 가볍게 헹궜나요?", 1, true),
            new GuideCheckSeed("플라스틱 등 다른 재질을 제거했나요?", 2, true)
        )
    );

    seedGuide(
        "유리병",
        "병 안의 내용물을 비우고 뚜껑 등 다른 재질을 분리해 유리병류로 배출해요.",
        "음료나 식품 유리병은 내용물을 완전히 비운 뒤 필요한 경우 가볍게 헹굽니다. 뚜껑과 쉽게 분리되는 다른 재질을 제거하고 깨지지 않도록 주의해 지역의 유리병류 수거 기준에 맞게 배출합니다.",
        "거울, 도자기, 내열유리, 전구 등은 일반 유리병과 재질 및 처리 방법이 다를 수 있습니다. 깨진 유리는 안전하게 포장하고 지역의 별도 배출 기준을 확인해주세요.",
        List.of(
            new GuideCheckSeed("병 안의 내용물을 완전히 비웠나요?", 0, true),
            new GuideCheckSeed("뚜껑 등 다른 재질을 분리했나요?", 1, true),
            new GuideCheckSeed("깨진 유리인지 확인하고 안전하게 취급하고 있나요?", 2, true)
        )
    );

    seedGuide(
        "스티로폼",
        "내용물과 테이프·상표를 제거하고 깨끗한 스티로폼만 분리배출해요.",
        "포장용 스티로폼은 내용물을 모두 제거하고 표면에 붙은 테이프, 운송장, 상표 등 다른 재질을 가능한 범위에서 떼어냅니다. 음식물이나 이물질이 없는 깨끗한 상태로 지역의 스티로폼류 배출 기준에 맞게 배출합니다.",
        "오염이 심하거나 다른 재질과 분리가 어려운 스티로폼, 건축용 단열재 등은 일반 포장용 스티로폼과 처리 기준이 다를 수 있습니다.",
        List.of(
            new GuideCheckSeed("내용물과 이물질을 모두 제거했나요?", 0, true),
            new GuideCheckSeed("테이프와 운송장 등 다른 재질을 제거했나요?", 1, true),
            new GuideCheckSeed("깨끗하고 마른 상태인지 확인했나요?", 2, true)
        )
    );

    seedGuide(
        "일반쓰레기",
        "재활용이 어려운 생활폐기물은 지역의 종량제 배출 기준에 맞게 버려요.",
        "재활용 품목으로 분류되지 않거나 오염 때문에 재활용이 어려운 생활폐기물은 거주 지역에서 사용하는 종량제 봉투 등 지정된 방식으로 배출합니다. 품목별 별도 수거 대상인지 먼저 확인한 뒤 일반쓰레기로 배출해주세요.",
        "음식물쓰레기, 대형폐기물, 폐가전, 건전지, 형광등 등 별도 수거 체계가 있는 품목을 일반 종량제 봉투에 함께 넣지 않도록 주의해주세요.",
        List.of(
            new GuideCheckSeed("재활용 또는 별도 수거 대상이 아닌지 확인했나요?", 0, true),
            new GuideCheckSeed("지역에서 사용하는 지정 종량제 봉투를 사용했나요?", 1, true),
            new GuideCheckSeed("위험하거나 날카로운 물건은 안전하게 포장했나요?", 2, false)
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
   * 해당 품목에 가이드가 전혀 없을 때만 local 기본 가이드를 추가합니다.
   * 이미 관리자 API에서 저장된 가이드는 수정하거나 체크리스트를 추가하지 않습니다.
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

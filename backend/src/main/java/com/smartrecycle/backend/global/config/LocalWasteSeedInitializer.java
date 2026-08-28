package com.smartrecycle.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalWasteSeedInitializer implements CommandLineRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(String... args) {
    seedCategories();
    seedItems();
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
}
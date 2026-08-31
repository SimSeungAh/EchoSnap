package com.smartrecycle.backend.domain.residence.entity;

public enum GeneralHousingType {

  DETACHED_HOUSE(
      "단독주택",
      PublicHousingGroup.DETACHED
  ),

  MULTI_FAMILY_HOUSE(
      "다가구주택",
      PublicHousingGroup.DETACHED
  ),

  ROW_HOUSE(
      "연립주택",
      PublicHousingGroup.SHARED
  ),

  MULTI_UNIT_HOUSE(
      "다세대주택",
      PublicHousingGroup.SHARED
  );

  private final String displayName;
  private final PublicHousingGroup publicHousingGroup;

  GeneralHousingType(
      String displayName,
      PublicHousingGroup publicHousingGroup
  ) {
    this.displayName = displayName;
    this.publicHousingGroup = publicHousingGroup;
  }

  public String getDisplayName() {
    return displayName;
  }

  public PublicHousingGroup getPublicHousingGroup() {
    return publicHousingGroup;
  }

  public enum PublicHousingGroup {
    DETACHED,
    SHARED
  }
}
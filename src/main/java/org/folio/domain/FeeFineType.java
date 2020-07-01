package org.folio.domain;

public enum FeeFineType {
  // These are hardcoded in mod-feesfines

  OVERDUE_FINE("Overdue fine", "9523cb96-e752-40c2-89da-60f3961a488d", true),
  REPLACEMENT_PROCESSING_FEE("Replacement processing fee", "d20df2fb-45fd-4184-b238-0d25747ffdd9", true),
  LOST_ITEM_FEE("Lost item fee", "cf238f9f-7018-47b7-b815-bb2db798e19f", true),
  LOST_ITEM_PROCESSING_FEE("Lost item processing fee", "c7dede15-aa48-45ed-860b-f996540180e0", true);

  private final String name;
  private final String id;
  private final boolean automatic;

  FeeFineType(String name, String id, boolean automatic) {
    this.name = name;
    this.id = id;
    this.automatic = automatic;
  }

  public String getName() {
    return name;
  }

  public boolean isAutomatic() {
    return automatic;
  }

  public String getId() {
    return id;
  }
}

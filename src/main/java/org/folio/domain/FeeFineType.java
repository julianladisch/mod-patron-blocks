package org.folio.domain;

public enum FeeFineType {
  // These are hardcoded in mod-feesfines

  OVERDUE_FINE("9523cb96-e752-40c2-89da-60f3961a488d"),
  REPLACEMENT_PROCESSING_FEE("d20df2fb-45fd-4184-b238-0d25747ffdd9"),
  LOST_ITEM_FEE("cf238f9f-7018-47b7-b815-bb2db798e19f"),
  LOST_ITEM_PROCESSING_FEE("c7dede15-aa48-45ed-860b-f996540180e0");

  private final String id;

  FeeFineType(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}

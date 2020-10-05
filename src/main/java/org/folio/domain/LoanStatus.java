package org.folio.domain;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.util.Arrays;

public enum LoanStatus {
  NONE(""),
  CHECKED_OUT("Checked out"),
  DECLARED_LOST("Declared lost"),
  CLAIMED_RETURNED("Claimed returned"),
  DUE_DATE_CHANGED("Due date changed");


  public static LoanStatus from(String value, String date) {
    return Arrays.stream(values())
      .filter(status -> status.valueMatches(value))
      .findFirst().map(status -> {
        status.setDate(date);
        return status;
      })
      .orElse(NONE);
  }

  private final String value;

  private String date;

  LoanStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public String getDate() {
    return date;
  }

  void setDate(String date) {
    this.date = date;
  }

  private boolean valueMatches(String value) {
    return equalsIgnoreCase(getValue(), value);
  }
}

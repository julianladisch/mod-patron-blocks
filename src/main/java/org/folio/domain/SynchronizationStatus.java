package org.folio.domain;

public enum SynchronizationStatus {
  OPEN("open"), IN_PROGRESS("in-progress"), DONE("done"), FAILED("failed");

  private String value;

  SynchronizationStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}

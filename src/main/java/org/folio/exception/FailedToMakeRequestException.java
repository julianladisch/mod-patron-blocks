package org.folio.exception;

public class FailedToMakeRequestException extends RuntimeException {
  public FailedToMakeRequestException(String message) {
    super(message);
  }
}

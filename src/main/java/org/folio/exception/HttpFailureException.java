package org.folio.exception;

public class HttpFailureException extends RuntimeException {
  public HttpFailureException(String message) {
    super(message);
  }
}

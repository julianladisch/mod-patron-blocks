package org.folio.exception;

public class EntityNotFoundInDbException extends RuntimeException {
  public EntityNotFoundInDbException(String message) {
    super(message);
  }
}

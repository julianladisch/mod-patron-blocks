package org.folio.util;

import static java.lang.String.format;

import java.lang.invoke.MethodHandles;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AsyncProcessingContext {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected abstract String getName();

  public void logFailedValidationError(String failedStep, String additionalMessage) {
    String message = format("Context '%s' is invalid, failed to perform '%s'", getName(),
      failedStep);

    if (additionalMessage != null) {
      message += format(". %s", additionalMessage);
    }

    log.error(message);
  }

  public void logFailedValidationError(String failedStep) {
    logFailedValidationError(failedStep, null);
  }
}

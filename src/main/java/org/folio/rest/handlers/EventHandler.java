package org.folio.rest.handlers;

import io.vertx.core.Future;

public interface EventHandler {
  Future<String> handle(String payload);
}

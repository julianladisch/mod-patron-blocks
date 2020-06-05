package org.folio.rest.handlers;

import io.vertx.core.Future;

@FunctionalInterface
public interface EventHandler<E> {
  /**
   * Handle an event.
   *
   * @param event  the event to handle
   * @return ID of a UserSummary affected by the processed event
   */
  Future<String> handle(E event);
}
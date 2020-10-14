package org.folio.rest.handlers;

import static org.folio.util.PostgresUtils.getPostgresClient;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.folio.domain.Event;
import org.folio.domain.EventType;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.EventService;
import org.folio.service.UserSummaryService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class EventHandler<E extends Event> {
  protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected final UserSummaryRepository userSummaryRepository;
  protected final EventService eventService;
  protected final UserSummaryService userSummaryService;

  protected EventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    PostgresClient postgresClient = getPostgresClient(okapiHeaders, vertx);
    userSummaryRepository = new UserSummaryRepository(postgresClient);
    eventService = new EventService(postgresClient);
    userSummaryService = new UserSummaryService(postgresClient);
  }

  protected EventHandler(PostgresClient postgresClient) {
    userSummaryRepository = new UserSummaryRepository(postgresClient);
    eventService = new EventService(postgresClient);
    userSummaryService = new UserSummaryService(postgresClient);
  }

  public Future<String> handle(E event) {
    return handle(event, false);
  }

  /**
   * Handle an event.
   *
   * @param event  the event to handle
   * @return ID of a UserSummary affected by the processed event
   */
  public abstract Future<String> handle(E event, boolean skipUserSummaryRebuilding);

  protected void logResult(AsyncResult<String> result, E event) {
    String eventType = EventType.getNameByEvent(event);
    if (result.failed()) {
      log.error("Failed to process event {} with payload:\n{}",
        result.cause(), eventType, Json.encodePrettily(event));
    } else {
      log.info("Event {} processed successfully. Affected user summary: {}",
        eventType, result.result());
    }
  }
}

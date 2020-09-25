package org.folio.rest.handlers;

import java.util.Map;

import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class LoanDueDateChangedEventHandler extends EventHandler<LoanDueDateChangedEvent> {

  public LoanDueDateChangedEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
  }

  public LoanDueDateChangedEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
  }

  @Override
  public Future<String> handle(LoanDueDateChangedEvent event) {
    return eventService.save(event)
      .map(eventId -> event.getUserId())
      .compose(userSummaryService::rebuild)
      .onComplete(result -> logResult(result, event));
  }
}

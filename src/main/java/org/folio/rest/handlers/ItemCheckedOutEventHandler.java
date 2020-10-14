package org.folio.rest.handlers;

import java.util.Map;

import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ItemCheckedOutEventHandler extends EventHandler<ItemCheckedOutEvent> {

  public ItemCheckedOutEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
  }

  public ItemCheckedOutEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
  }

  @Override
  public Future<String> handle(ItemCheckedOutEvent event, boolean skipUserSummaryRebuilding) {
    return eventService.save(event)
      .compose(eventId -> skipUserSummaryRebuilding
        ? Future.succeededFuture()
        : userSummaryService.rebuild(event.getUserId()))
      .onComplete(result -> logResult(result, event));
  }
}

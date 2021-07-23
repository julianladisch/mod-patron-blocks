package org.folio.rest.handlers;

import java.util.Map;

import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ItemClaimedReturnedEventHandler extends EventHandler<ItemClaimedReturnedEvent> {

  public ItemClaimedReturnedEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
  }

  public ItemClaimedReturnedEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
  }
}

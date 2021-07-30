package org.folio.rest.handlers;

import java.util.Map;

import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Vertx;

public class LoanDueDateChangedEventHandler extends EventHandler<LoanDueDateChangedEvent> {

  public LoanDueDateChangedEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
  }

  public LoanDueDateChangedEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
  }
}

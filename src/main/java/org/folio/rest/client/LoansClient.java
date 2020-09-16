package org.folio.rest.client;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.folio.rest.jaxrs.model.OpenLoan;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class LoansClient extends OkapiClient{
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public LoansClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(WebClient.create(vertx), okapiHeaders);
  }
}

package org.folio.rest.client;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.folio.rest.jaxrs.model.OverdueFinePolicy;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class FeesFinesClient extends OkapiClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public FeesFinesClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(WebClient.create(vertx), okapiHeaders);
  }

  public Future<OverdueFinePolicy> findOverdueFinePolicyById(String overdueFinePolicyId) {
    return fetchById("overdue-fines-policies", overdueFinePolicyId, OverdueFinePolicy.class);
  }
}

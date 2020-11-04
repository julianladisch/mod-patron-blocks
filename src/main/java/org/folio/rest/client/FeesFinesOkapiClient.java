package org.folio.rest.client;

import java.util.Map;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.FeeFineCollection;
import org.folio.rest.jaxrs.model.Feefine;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class FeesFinesOkapiClient extends OkapiClient {

  public FeesFinesOkapiClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<Map<String, String>> fetchFeeFineTypes() {
    return fetchAll("/feefines", FeeFineCollection.class)
      .map(feesFines -> feesFines.getFeefines().stream()
        .collect(Collectors.toMap(Feefine::getFeeFineType, Feefine::getId)));
  }
}

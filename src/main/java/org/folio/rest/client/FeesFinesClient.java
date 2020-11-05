package org.folio.rest.client;

import java.util.List;
import java.util.Map;

import org.folio.rest.jaxrs.model.FeeFineTypeCollection;
import org.folio.rest.jaxrs.model.Feefine;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class FeesFinesClient extends OkapiClient {

  public FeesFinesClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<List<Feefine>> fetchFeeFineTypes() {
    return fetchAll("/feefines", FeeFineTypeCollection.class)
      .map(FeeFineTypeCollection::getFeefines);
  }
}

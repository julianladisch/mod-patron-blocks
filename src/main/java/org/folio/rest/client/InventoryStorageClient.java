package org.folio.rest.client;

import java.util.Map;

import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Location;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class InventoryStorageClient extends OkapiClient {

  public InventoryStorageClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(WebClient.create(vertx), okapiHeaders);
  }

  public Future<Item> findItemById(String itemId) {
    return fetchById("item-storage/items", itemId, Item.class);
  }

  public Future<Location> findLocationById(String locationId) {
    return fetchById("locations", locationId, Location.class);
  }
}

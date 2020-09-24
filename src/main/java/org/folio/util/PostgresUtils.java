package org.folio.util;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Map;

import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Vertx;

public class PostgresUtils {
  private PostgresUtils() { }

  public static PostgresClient getPostgresClient(Map<String, String> okapiHeaders, Vertx vertx) {
    return PostgresClient.getInstance(vertx, tenantId(okapiHeaders));
  }
}

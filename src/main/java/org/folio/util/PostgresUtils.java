package org.folio.util;

import static org.folio.rest.tools.utils.TenantTool.calculateTenantId;

import java.util.Map;

import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Vertx;

public class PostgresUtils {
  private PostgresUtils() { }

  public static PostgresClient getPostgresClient(Map<String, String> okapiHeaders, Vertx vertx) {
    String tenantId = calculateTenantId(okapiHeaders.get(XOkapiHeaders.TENANT.toLowerCase()));
    return PostgresClient.getInstance(vertx, tenantId);
  }
}

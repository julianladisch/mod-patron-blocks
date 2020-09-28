package org.folio.rest.client;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class WebClientProvider {
  private static WebClient webClient;

  private WebClientProvider() {
  }

  public static WebClient getWebClient(Vertx vertx) {
    if (webClient == null) {
      webClient = WebClient.create(vertx);
    }

    return webClient;
  }
}

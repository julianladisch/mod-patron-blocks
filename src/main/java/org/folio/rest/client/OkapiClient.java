package org.folio.rest.client;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.okapi.common.XOkapiHeaders.TOKEN;
import static org.folio.okapi.common.XOkapiHeaders.URL;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;

public class OkapiClient {
  static final ObjectMapper objectMapper = new ObjectMapper();

  private final WebClient webClient;
  private final String okapiUrl;
  private final String tenant;
  private final String token;

  OkapiClient(WebClient webClient, Map<String, String> okapiHeaders) {
    this.webClient = webClient;
    okapiUrl = okapiHeaders.get(URL);
    tenant = okapiHeaders.get(TENANT);
    token = okapiHeaders.get(TOKEN);
  }

  HttpRequest<Buffer> getAbs(String path) {
    return webClient.getAbs(okapiUrl + path)
      .putHeader(ACCEPT, APPLICATION_JSON)
      .putHeader(URL, okapiUrl)
      .putHeader(TENANT, tenant)
      .putHeader(TOKEN, token);
  }

  HttpRequest<Buffer> postAbs(String path) {
    return webClient.postAbs(okapiUrl + path)
      .putHeader(ACCEPT, APPLICATION_JSON)
      .putHeader(URL, okapiUrl)
      .putHeader(TENANT, tenant)
      .putHeader(TOKEN, token);
  }
}

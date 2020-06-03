package org.folio.rest.client;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;

public class OkapiClient {
  private static final String OKAPI_URL_HEADER = "x-okapi-url";
  static final ObjectMapper objectMapper = new ObjectMapper();

  private final WebClient webClient;
  private final String okapiUrl;
  private final  String tenant;
  private final String token;

  OkapiClient(WebClient webClient, Map<String, String> okapiHeaders) {
    this.webClient = webClient;
    okapiUrl = okapiHeaders.get(OKAPI_URL_HEADER);
    tenant = okapiHeaders.get(OKAPI_HEADER_TENANT);
    token = okapiHeaders.get(OKAPI_HEADER_TOKEN);
  }

  HttpRequest<Buffer> getAbs(String path) {
    return webClient.getAbs(okapiUrl + path)
      .putHeader(OKAPI_HEADER_TENANT, tenant)
      .putHeader(OKAPI_URL_HEADER, okapiUrl)
      .putHeader(OKAPI_HEADER_TOKEN, token);
  }

  HttpRequest<Buffer> postAbs(String path) {
    return webClient.postAbs(okapiUrl + path)
      .putHeader(ACCEPT, APPLICATION_JSON)
      .putHeader(OKAPI_HEADER_TENANT, tenant)
      .putHeader(OKAPI_URL_HEADER, okapiUrl)
      .putHeader(OKAPI_HEADER_TOKEN, token);
  }
}

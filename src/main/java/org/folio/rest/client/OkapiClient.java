package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.okapi.common.XOkapiHeaders.TOKEN;
import static org.folio.okapi.common.XOkapiHeaders.URL;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.exception.EntityNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class OkapiClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static final ObjectMapper objectMapper = new ObjectMapper();
  private final WebClient webClient;
  private final String okapiUrl;
  private final String tenant;
  private final String token;

  OkapiClient(WebClient webClient, Map<String, String> okapiHeaders) {
    CaseInsensitiveMap<String, String> headers = new CaseInsensitiveMap<>(okapiHeaders);
    this.webClient = webClient;
    okapiUrl = headers.get(URL);
    tenant = headers.get(TENANT);
    token = headers.get(TOKEN);
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

  <T> Future<T> fetchById(String pathToEntity, String id, Class<T> responseType) {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    String path = String.format("/%s/%s", pathToEntity, id);

    getAbs(path).send(promise);

    return promise.future().compose(response -> {
      int responseStatus = response.statusCode();
      if (responseStatus != 200) {
        String errorMessage = String.format("Failed to fetch %s by ID: %s. Response: %d %s",
          responseType.getName(), id, responseStatus, response.bodyAsString());
        log.error(errorMessage);
        return failedFuture(new EntityNotFoundException(errorMessage));
      } else {
        try {
          T fetchedObject = objectMapper.readValue(response.bodyAsString(), responseType);
          log.info("Fetched by ID: /{}/{}", path, id);
          return succeededFuture(fetchedObject);
        } catch (JsonProcessingException e) {
          log.error("Failed to parse response: " + response.bodyAsString());
          return failedFuture(e);
        }
      }
    });
  }
}

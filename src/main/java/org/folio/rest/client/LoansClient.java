package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.folio.exception.EntityNotFoundException;
import org.folio.rest.jaxrs.model.Loan;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class LoansClient extends OkapiClient{
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public LoansClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(WebClient.create(vertx), okapiHeaders);
  }

  public Future<Loan> findOpenLoansForUserId(String userId) {
//    return fetchById("loan-storage/loans", userId, Loan.class);
    String path = String.format("/loan-storage/loans?query=userId=%s&status.name=open", userId);

    return findOpenLoansForUserId(path, userId, Loan.class);
  }

  <T> Future<T> findOpenLoansForUserId(String path, String userId, Class<T> responseType) {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
//    String path = String.format("/%s?query=userId=%s", pathToEntity, userId);

    getAbs(path).send(promise);

    return promise.future().compose(response -> {
      int responseStatus = response.statusCode();
      if (responseStatus != 200) {
        String errorMessage = String.format("Failed to fetch %s by userId: %s. Response: %d %s",
          responseType.getName(), userId, responseStatus, response.bodyAsString());
        log.error(errorMessage);
        return failedFuture(new EntityNotFoundException(errorMessage));
      } else {
        try {
          T fetchedObject = objectMapper.readValue(response.bodyAsString(), responseType);
          log.info("Fetched by ID: {}/{}. Response body: \n{}", path, userId, response.bodyAsString());
          return succeededFuture(fetchedObject);
        } catch (JsonProcessingException e) {
          log.error("Failed to parse response from {}/{}. Response body: \n{}", e, path, userId,
            response.bodyAsString());
          return failedFuture(e);
        }
      }
    });
  }
}

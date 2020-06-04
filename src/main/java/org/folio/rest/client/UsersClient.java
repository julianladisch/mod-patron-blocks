package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.util.UuidHelper.validateUUID;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.folio.exception.EntityNotFoundException;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class UsersClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final OkapiClient okapiClient;

  public UsersClient(Vertx vertx, Map<String, String> okapiHeaders) {
    this.okapiClient = new OkapiClient(WebClient.create(vertx), okapiHeaders);
  }

  public Future<String> findPatronGroupIdForUser(String userId) {
    try {
      validateUUID(userId);
      log.info("Calling mod-users to find patron group ID for user " + userId);

      Promise<HttpResponse<Buffer>> promise = Promise.promise();
      okapiClient.getAbs("/users/" + userId)
        .send(promise);

      return promise.future().compose(response -> {
        if (response.statusCode() != 200) {
          return failedFuture(new EntityNotFoundException(
            String.format("Failed to fetch user with ID %s. Response status code: %d",
              userId, response.statusCode())));
        } else {
          try {
            JsonObject responseJson = new JsonObject(response.bodyAsString());
            String patronGroupId = responseJson.getString("patronGroup");
            validateUUID(patronGroupId);
            log.info("Patron group ID for user {} successfully found: {}", userId, patronGroupId);
            return succeededFuture(patronGroupId);
          } catch (Exception e) {
            return failedFuture(e);
          }
        }
      });
    } catch (Exception e) {
      return failedFuture(e);
    }
  }

}

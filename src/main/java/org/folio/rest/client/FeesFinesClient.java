package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.folio.domain.MultipleRecords;
import org.folio.exception.EntityNotFoundException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Loan;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class FeesFinesClient extends OkapiClient{
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public FeesFinesClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(WebClient.create(vertx), okapiHeaders);
  }

    public Future<MultipleRecords<Account>> findAccountsByUserId(String userId) {
      Promise<HttpResponse<Buffer>> promise = Promise.promise();
      var path = String.format("/accounts?query=userId=%s&status.name=open", userId);

      getAbs(path).send(promise);

      return promise.future().compose(response -> {
        var responseStatus = response.statusCode();
        if (responseStatus != 200) {
          String errorMessage = String.format("Failed to fetch %s by userId: %s. Response: %d %s",
            Loan.class.getName(), userId, responseStatus, response.bodyAsString());
          log.error(errorMessage);
          return failedFuture(new EntityNotFoundException(errorMessage));
        } else {
          log.info("Fetched by ID: {}/{}. Response body: \n{}", path, userId, response.bodyAsString());
          return mapResponseToAccounts(response);
        }
      });
    }

  private Future<MultipleRecords<Account>> mapResponseToAccounts(HttpResponse<Buffer> response) {
    var json = response.bodyAsJson(JsonObject.class);
    return MultipleRecords.from(json, this::mapToAccount, "accounts");
  }

  private Account mapToAccount(JsonObject representation) {

    return new Account()
      .withId(representation.getString("id"))
      .withUserId(representation.getString("userId"))
      .withLoanId(representation.getString("loanId"))
      .withFeeFineId(representation.getString("feeFineId"))
      .withFeeFineType(representation.getString("feeFineType"))
      .withRemaining(representation.getDouble("remaining"));
  }

  public Future<MultipleRecords<Account>> findOpenAccounts() {
    return null;
  }
}

package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import java.lang.invoke.MethodHandles;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.folio.domain.MultipleRecords;
import org.folio.exception.EntityNotFoundException;
import org.folio.rest.jaxrs.model.Loan;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class LoansClient extends OkapiClient{
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int PAGE_LIMIT = 50;

  public LoansClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(WebClient.create(vertx), okapiHeaders);
  }

  public Future<MultipleRecords<Loan>> findOpenLoansByUserId(String userId) {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    var path = String.format("/loan-storage/loans?query=userId=%s&status.name=open", userId);

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
        return mapResponseToLoans(response);
      }
    });
  }

  private Future<MultipleRecords<Loan>> mapResponseToLoans(HttpResponse<Buffer> response) {
    var json = response.bodyAsJson(JsonObject.class);
    return MultipleRecords.from(json, this::mapToLoan, "loans");
  }

  private Loan mapToLoan(JsonObject representation) {

    return new Loan()
      .withId(getProperty(representation, "id"))
      .withUserId(getProperty(representation, "userId"))
      .withDueDate(Date.valueOf(getProperty(representation, "dueDate")))
      .withItemStatus(getProperty(representation, "itemStatus"))
      .withDueDateChangedByRecall(representation.getBoolean("dueDateChangedByRecall"));
  }

  private String getProperty(JsonObject representation, String propertyName) {
    if (representation != null) {
      return representation.getString(propertyName);
    } else {
      return null;
    }
  }

  public Future<MultipleRecords<Loan>> findOpenLoans() {
    String path = "/loan-storage/loans?query=status.name=open";
//    getManyByPage(path, PAGE_LIMIT, 0)
//      .compose(json -> {
//        int offset = 0;
//        int totalRecords = json.getInteger("totalRecords");
//        int loansSize = json.getJsonArray("loans").size();
//        if (loansSize < totalRecords) {
//          offset = offset + loansSize;
//          getManyByPage(path, PAGE_LIMIT, offset);
//        }
//
//      })

//    return findLoansByPage(path);
    return null;
  }

//  private void recursiveApiCalls(String path, int counter, int maxRepetitions) {
//    int totalLoanSize = totalLoanSize +
//    return getManyByPage(path, PAGE_LIMIT, 0)
//      .compose(json -> {
//        int offset = 0;
//        int totalRecords = json.getInteger("totalRecords");
//        int loansSize = json.getJsonArray("loans").size();
//        if (loansSize < totalRecords) {
//          recursiveApiCalls(path, PAGE_LIMIT, offset + loansSize);
//          return;
//        }
//      });
//  }

  private List<Future<JsonObject>> findLoansByPage(String path) {

    return null;
  }

//    return getManyByPage(path, PAGE_LIMIT, 0)
//      .compose(json -> {
//        int totalRecords = json.getInteger("totalRecords");
//        int pages = (int) Math.ceil((totalRecords / (double) PAGE_LIMIT));
//        List<Future<JsonObject>> paginatedFutures = IntStream.range(0, pages)
//          .mapToObj(i -> {
//            Promise<JsonObject> promise = Promise.promise();
//            getManyByPage(path, PAGE_LIMIT, i * PAGE_LIMIT)
//              .onComplete(call -> {
//                if (call.succeeded()) {
//                  promise.complete(call.result());
//                } else {
//                  promise.fail("");
//                }
//              });
//            return promise.future();
//        }).collect(Collectors.toList());



//      });
}

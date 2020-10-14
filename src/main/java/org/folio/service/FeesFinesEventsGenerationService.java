package org.folio.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.repository.SynchronizationJobRepository;
import org.folio.rest.handlers.EventHandler;
import org.folio.rest.handlers.FeeFineBalanceChangedEventHandler;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.SynchronizationJob;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class FeesFinesEventsGenerationService extends EventsGenerationService {
  private final EventHandler<FeeFineBalanceChangedEvent> feeFineBalanceChangedEventHandler;

  public FeesFinesEventsGenerationService(Map<String, String> okapiHeaders, Vertx vertx,
    SynchronizationJobRepository syncRepository) {

    super(okapiHeaders, vertx, syncRepository);
    this.feeFineBalanceChangedEventHandler = new FeeFineBalanceChangedEventHandler(
      okapiHeaders, vertx);
  }

  @Override
  protected void addGeneratedEventsForEachPagesToList(SynchronizationJob syncJob, String path,
    int totalRecords, List<Future> generatedEventsForPages, int pageNumber) {

    Future<JsonObject> readPage = okapiClient.getMany(path, PAGE_LIMIT, pageNumber * PAGE_LIMIT)
      .compose(jsonPage -> {
        if (jsonPage == null || jsonPage.getJsonArray("accounts").size() == 0) {
          String errorMessage = String.format(
            "Error in receiving page number %d of accounts: %s", pageNumber, path);
          log.error(errorMessage);
          return failedFuture(errorMessage);
        }
        log.info("addGeneratedEventsForEachPagesToList: " + getClass().getSimpleName());
        return generateEventsByAccounts(mapJsonToAccounts(jsonPage))
          .onComplete(result -> {
            if (result.succeeded()) {
              log.info("success addGeneratedEventsForEachPagesToList: " + getClass().getSimpleName());
              updateSyncJobWithProcessedAccounts(syncJob,
                syncJob.getNumberOfProcessedFeesFines() + jsonPage.getJsonArray("accounts").size(),
                totalRecords);
            } else {
              log.error("failure addGeneratedEventsForEachPagesToList: " + getClass().getSimpleName());
              updateSyncJobWithError(syncJob, result.cause().getLocalizedMessage());
            }
          })
          .map(jsonPage);
      });
    generatedEventsForPages.add(readPage);
  }

  private Future<Void> generateEventsByAccounts(List<Account> records) {
    Future<Void> aggregateFuture = succeededFuture();

    for (Account account : records) {
      Future<Void> generateEvents = generateFeeFineBalanceChangedEvent(account);
      aggregateFuture.compose(r -> generateEvents);
      aggregateFuture = generateEvents;
    }

    return aggregateFuture;
  }

  private Future<Void> generateFeeFineBalanceChangedEvent(Account account) {
    return feeFineBalanceChangedEventHandler.handle(new FeeFineBalanceChangedEvent()
      .withBalance(BigDecimal.valueOf(account.getRemaining()))
      .withFeeFineId(account.getFeeFineId())
      .withUserId(account.getUserId())
      .withLoanId(account.getLoanId())
      .withMetadata(account.getMetadata()))
      .mapEmpty();
  }

  private List<Account> mapJsonToAccounts(JsonObject loansJson) {
    return loansJson.getJsonArray("accounts").stream()
      .filter(obj -> obj instanceof JsonObject)
      .map(JsonObject.class::cast)
      .map(this::mapToAccount)
      .collect(Collectors.toList());
  }

  private Account mapToAccount(JsonObject representation) {
    return new Account()
      .withId(representation.getString("id"))
      .withUserId(representation.getString("userId"))
      .withLoanId(representation.getString("loanId"))
      .withFeeFineId(representation.getString("feeFineId"))
      .withFeeFineType(representation.getString("feeFineType"))
      .withRemaining(representation.getDouble("remaining"))
      .withMetadata(mapMetadataFromJson(representation.getJsonObject("metadata")));
  }

  private void updateSyncJobWithProcessedAccounts(SynchronizationJob syncJob, int processed,
    int total) {

    SynchronizationJob updatedSyncJob = syncJob
      .withNumberOfProcessedFeesFines(processed)
      .withTotalNumberOfFeesFines(total);
    syncRepository.update(updatedSyncJob, syncJob.getId())
    .onComplete(r -> {
      if (r.failed()) {
        log.error("updateSyncJobWithProcessedAccounts failed");
      } else {
        log.info("updateSyncJobWithProcessedAccounts success");
      }
    });
  }
}

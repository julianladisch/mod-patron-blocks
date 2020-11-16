package org.folio.service;

import static io.vertx.core.Future.failedFuture;

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
  public static final String ACCOUNTS = "accounts";
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
        if (jsonPage == null || jsonPage.getJsonArray(ACCOUNTS).size() == 0) {
          String errorMessage = String.format(
            "Error in receiving page number %d of accounts: %s", pageNumber, path);
          log.error(errorMessage);
          return failedFuture(errorMessage);
        }
        return generateEventsByAccounts(mapJsonToAccounts(jsonPage))
          .onComplete(r -> logEventsGenerationResult(r, ACCOUNTS))
          .compose(r -> updateSyncJobWithProcessedAccounts(syncJob,
            syncJob.getNumberOfProcessedFeesFines() + jsonPage.getJsonArray(ACCOUNTS).size(),
            totalRecords))
          .recover(t -> updateSyncJobWithError(syncJob, t.getLocalizedMessage()))
          .map(jsonPage);
      });
    generatedEventsForPages.add(readPage);
  }

  private Future<String> generateEventsByAccounts(List<Account> records) {
    return records.stream()
      .map(this::generateFeeFineBalanceChangedEvent)
      .reduce(Future.succeededFuture(), (a, b) -> a.compose(r -> b));
  }

  private Future<String> generateFeeFineBalanceChangedEvent(Account account) {
    log.info("Start generateFeeFineBalanceChangedEvent for account " + account.getId());
    return feeFineBalanceChangedEventHandler.handle(new FeeFineBalanceChangedEvent()
      .withBalance(BigDecimal.valueOf(account.getRemaining()))
      .withFeeFineId(account.getId())
      .withFeeFineTypeId(account.getFeeFineId())
      .withUserId(account.getUserId())
      .withLoanId(account.getLoanId())
      .withMetadata(account.getMetadata()), true)
      .onComplete(r -> log.info("Finished generateFeeFineBalanceChangedEvent for account: "
        + account.getId()));
    }

  private List<Account> mapJsonToAccounts(JsonObject loansJson) {
    return loansJson.getJsonArray(ACCOUNTS).stream()
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

  private Future<SynchronizationJob> updateSyncJobWithProcessedAccounts(SynchronizationJob syncJob,
    int processed, int total) {

    syncJob.withNumberOfProcessedFeesFines(processed)
      .withTotalNumberOfFeesFines(total);

    return syncRepository.update(syncJob);
  }
}

package org.folio.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang.BooleanUtils.isTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.repository.SynchronizationJobRepository;
import org.folio.rest.handlers.EventHandler;
import org.folio.rest.handlers.ItemCheckedOutEventHandler;
import org.folio.rest.handlers.ItemClaimedReturnedEventHandler;
import org.folio.rest.handlers.ItemDeclaredLostEventHandler;
import org.folio.rest.handlers.LoanDueDateChangedEventHandler;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.jaxrs.model.SynchronizationJob;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class LoanEventsGenerationService extends EventsGenerationService {

  private static final String DECLARED_LOST_STATUS = "Declared lost";
  private static final String CLAIMED_RETURNED_STATUS = "Claimed returned";
  private static final String LOANS = "loans";
  private final EventHandler<ItemCheckedOutEvent> checkedOutEventHandler;
  private final EventHandler<ItemDeclaredLostEvent> declaredLostEventHandler;
  private final EventHandler<ItemClaimedReturnedEvent> claimedReturnedEventHandler;
  private final EventHandler<LoanDueDateChangedEvent> dueDateChangedEventHandler;

  public LoanEventsGenerationService(Map<String, String> okapiHeaders, Vertx vertx,
    SynchronizationJobRepository syncRepository) {

    super(okapiHeaders, vertx, syncRepository);
    this.checkedOutEventHandler = new ItemCheckedOutEventHandler(okapiHeaders, vertx);
    this.declaredLostEventHandler = new ItemDeclaredLostEventHandler(okapiHeaders, vertx);
    this.claimedReturnedEventHandler = new ItemClaimedReturnedEventHandler(okapiHeaders, vertx);
    this.dueDateChangedEventHandler = new LoanDueDateChangedEventHandler(okapiHeaders, vertx);
  }

  @Override
  protected void addGeneratedEventsForEachPagesToList(SynchronizationJob syncJob, String path,
    int totalRecords, List<Future> generatedEventsForPages, int pageNumber) {

    Future<JsonObject> readPage = okapiClient.getMany(path, PAGE_LIMIT, pageNumber * PAGE_LIMIT)
      .compose(jsonPage -> {
        if (jsonPage == null || jsonPage.getJsonArray(LOANS).size() == 0) {
          String errorMessage = String.format(
            "Error in receiving page number %d of loans: %s", pageNumber, path);
          log.error(errorMessage);
          return failedFuture(errorMessage);
        }
        return generateEventsByLoans(mapJsonToLoans(jsonPage))
          .onComplete(r -> logEventsGenerationResult(r, LOANS))
          .compose(r -> updateSyncJobWithProcessedLoans(syncJob,
            syncJob.getNumberOfProcessedLoans() + jsonPage.getJsonArray(LOANS).size(),
            totalRecords))
          .recover(t -> updateSyncJobWithError(syncJob, t.getLocalizedMessage()))
          .map(jsonPage);
      });
    generatedEventsForPages.add(readPage);
  }

  private List<Loan> mapJsonToLoans(JsonObject loansJson) {
    return loansJson.getJsonArray(LOANS).stream()
      .filter(obj -> obj instanceof JsonObject)
      .map(JsonObject.class::cast)
      .map(this::mapToLoan)
      .collect(Collectors.toList());
  }

  private Loan mapToLoan(JsonObject representation) {
    return new Loan()
      .withId(representation.getString("id"))
      .withUserId(representation.getString("userId"))
      .withDueDate(getDateFromJson(representation, "dueDate"))
      .withItemStatus(representation.getString("itemStatus"))
      .withDueDateChangedByRecall(representation.getBoolean("dueDateChangedByRecall"))
      .withMetadata(mapMetadataFromJson(representation.getJsonObject("metadata")));
  }

  private Future<Void> generateEventsByLoans(List<Loan> loans) {
    return loans.stream()
      .map(this::generateEvent)
      .reduce(Future.succeededFuture(), (a, b) -> a.compose(r -> b));
  }

  private Future<Void> generateEvent(Loan loan) {
    log.info("Start generateEvent for loan " + loan.getId());
    return checkedOutEventHandler.handle(new ItemCheckedOutEvent()
      .withLoanId(loan.getId())
      .withUserId(loan.getUserId())
      .withDueDate(loan.getDueDate())
      .withMetadata(loan.getMetadata()), true)
      .compose(v -> generateClaimedReturnedEvent(loan))
      .compose(v -> generateDeclaredLostEvent(loan))
      .compose(v -> generateDueDateChangedEvent(loan))
      .onComplete(r -> log.info("Finished generateEvent for loan: "
        + loan.getId()));
  }

  private Future<String> generateClaimedReturnedEvent(Loan loan) {
    if (CLAIMED_RETURNED_STATUS.equalsIgnoreCase(loan.getItemStatus())) {
      return claimedReturnedEventHandler.handle(new ItemClaimedReturnedEvent()
        .withLoanId(loan.getId())
        .withUserId(loan.getUserId())
        .withMetadata(loan.getMetadata()), true);
    }
    return succeededFuture(null);
  }

  private Future<String> generateDeclaredLostEvent(Loan loan) {
    if (DECLARED_LOST_STATUS.equals(loan.getItemStatus())) {
      return declaredLostEventHandler.handle(new ItemDeclaredLostEvent()
          .withLoanId(loan.getId())
          .withUserId(loan.getUserId())
          .withMetadata(loan.getMetadata()), true);
    }
    return succeededFuture(null);
  }

  private Future<Void> generateDueDateChangedEvent(Loan loan) {
    if (isTrue(loan.getDueDateChangedByRecall())) {
      dueDateChangedEventHandler.handle(new LoanDueDateChangedEvent()
          .withLoanId(loan.getId())
          .withUserId(loan.getUserId())
          .withDueDate(loan.getDueDate())
          .withDueDateChangedByRecall(loan.getDueDateChangedByRecall())
          .withMetadata(loan.getMetadata()), true);
    }
    return succeededFuture(null);
  }

  private Future<SynchronizationJob> updateSyncJobWithProcessedLoans(SynchronizationJob syncJob,
    int processed, int total) {

    syncJob.withNumberOfProcessedLoans(processed)
      .withTotalNumberOfLoans(total);

    return syncRepository.update(syncJob);
  }
}

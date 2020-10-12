package org.folio.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang.BooleanUtils.isTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.repository.SynchronizationRequestRepository;
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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class LoanEventsGenerationService extends EventsGenerationService {

  private static final String DECLARED_LOST_STATUS = "Declared lost";
  private static final String CLAIMED_RETURNED_STATUS = "Claimed returned";
  private final EventHandler<ItemCheckedOutEvent> checkedOutEventHandler;
  private final EventHandler<ItemDeclaredLostEvent> declaredLostEventHandler;
  private final EventHandler<ItemClaimedReturnedEvent> claimedReturnedEventHandler;
  private final EventHandler<LoanDueDateChangedEvent> dueDateChangedEventHandler;

  public LoanEventsGenerationService(Map<String, String> okapiHeaders, Vertx vertx,
    SynchronizationRequestRepository syncRepository) {

    super(okapiHeaders, vertx, syncRepository);
    this.checkedOutEventHandler = new ItemCheckedOutEventHandler(okapiHeaders, vertx);
    this.declaredLostEventHandler = new ItemDeclaredLostEventHandler(okapiHeaders, vertx);
    this.claimedReturnedEventHandler = new ItemClaimedReturnedEventHandler(okapiHeaders, vertx);
    this.dueDateChangedEventHandler = new LoanDueDateChangedEventHandler(okapiHeaders, vertx);
  }

  @Override
  public Future<SynchronizationJob> generateEvents(SynchronizationJob syncJob, String path) {
    return okapiClient.getManyByPage(path, 0, 0)
      .compose(response -> {
        int totalRecords = response.getInteger("totalRecords");
        int numberOfPages = calculateNumberOfPages(totalRecords);

        List<Future> generatedEventsForPages = new ArrayList<>();
        for (int i = 0; i < numberOfPages; i++) {
          int pageNumber = i;
          Future<JsonObject> readPage = okapiClient.getManyByPage(path, PAGE_LIMIT, i * PAGE_LIMIT)
            .compose(jsonPage -> {
              if (jsonPage == null || jsonPage.size() == 0) {
                String errorMessage = String.format(
                  "Error in receiving page number %d of loans: %s", pageNumber, path);
                log.error(errorMessage);
                return failedFuture(errorMessage);
              }
              return generateEventsByLoans(mapJsonToLoans(jsonPage))
                .onComplete(result -> {
                  if (result.succeeded()) {
                    updateSyncJobWithProcessedLoans(syncJob,
                      syncJob.getNumberOfProcessedLoans() + jsonPage.getJsonArray("loans").size(),
                      totalRecords);
                  } else {
                    updateSyncJobWithError(syncJob, result.cause().getLocalizedMessage());
                  }
                })
                .map(jsonPage);
            });
          generatedEventsForPages.add(readPage);
        }
        return updateJobWhenGenerationsCompleted(syncJob, generatedEventsForPages)
          .map(syncJob);

      });
  }

  private List<Loan> mapJsonToLoans(JsonObject loansJson) {
    return loansJson.getJsonArray("loans").stream()
      .filter(obj -> obj instanceof JsonObject)
      .map(JsonObject.class::cast)
      .map(this::mapToLoan)
      .collect(Collectors.toList());
  }

  private Loan mapToLoan(JsonObject representation) {
    return new Loan()
      .withId(representation.getString("id"))
      .withUserId(representation.getString("userId"))
      .withDueDate(parseDateFromJson(representation, "dueDate"))
      .withItemStatus(representation.getString("itemStatus"))
      .withDueDateChangedByRecall(representation.getBoolean("dueDateChangedByRecall"))
      .withMetadata(mapMetadataFromJson(representation.getJsonObject("metadata")));
  }

  private Future<Void> generateEventsByLoans(List<Loan> loans) {
    return CompositeFuture.all(loans.stream()
      .map(this::generateEvent)
      .collect(Collectors.toList()))
      .mapEmpty();
  }

  private Future<Void> generateEvent(Loan loan) {
    return checkedOutEventHandler.handle(new ItemCheckedOutEvent()
      .withLoanId(loan.getId())
      .withUserId(loan.getUserId())
      .withDueDate(loan.getDueDate())
      .withMetadata(loan.getMetadata()))
      .compose(v -> generateClaimedReturnedEvent(loan))
      .compose(v -> generateDeclaredLostEvent(loan))
      .compose(v -> generateDueDateChangedEvent(loan));
  }

  private Future<Void> generateClaimedReturnedEvent(Loan loan) {
    if (CLAIMED_RETURNED_STATUS.equalsIgnoreCase(loan.getItemStatus())) {
      return claimedReturnedEventHandler.handle(new ItemClaimedReturnedEvent()
        .withLoanId(loan.getId())
        .withUserId(loan.getUserId())
        .withMetadata(loan.getMetadata()))
        .mapEmpty();
    }
    return succeededFuture(null);
  }

  private Future<Void> generateDeclaredLostEvent(Loan loan) {
    if (DECLARED_LOST_STATUS.equals(loan.getItemStatus())) {
      declaredLostEventHandler.handle(new ItemDeclaredLostEvent()
        .withLoanId(loan.getId())
        .withUserId(loan.getUserId())
        .withMetadata(loan.getMetadata()));
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
        .withMetadata(loan.getMetadata()));
    }
    return succeededFuture(null);
  }

  private void updateSyncJobWithProcessedLoans(SynchronizationJob syncJob, int processed,
    int total) {

    SynchronizationJob updatedSyncJob = syncJob
      .withNumberOfProcessedLoans(processed)
      .withTotalNumberOfLoans(total);
    syncRepository.update(updatedSyncJob, syncJob.getId());
  }
}

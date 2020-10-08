package org.folio.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang.BooleanUtils.isTrue;
import static org.folio.domain.SynchronizationStatus.DONE;
import static org.folio.domain.SynchronizationStatus.FAILED;
import static org.folio.domain.SynchronizationStatus.IN_PROGRESS;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.folio.domain.SynchronizationStatus;
import org.folio.exception.EntityNotFoundException;
import org.folio.repository.SynchronizationRequestRepository;
import org.folio.rest.client.OkapiClient;
import org.folio.rest.handlers.EventHandler;
import org.folio.rest.handlers.FeeFineBalanceChangedEventHandler;
import org.folio.rest.handlers.ItemCheckedOutEventHandler;
import org.folio.rest.handlers.ItemClaimedReturnedEventHandler;
import org.folio.rest.handlers.ItemDeclaredLostEventHandler;
import org.folio.rest.handlers.LoanDueDateChangedEventHandler;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.SynchronizationJob;
import org.folio.rest.jaxrs.model.SynchronizationRequest;
import org.folio.rest.jaxrs.model.SynchronizationResponse;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SynchronizationRequestService {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String FULL_SCOPE = "full";
  private static final String USER_SCOPE = "user";
  private static final String DECLARED_LOST_STATUS = "Declared lost";
  private static final String CLAIMED_RETURNED_STATUS = "Claimed returned";
  private static final int PAGE_LIMIT = 50;

  private final SynchronizationRequestRepository syncRepository;
  private final EventHandler<ItemCheckedOutEvent> checkedOutEventHandler;
  private final EventHandler<ItemDeclaredLostEvent> declaredLostEventHandler;
  private final EventHandler<ItemClaimedReturnedEvent> claimedReturnedEventHandler;
  private final EventHandler<LoanDueDateChangedEvent> dueDateChangedEventHandler;
  private final EventHandler<FeeFineBalanceChangedEvent> feeFineBalanceChangedEventHandler;
  private final OkapiClient okapiClient;
  private final EventService eventService;
  private final String tenantId;
  private final Vertx vertx;

  public SynchronizationRequestService(Map<String, String> okapiHeaders, Vertx vertx) {
    this.vertx = vertx;
    this.tenantId = TenantTool.tenantId(okapiHeaders);
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    this.syncRepository = new SynchronizationRequestRepository(postgresClient);
    this.eventService = new EventService(postgresClient);
    this.okapiClient = new OkapiClient(vertx, okapiHeaders);
    this.checkedOutEventHandler = new ItemCheckedOutEventHandler (okapiHeaders, vertx);
    this.declaredLostEventHandler = new ItemDeclaredLostEventHandler(okapiHeaders, vertx);
    this.claimedReturnedEventHandler = new ItemClaimedReturnedEventHandler(okapiHeaders, vertx);
    this.dueDateChangedEventHandler = new LoanDueDateChangedEventHandler(okapiHeaders, vertx);
    this.feeFineBalanceChangedEventHandler = new FeeFineBalanceChangedEventHandler(okapiHeaders, vertx);
  }

  public Future<SynchronizationResponse> createSyncRequest(SynchronizationRequest request) {
    var syncRecordId = UUID.randomUUID().toString();
    SynchronizationJob entity = new SynchronizationJob()
      .withId(syncRecordId)
      .withStatus(SynchronizationStatus.OPEN.getValue())
      .withScope(request.getScope().value())
      .withUserId(request.getUserId())
      .withTotalNumberOfLoans(0)
      .withTotalNumberOfFeesFines(0)
      .withNumberOfProcessedLoans(0)
      .withNumberOfProcessedFeesFines(0);

    return syncRepository.save(entity)
      .map(id -> new SynchronizationResponse()
        .withId(id)
        .withScope(entity.getScope())
        .withStatus(entity.getStatus()));
  }

  public Future<SynchronizationJob> retrieveSyncRequest(String syncRequestId) {
    return syncRepository.get(syncRequestId)
      .compose(optionalSyncResponse -> {
        if (optionalSyncResponse.isPresent()) {
          return succeededFuture(optionalSyncResponse.get());
        }
        return failedFuture(new EntityNotFoundException(
          "This synchronization request does not exist"));
      });
  }

  public Future<SynchronizationJob> runSynchronization() {
     return syncRepository.getJobsByStatus(IN_PROGRESS)
       .compose(this::doSynchronization);
  }

  private Future<SynchronizationJob> doSynchronization(List<SynchronizationJob> reqList) {
    if (!reqList.isEmpty()) {
      log.debug("Synchronization is in-progress now");
      return succeededFuture();
    }

    return syncRepository.getTheOldestSyncRequest(tenantId)
      .compose(this::doSynchronization);
  }

  private Future<SynchronizationJob> doSynchronization(SynchronizationJob synchronizationJob) {
      return succeededFuture(updateStatusOfJob(synchronizationJob, IN_PROGRESS))
        .compose(syncJob -> cleanExistingEvents(syncJob, tenantId))
        .compose(this::createEventsByLoans)
        .compose(this::createEventsByFeesFines)
        .onSuccess(syncJob -> updateStatusOfJob(syncJob, DONE))
        .onFailure(throwable -> updateJobAsFailed(synchronizationJob,
          throwable.getLocalizedMessage()));
  }

  private Future<SynchronizationJob> createEventsByLoans(SynchronizationJob syncJob) {
     String path = USER_SCOPE.equalsIgnoreCase(syncJob.getScope())
       ? String.format("/loan-storage/loans?query=userId=%s&status.name=open", syncJob.getUserId())
       : "/loan-storage/loans?query=status.name=open";

     return okapiClient.getManyByPage(path, PAGE_LIMIT, 0)
       .compose(response -> {
         int totalRecords = response.getInteger("totalRecords");
         int numberOfPages = (int) Math.ceil((totalRecords / (double) PAGE_LIMIT));

         Future<JsonObject> future = succeededFuture();
         for (int i = 0; i < numberOfPages; i++) {
           int pageNumber = i;
           Future<JsonObject> readPage =
             okapiClient.getManyByPage(path, PAGE_LIMIT, i * PAGE_LIMIT)
               .compose(jsonPage -> {
                 if (jsonPage == null || jsonPage.size() == 0) {
                   String errorMessage = String.format(
                     "Error in receiving page number %d of loans: %s", pageNumber, path);
                   log.error(errorMessage);
                   return failedFuture(errorMessage);
                 }
                 vertx.executeBlocking(promise -> generateEventsByLoans(mapJsonToLoans(jsonPage))
                     .onComplete(v -> promise.complete()),
                   handler -> {
                     if (handler.succeeded()) {
                       updateSyncJobWithProcessedLoans(syncJob,
                         jsonPage.getJsonArray("loans").size(), totalRecords);
                     } else {
                       updateSyncJobWithError(syncJob, handler.cause().getLocalizedMessage());
                     }
                   }
                 );
                 return succeededFuture(jsonPage);
               });
           future.compose(v -> readPage);
           future = readPage;
         }
       return future.map(syncJob);
     });
  }

  private Future<SynchronizationJob> createEventsByFeesFines(SynchronizationJob syncJob) {
    String path = USER_SCOPE.equalsIgnoreCase(syncJob.getScope())
      ? String.format("/accounts?query=userId=%s&status.name=open", syncJob.getUserId())
      : "/accounts?query=status.name=open";

    return okapiClient.getManyByPage(path, PAGE_LIMIT, 0)
      .compose(response -> {
        int totalRecords = response.getInteger("totalRecords");
        int numberOfPages = (int) Math.ceil((totalRecords / (double) PAGE_LIMIT));

        Future<JsonObject> future = succeededFuture();
        for (int i = 0; i < numberOfPages; i++) {
          int pageNumber = i;
          Future<JsonObject> readPage =
            okapiClient.getManyByPage(path, PAGE_LIMIT, i * PAGE_LIMIT);
          readPage.compose(jsonPage -> {
            if (jsonPage == null || jsonPage.size() == 0) {
              String errorMessage = String.format(
                "Error in receiving page number %d of accounts: %s", pageNumber, path);
              log.error(errorMessage);
              return failedFuture(errorMessage);
            }
            vertx.executeBlocking(promise -> generateEventsByAccounts(mapJsonToAccounts(jsonPage))
                .onComplete(v -> promise.complete()),
              handler -> {
                if (handler.succeeded()) {
                  updateSyncJobWithProcessedAccounts(syncJob,
                    jsonPage.getJsonArray("accounts").size(), totalRecords);
                } else {
                  updateSyncJobWithError(syncJob, handler.cause().getLocalizedMessage());
                }
              }
            );
            return succeededFuture(jsonPage);
          });
          future.compose(v -> readPage);
          future = readPage;
        }
        return future.map(syncJob);
      });
  }

  private Future<Void> generateEventsByAccounts(List<Account> records) {
    return CompositeFuture.all(records.stream()
      .map(this::generateFeeFineBalanceChangedEvent)
      .collect(Collectors.toList()))
      .mapEmpty();
  }

  private void updateSyncJobWithError(SynchronizationJob syncJob, String localizedMessage) {
    List<String> errors = syncJob.getErrors();
    errors.add(localizedMessage);
    syncRepository.update(syncJob.withErrors(errors), syncJob.getId());
  }

  private void updateSyncJobWithProcessedLoans(SynchronizationJob syncJob, int processed,
    int total) {

    SynchronizationJob updatedSyncJob = syncJob
      .withNumberOfProcessedLoans(processed)
      .withTotalNumberOfLoans(total);
    syncRepository.update(updatedSyncJob, syncJob.getId());
  }

  private void updateSyncJobWithProcessedAccounts(SynchronizationJob syncJob, int processed,
    int total) {

    SynchronizationJob updatedSyncJob = syncJob
      .withNumberOfProcessedFeesFines(processed)
      .withTotalNumberOfFeesFines(total);
    syncRepository.update(updatedSyncJob, syncJob.getId());
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

  private Future<Void> generateFeeFineBalanceChangedEvent(Account account) {
    return feeFineBalanceChangedEventHandler.handle(new FeeFineBalanceChangedEvent()
        .withBalance(BigDecimal.valueOf(account.getRemaining()))
        .withFeeFineId(account.getFeeFineId())
        .withUserId(account.getUserId())
        .withLoanId(account.getLoanId())
        .withMetadata(account.getMetadata()))
      .mapEmpty();
  }

  private Future<SynchronizationJob> cleanExistingEvents(SynchronizationJob syncJob,
    String tenantId) {

    var scope = syncJob.getScope();
    if (FULL_SCOPE.equalsIgnoreCase(scope)) {
      return eventService.removeAllEvents(tenantId)
        .map(syncJob);
    }
    return eventService.removeAllEventsForUser(tenantId, syncJob.getUserId())
      .map(syncJob);
  }

  private SynchronizationJob updateStatusOfJob(SynchronizationJob syncJob,
    SynchronizationStatus syncStatus) {

    syncJob.setStatus(syncStatus.getValue());
    log.debug("Status of synchronization job has been updated: " + syncStatus.getValue());
    syncRepository.update(syncJob, syncJob.getId());

    return syncJob;
  }

  private void updateJobAsFailed(SynchronizationJob syncJob, String errorMessage) {
    syncJob.getErrors().add(errorMessage);
    syncJob.setStatus(FAILED.getValue());
    log.error("Synchronization job failed " + errorMessage);
    syncRepository.update(syncJob, syncJob.getId());
  }

  private List<Loan> mapJsonToLoans(JsonObject loansJson) {
    return loansJson.getJsonArray("loans").stream()
      .filter(obj -> obj instanceof JsonObject)
      .map(JsonObject.class::cast)
      .map(this::mapToLoan)
      .collect(Collectors.toList());
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

  private Loan mapToLoan(JsonObject representation) {
    return new Loan()
      .withId(representation.getString("id"))
      .withUserId(representation.getString("userId"))
      .withDueDate(parseDateFromJson(representation, "dueDate"))
      .withItemStatus(representation.getString("itemStatus"))
      .withDueDateChangedByRecall(representation.getBoolean("dueDateChangedByRecall"))
      .withMetadata(mapMetadataFromJson(representation.getJsonObject("metadata")));
  }

  private Metadata mapMetadataFromJson(JsonObject jsonMetadata) {
    return new Metadata()
      .withCreatedDate(parseDateFromJson(jsonMetadata, "createdDate"))
      .withUpdatedDate(parseDateFromJson(jsonMetadata, "updatedDate"));
  }

  private Date parseDateFromJson(JsonObject representation, String fieldName) {
    try {
      String dueDate = representation.getString(fieldName);
      return DateUtils.parseDate(dueDate, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    } catch (Exception e) {
      throw new RuntimeException("Date parsing error for field: " + fieldName);
    }
  }
}

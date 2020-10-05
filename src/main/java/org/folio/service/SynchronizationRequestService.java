package org.folio.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang.BooleanUtils.isTrue;
import static org.apache.commons.lang3.math.NumberUtils.DOUBLE_ZERO;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.domain.MultipleRecords;
import org.folio.domain.SynchronizationStatus;
import org.folio.exception.EntityNotFoundException;
import org.folio.repository.SynchronizationRequestRepository;
import org.folio.rest.client.FeesFinesClient;
import org.folio.rest.client.LoansClient;
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
import org.folio.rest.jaxrs.model.SynchronizationJob;
import org.folio.rest.jaxrs.model.SynchronizationRequest;
import org.folio.rest.jaxrs.model.SynchronizationResponse;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SynchronizationRequestService {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String FULL_SCOPE = "full";
  private static final String USER_SCOPE = "user";
  private static final String DECLARED_LOST_STATUS = "Declared lost";
  private static final String CLAIMED_RETURNED_STATUS = "Claimed returned";

  private final SynchronizationRequestRepository syncRepository;
  private final EventHandler<ItemCheckedOutEvent> checkedOutEventHandler;
  private final EventHandler<ItemDeclaredLostEvent> declaredLostEventHandler;
  private final EventHandler<ItemClaimedReturnedEvent> claimedReturnedEventHandler;
  private final EventHandler<LoanDueDateChangedEvent> dueDateChangedEventHandler;
  private final EventHandler<FeeFineBalanceChangedEvent> feeFineBalanceChangedEventHandler;
  private final LoansClient loansClient;
  private final FeesFinesClient feesFinesClient;
  private final EventService eventService;
  private final String tenantId;

  public SynchronizationRequestService(Map<String, String> okapiHeaders, Vertx vertx) {
    tenantId = TenantTool.tenantId(okapiHeaders);
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    syncRepository = new SynchronizationRequestRepository(postgresClient);
    loansClient = new LoansClient(vertx, okapiHeaders);
    eventService = new EventService(postgresClient);
    feesFinesClient = new FeesFinesClient(vertx, okapiHeaders);
    checkedOutEventHandler = new ItemCheckedOutEventHandler (okapiHeaders, vertx);
    declaredLostEventHandler = new ItemDeclaredLostEventHandler(okapiHeaders, vertx);
    claimedReturnedEventHandler = new ItemClaimedReturnedEventHandler(okapiHeaders, vertx);
    dueDateChangedEventHandler = new LoanDueDateChangedEventHandler(okapiHeaders, vertx);
    feeFineBalanceChangedEventHandler = new FeeFineBalanceChangedEventHandler(okapiHeaders, vertx);
  }

  public Future<SynchronizationResponse> createSyncRequest(SynchronizationRequest request) {
    var syncRecordId = UUID.randomUUID().toString();
    SynchronizationJob entity = new SynchronizationJob()
      .withId(syncRecordId)
      .withStatus(SynchronizationStatus.OPEN.getValue())
      .withScope(request.getScope().value())
      .withUserId(request.getUserId())
      .withTotalNumberOfLoans(DOUBLE_ZERO)
      .withTotalNumberOfFeesFines(DOUBLE_ZERO)
      .withNumberOfProcessedLoans(DOUBLE_ZERO)
      .withNumberOfProcessedFeesFines(DOUBLE_ZERO);

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

     return syncRepository.getJobsByStatus(SynchronizationStatus.IN_PROGRESS)
        .compose(reqList -> doSynchronization(reqList));
  }

  private Future<SynchronizationJob> doSynchronization(
    List<SynchronizationJob> reqList) {

    if (!reqList.isEmpty()) {
      log.debug("Synchronization is in-progress now");
      return succeededFuture();
    }

    return syncRepository.getTheOldestSyncRequest(tenantId)
      .compose(syncJob -> doSynchronization(syncJob));
  }

  private Future<SynchronizationJob> doSynchronization(SynchronizationJob synchronizationJob) {
      return succeededFuture(updateStatusOfJob(synchronizationJob,
        SynchronizationStatus.IN_PROGRESS))
      .compose(syncJob -> cleanExistingEvents(syncJob, tenantId)) // remove all events if scope is all or remove only for specific user
      .compose(syncJob -> createEventsByLoans(syncJob)) // create new events
      .compose(syncJob -> createEventsByFeesFines(syncJob)) // create events by fees/fines
      .onSuccess(syncJob -> updateStatusOfJob(synchronizationJob, SynchronizationStatus.DONE))
      .onFailure(throwable -> updateJobAsFailed(synchronizationJob,
        throwable.getLocalizedMessage()))
      .compose(syncJob -> cleanExistingEvents(syncJob, tenantId));       //remove all events
  }

  private Future<SynchronizationJob> createEventsByFeesFines(SynchronizationJob syncJob) {
    Future<MultipleRecords<Account>> accountRecords = USER_SCOPE.equalsIgnoreCase(syncJob.getScope())
      ? feesFinesClient.findAccountsByUserId(syncJob.getUserId())
      : feesFinesClient.findOpenAccounts();

    return accountRecords
      .compose(records -> generateEventsByAccounts(records))
      .map(syncJob);
  }

  private Future<Void> generateEventsByAccounts(MultipleRecords<Account> records) {
    return CompositeFuture.all(records.getRecords().stream()
        .map(account -> generateFeeFineBalanceChangedEvent(account))
        .collect(Collectors.toList()))
      .mapEmpty();
  }

  private Future<SynchronizationJob> createEventsByLoans(SynchronizationJob syncJob) {
     Future<MultipleRecords<Loan>> loanRecords = USER_SCOPE.equalsIgnoreCase(syncJob.getScope())
        ? loansClient.findOpenLoansByUserId(syncJob.getUserId())
        : loansClient.findOpenLoans();

    return loanRecords
      .compose(records -> generateEventsByLoans(records))
      .map(syncJob);
  }

  private Future<Void> generateEventsByLoans(MultipleRecords<Loan> records) {
    return CompositeFuture.all(records.getRecords().stream()
        .map(loan -> generateEvent(loan))
        .collect(Collectors.toList()))
      .mapEmpty();
  }

  private Future<Void> generateEvent(Loan loan) {

    return checkedOutEventHandler.handle(new ItemCheckedOutEvent()
        .withLoanId(loan.getId())
        .withUserId(loan.getUserId()))
      .compose(v -> generateClaimedReturnedEvent(loan))
      .compose(v -> generateDeclaredLostEvent(loan))
      .compose(v -> generateDueDateChangedEvent(loan));
  }

  private Future<Void> generateClaimedReturnedEvent(Loan loan) {
    if (CLAIMED_RETURNED_STATUS.equalsIgnoreCase(loan.getItemStatus())) {
      return claimedReturnedEventHandler.handle(new ItemClaimedReturnedEvent()
        .withLoanId(loan.getId())
        .withUserId(loan.getUserId()))
        .mapEmpty();
    }
    return succeededFuture(null);
  }

  private Future<Void> generateDeclaredLostEvent(Loan loan) {
    if (DECLARED_LOST_STATUS.equals(loan.getItemStatus())) {
      declaredLostEventHandler.handle(new ItemDeclaredLostEvent()
        .withLoanId(loan.getId())
        .withUserId(loan.getUserId()));
    }
    return succeededFuture(null);
  }

  private Future<Void> generateDueDateChangedEvent(Loan loan) {
    if (isTrue(loan.getDueDateChangedByRecall())) {
      dueDateChangedEventHandler.handle(new LoanDueDateChangedEvent()
        .withLoanId(loan.getId())
        .withUserId(loan.getUserId())
        .withDueDate(loan.getDueDate())
        .withDueDateChangedByRecall(loan.getDueDateChangedByRecall()));
    }
    return succeededFuture(null);
  }

  private Future<Void> generateFeeFineBalanceChangedEvent(Account account) {

    return feeFineBalanceChangedEventHandler.handle(new FeeFineBalanceChangedEvent()
        .withBalance(BigDecimal.valueOf(account.getRemaining()))
        .withFeeFineId(account.getFeeFineId())
        .withUserId(account.getUserId())
        .withLoanId(account.getLoanId()))
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
    syncRepository.update(syncJob, syncJob.getId());

    return syncJob;
  }

  private void updateJobAsFailed(SynchronizationJob syncJob, String errorMessage) {
    syncJob.getErrors().add(errorMessage);
    syncJob.setStatus(SynchronizationStatus.FAILED.getValue());
    syncRepository.update(syncJob, syncJob.getId());
  }
}

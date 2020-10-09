package org.folio.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.domain.SynchronizationStatus.DONE;
import static org.folio.domain.SynchronizationStatus.FAILED;
import static org.folio.domain.SynchronizationStatus.IN_PROGRESS;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.domain.SynchronizationStatus;
import org.folio.exception.EntityNotFoundException;
import org.folio.repository.SynchronizationRequestRepository;
import org.folio.rest.jaxrs.model.SynchronizationJob;
import org.folio.rest.jaxrs.model.SynchronizationRequest;
import org.folio.rest.jaxrs.model.SynchronizationResponse;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SynchronizationRequestService {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String FULL_SCOPE = "full";
  private static final String USER_SCOPE = "user";

  private final SynchronizationRequestRepository syncRepository;
  private final LoanEventsGenerationService loanEventsGenerationService;
  private final FeesFinesEventsGenerationService feesFinesEventsGenerationService;
  private final EventService eventService;
  private final String tenantId;

  public SynchronizationRequestService(Map<String, String> okapiHeaders, Vertx vertx) {
    this.tenantId = TenantTool.tenantId(okapiHeaders);
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    this.syncRepository = new SynchronizationRequestRepository(postgresClient);
    this.eventService = new EventService(postgresClient);
    this.loanEventsGenerationService = new LoanEventsGenerationService(
      okapiHeaders, vertx, syncRepository);
    this.feesFinesEventsGenerationService = new FeesFinesEventsGenerationService(
      okapiHeaders, vertx, syncRepository);
  }

  public Future<SynchronizationResponse> createSyncRequest(SynchronizationRequest request) {
    String syncRecordId = UUID.randomUUID().toString();
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
    System.out.println("SynchronizationRequestService: 80 " + Thread.currentThread().getName());
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
      return succeededFuture(loanEventsGenerationService.updateStatusOfJob(
        synchronizationJob, IN_PROGRESS))
        .compose(syncJob -> cleanExistingEvents(syncJob, tenantId))
        .compose(this::createEventsByLoans)
        .compose(this::createEventsByFeesFines)
        .onFailure(throwable -> updateJobAsFailed(synchronizationJob,
          throwable.getLocalizedMessage()));
  }

  private Future<SynchronizationJob> createEventsByLoans(SynchronizationJob syncJob) {
     String path = USER_SCOPE.equalsIgnoreCase(syncJob.getScope())
       ? String.format("/loan-storage/loans?query=userId=%s and status.name=open", syncJob.getUserId())
       : "/loan-storage/loans?query=status.name=open";

     return loanEventsGenerationService.generateEvents(syncJob, path);
  }

  private Future<SynchronizationJob> createEventsByFeesFines(SynchronizationJob syncJob) {
    String path = USER_SCOPE.equalsIgnoreCase(syncJob.getScope())
      ? String.format("/accounts?query=userId=%s and status.name=open", syncJob.getUserId())
      : "/accounts?query=status.name=open";

    return feesFinesEventsGenerationService.generateEvents(syncJob, path);
  }

  private Future<SynchronizationJob> cleanExistingEvents(SynchronizationJob syncJob,
    String tenantId) {

    String scope = syncJob.getScope();
    if (FULL_SCOPE.equalsIgnoreCase(scope)) {
      return eventService.removeAllEvents(tenantId)
        .map(syncJob);
    }
    return eventService.removeAllEventsForUser(tenantId, syncJob.getUserId())
      .map(syncJob);
  }

  private void updateJobAsFailed(SynchronizationJob syncJob, String errorMessage) {
    syncJob.getErrors().add(errorMessage);
    syncJob.setStatus(FAILED.getValue());
    log.error("Synchronization job failed " + errorMessage);
    syncRepository.update(syncJob, syncJob.getId());
  }
}

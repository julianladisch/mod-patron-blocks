package org.folio.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.math.NumberUtils.DOUBLE_ZERO;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.domain.SynchronizationStatus;
import org.folio.exception.EntityNotFoundException;
import org.folio.repository.SynchronizationRequestRepository;
import org.folio.rest.client.FeesFinesClient;
import org.folio.rest.client.LoansClient;
import org.folio.rest.jaxrs.model.Loan;
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
  private final SynchronizationRequestRepository syncRepository;
  private final LoansClient loansClient;
  private final FeesFinesClient feesFinesClient;
  private final String tenantId;

  public SynchronizationRequestService(Map<String, String> okapiHeaders, Vertx vertx) {
    tenantId = TenantTool.tenantId(okapiHeaders);
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    syncRepository = new SynchronizationRequestRepository(postgresClient);
    loansClient = new LoansClient(vertx, okapiHeaders);
    feesFinesClient = new FeesFinesClient(vertx, okapiHeaders);
  }

  public Future<SynchronizationResponse> createSyncRequest(SynchronizationRequest request) {
    String syncRecordId = UUID.randomUUID().toString();
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
      .map(syncJob -> updateStatusOfRecord(syncJob, SynchronizationStatus.IN_PROGRESS))
      .compose(syncJob -> removeAllEvents(syncJob)) //remove all events
      // create new event
      // process the new event
      // update status of event (done or failed)
      // remove all events
      //.compose()

  }

  private Future<SynchronizationJob> removeAllEvents(SynchronizationJob syncJob) {

    return null;
  }

  private SynchronizationJob updateStatusOfRecord(SynchronizationJob syncJob,
    SynchronizationStatus syncStatus) {

    syncJob.setStatus(syncStatus.getValue());
    syncRepository.update(syncJob, syncJob.getId());

    return syncJob;
  }

  private Future<SynchronizationJob> fillAdditionalFieldsForSyncRetrieve(
    SynchronizationJob syncRetrieve) {

    String userId = null;
    if ("user".equalsIgnoreCase(syncRetrieve.getScope())) {
      userId = parseUserId(syncRetrieve.getScope());
    }
    return addNumberOfLoans(userId)
      .onComplete(addNumberOfFeesFines(userId));

  }

  private Future<SynchronizationJob> addNumberOfLoans(String userId) {
    if (userId != null) {
      Future<Loan> openLoansForUserId = loansClient.findOpenLoansForUserId(userId);
      Loan result = openLoansForUserId.result();
    }
    return null;
  }

  private Future<SynchronizationJob> addNumberOfFeesFines(String userId) {
    return null;
  }

  private String parseUserId(String scope) {
    return StringUtils.substringAfterLast(scope, ":");
  }
}

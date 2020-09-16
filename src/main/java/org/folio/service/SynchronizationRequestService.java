package org.folio.service;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;
import java.util.UUID;

import org.folio.domain.SynchronizationStatus;
import org.folio.repository.SynchronizationRequestRepository;
import org.folio.rest.client.FeesFinesClient;
import org.folio.rest.client.LoansClient;
import org.folio.rest.jaxrs.model.SynchronizationRequest;
import org.folio.rest.jaxrs.model.SynchronizationResponse;
import org.folio.rest.jaxrs.model.SynchronizationRetrieve;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class SynchronizationRequestService {

  private final SynchronizationRequestRepository syncRepository;
  private final LoansClient loansClient;
  private final FeesFinesClient feesFinesClient;
  private int numberOfProcessedLoans = 0;
  private int numberOfProcessedFeesFines = 0;

  public SynchronizationRequestService(Map<String, String> okapiHeaders, Vertx vertx) {
    String tenantId = TenantTool.tenantId(okapiHeaders);
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    syncRepository = new SynchronizationRequestRepository(postgresClient);
    loansClient = new LoansClient(vertx, okapiHeaders);
    feesFinesClient = new FeesFinesClient(vertx, okapiHeaders);
  }

  public void createSyncRequest(SynchronizationRequest entity) {
    String syncRecordId = UUID.randomUUID().toString();
    SynchronizationResponse response = new SynchronizationResponse()
      .withId(syncRecordId)
      .withStatus(SynchronizationStatus.OPEN.name())
      .withScope(entity.getScope());
    syncRepository.save(response, syncRecordId);
  }

  public Future<SynchronizationRetrieve> retrieveSyncRequest(String syncRequestId) {

    return syncRepository.get(syncRequestId)
      .compose(optionalSyncResponse -> optionalSyncResponse
        .map(this::mapToRetrieve)
        .map(this::fillAdditionalFieldsForSyncRetrieve)
        .orElseGet(() -> succeededFuture(new SynchronizationRetrieve())));
  }

  private SynchronizationRetrieve mapToRetrieve(SynchronizationResponse syncResponse) {
    return new SynchronizationRetrieve()
      .withId(syncResponse.getId())
      .withScope(syncResponse.getScope())
      .withStatus(syncResponse.getStatus());
  }

  private Future<SynchronizationRetrieve> fillAdditionalFieldsForSyncRetrieve(
    SynchronizationRetrieve syncRetrieve) {

    String userId = null;
    if (!"full".equalsIgnoreCase(syncRetrieve.getScope())) {
      userId = parseUserId(syncRetrieve.getScope());
    }
    return addNumberOfLoans(userId)
      .onComplete(addNumberOfFeesFines(userId));

  }

  private Future<SynchronizationRetrieve> addNumberOfLoans(String userId) {
    if (userId != null) {
      //loansClient.
    }
    return null;
  }

  private Future<SynchronizationRetrieve> addNumberOfFeesFines(String userId) {
    return null;
  }

  private String parseUserId(String scope) {
    return scope.substring(scope.indexOf(":"));
  }
}

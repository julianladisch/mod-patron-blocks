package org.folio.rest.impl;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.folio.domain.SynchronizationStatus;
import org.folio.rest.jaxrs.model.SynchronizationRequest;
import org.folio.rest.jaxrs.model.SynchronizationResponse;
import org.folio.rest.jaxrs.resource.AutomatedPatronBlocksSynchronizationRequest;
import org.folio.service.SynchronizationRequestService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class AutomatedPatronBlocksSynchronizationRequestAPI implements
  AutomatedPatronBlocksSynchronizationRequest {

  private static final String SYNC_REQUESTS = "sync-requests";

  @Override
  public void postAutomatedPatronBlocksSynchronizationRequest(SynchronizationRequest entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    SynchronizationRequestService syncService =
      new SynchronizationRequestService(okapiHeaders, vertxContext.owner());
    syncService.createSyncRequest(entity);

    SynchronizationResponse response = buildSynchronizationResponse(entity,
      SynchronizationStatus.OPEN);
    asyncResultHandler.handle(Future.succeededFuture(
      PostAutomatedPatronBlocksSynchronizationRequestResponse
        .respond201WithApplicationJson(response)));


//    PgUtil.post(SYNC_REQUESTS, entity, okapiHeaders, vertxContext,
//      PostAutomatedPatronBlocksSynchronizationRequestResponse.class, asyncResultHandler);
//
//    entity.getScope()
  }

  @Override
  public void getAutomatedPatronBlocksSynchronizationRequestBySyncRequestId(
    String syncRequestId, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

//    PgUtil.getById(SYNC_REQUESTS, SynchronizationRetrieve.class,
//      syncRequestId, okapiHeaders, vertxContext,
//      GetAutomatedPatronBlocksSynchronizationRequestBySyncRequestIdResponse.class,
//      asyncResultHandler);
    SynchronizationRequestService syncService =
      new SynchronizationRequestService(okapiHeaders, vertxContext.owner());
    syncService.retrieveSyncRequest(syncRequestId)
      .onSuccess(response ->
        asyncResultHandler.handle(succeededFuture(
          GetAutomatedPatronBlocksSynchronizationRequestBySyncRequestIdResponse
            .respond200WithApplicationJson(response))))
      .onFailure(response -> asyncResultHandler.handle(failedFuture(
        "This synchronization request does not exist")));


  }

  private SynchronizationResponse buildSynchronizationResponse(SynchronizationRequest entity,
    SynchronizationStatus status) {

    return new SynchronizationResponse()
      .withId(UUID.randomUUID().toString())
      .withScope(entity.getScope())
      .withStatus(status.toString());
  }
}

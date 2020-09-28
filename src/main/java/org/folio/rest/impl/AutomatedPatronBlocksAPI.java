package org.folio.rest.impl;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static org.folio.util.UuidUtil.isUuid;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.exception.EntityNotFoundException;
import org.folio.rest.jaxrs.model.SynchronizationRequest;
import org.folio.rest.jaxrs.resource.AutomatedPatronBlocks;
import org.folio.service.PatronBlocksService;
import org.folio.service.SynchronizationRequestService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class AutomatedPatronBlocksAPI implements AutomatedPatronBlocks {

  @Override
  public void getAutomatedPatronBlocksByUserId(String userId,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    if (!isUuid(userId)) {
      asyncResultHandler.handle(succeededFuture(GetAutomatedPatronBlocksByUserIdResponse
        .respond400WithTextPlain(format("Invalid user UUID: \"%s\"", userId))));
      return;
    }

    new PatronBlocksService(okapiHeaders, vertxContext.owner())
      .getBlocksForUser(userId)
      .onSuccess(blocks -> asyncResultHandler.handle(succeededFuture(
        GetAutomatedPatronBlocksByUserIdResponse.respond200WithApplicationJson(blocks))))
      .onFailure(failure -> asyncResultHandler.handle(succeededFuture(
        GetAutomatedPatronBlocksByUserIdResponse.respond500WithTextPlain(
          failure.getLocalizedMessage()))));
  }

  @Override
  public void postAutomatedPatronBlocksSynchronizationRequest(SynchronizationRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new SynchronizationRequestService(okapiHeaders, vertxContext.owner())
      .createSyncRequest(request)
      .onSuccess(response -> asyncResultHandler.handle(succeededFuture(
        PostAutomatedPatronBlocksSynchronizationRequestResponse
          .respond201WithApplicationJson(response))))
      .onFailure(failure -> asyncResultHandler.handle(succeededFuture(
        PostAutomatedPatronBlocksSynchronizationRequestResponse
          .respond500WithTextPlain(failure.getLocalizedMessage()))));
  }

  @Override
  public void getAutomatedPatronBlocksSynchronizationRequestBySyncRequestId(String syncRequestId,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new SynchronizationRequestService(okapiHeaders, vertxContext.owner())
      .retrieveSyncRequest(syncRequestId)
      .onSuccess(response ->
        asyncResultHandler.handle(succeededFuture(
          GetAutomatedPatronBlocksSynchronizationRequestBySyncRequestIdResponse
            .respond200WithApplicationJson(response))))
      .onFailure(throwable -> {
        String errorMessage = throwable.getLocalizedMessage();
        if (throwable instanceof EntityNotFoundException) {
          asyncResultHandler.handle(succeededFuture(
            GetAutomatedPatronBlocksSynchronizationRequestBySyncRequestIdResponse
              .respond404WithTextPlain(errorMessage)));
        } else {
          GetAutomatedPatronBlocksSynchronizationRequestBySyncRequestIdResponse
            .respond500WithTextPlain(errorMessage);
        }
      });
  }

  @Override
  public void postAutomatedPatronBlocksSynchronizationRun(Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    new SynchronizationRequestService(okapiHeaders, vertxContext.owner())
      .runSynchronization()
      .onSuccess(response -> asyncResultHandler.handle(succeededFuture(
        GetAutomatedPatronBlocksSynchronizationRequestBySyncRequestIdResponse
          .respond200WithApplicationJson(response))))
      .onFailure(throwable -> {
        String errorMessage = throwable.getLocalizedMessage();
        GetAutomatedPatronBlocksSynchronizationRequestBySyncRequestIdResponse
          .respond500WithTextPlain(errorMessage);
      });
  }
}

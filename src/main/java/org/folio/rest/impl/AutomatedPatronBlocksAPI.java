package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static org.folio.repository.SynchronizationJobRepository.SYNCHRONIZATION_JOBS_TABLE;
import static org.folio.util.UuidUtil.isUuid;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.exception.EntityNotFoundException;
import org.folio.rest.jaxrs.model.SynchronizationJob;
import org.folio.rest.jaxrs.resource.AutomatedPatronBlocks;
import org.folio.rest.persist.PgUtil;
import org.folio.service.PatronBlocksService;
import org.folio.service.SynchronizationRequestService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AutomatedPatronBlocksAPI implements AutomatedPatronBlocks {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
  public void postAutomatedPatronBlocksSynchronizationJob(SynchronizationJob request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    if (SynchronizationJob.Scope.USER == request.getScope() && request.getUserId() == null) {
      asyncResultHandler.handle(succeededFuture(
        PostAutomatedPatronBlocksSynchronizationJobResponse.respond422WithTextPlain(
          "UserId is required for synchronization job with scope: USER")));
      return;
    }

    PgUtil.post(SYNCHRONIZATION_JOBS_TABLE, request, okapiHeaders, vertxContext,
      PostAutomatedPatronBlocksSynchronizationJobResponse.class, asyncResultHandler);
  }

  @Override
  public void getAutomatedPatronBlocksSynchronizationJobBySyncJobId(String syncRequestId,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new SynchronizationRequestService(okapiHeaders, vertxContext.owner())
      .getSynchronizationJob(syncRequestId)
      .onSuccess(response ->
          asyncResultHandler.handle(succeededFuture(
            GetAutomatedPatronBlocksSynchronizationJobBySyncJobIdResponse
              .respond200WithApplicationJson(response))))
      .onFailure(throwable -> {
        String errorMessage = throwable.getLocalizedMessage();
        if (throwable instanceof EntityNotFoundException) {
          asyncResultHandler.handle(succeededFuture(
            GetAutomatedPatronBlocksSynchronizationJobBySyncJobIdResponse
              .respond404WithTextPlain(errorMessage)));
        } else {
          GetAutomatedPatronBlocksSynchronizationJobBySyncJobIdResponse
            .respond500WithTextPlain(errorMessage);
        }
      });
  }

  @Override
  public void postAutomatedPatronBlocksSynchronizationStart(Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    asyncResultHandler.handle(succeededFuture(
      PostAutomatedPatronBlocksSynchronizationStartResponse.respond202()));

    vertxContext.owner().executeBlocking(promise ->
      new SynchronizationRequestService(okapiHeaders, vertxContext.owner())
        .runSynchronization()
        .onComplete(v -> promise.complete()),
      response -> {
        if (response.failed()) {
          log.error("Synchronization error processing");
        } else {
          log.info("Synchronization has been completed");
        }
    });
  }
}

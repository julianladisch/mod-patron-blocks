package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.domain.Event;
import org.folio.domain.EventType;
import org.folio.rest.handlers.ItemAgedToLostEventHandler;
import org.folio.rest.handlers.ItemCheckedInEventHandler;
import org.folio.rest.handlers.ItemCheckedOutEventHandler;
import org.folio.rest.handlers.ItemClaimedReturnedEventHandler;
import org.folio.rest.handlers.LoanDueDateChangedEventHandler;
import org.folio.rest.handlers.ItemDeclaredLostEventHandler;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.ItemAgedToLostEvent;
import org.folio.rest.jaxrs.model.ItemCheckedInEvent;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.jaxrs.resource.AutomatedPatronBlocksHandlers;
import org.folio.rest.handlers.FeeFineBalanceChangedEventHandler;
import org.folio.rest.persist.PgExceptionUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;

public class EventHandlersAPI implements AutomatedPatronBlocksHandlers {
  private static final Logger log = LogManager.getLogger(EventHandlersAPI.class);

  @Override
  public void postAutomatedPatronBlocksHandlersFeeFineBalanceChanged(
    FeeFineBalanceChangedEvent event, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    logEventReceived(event);

    handleOperationResult(
      new FeeFineBalanceChangedEventHandler(okapiHeaders, vertxContext.owner())
        .handle(event), asyncResultHandler);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemCheckedOut(ItemCheckedOutEvent event,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    logEventReceived(event);

    handleOperationResult(
      new ItemCheckedOutEventHandler(okapiHeaders, vertxContext.owner()).handle(event),
      asyncResultHandler);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemCheckedIn(ItemCheckedInEvent event,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    logEventReceived(event);

    handleOperationResult(new ItemCheckedInEventHandler(okapiHeaders, vertxContext.owner())
      .handle(event), asyncResultHandler);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemDeclaredLost(ItemDeclaredLostEvent event,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    logEventReceived(event);

    handleOperationResult(
      new ItemDeclaredLostEventHandler(okapiHeaders, vertxContext.owner()).handle(event),
      asyncResultHandler);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemAgedToLost(ItemAgedToLostEvent event,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    logEventReceived(event);

    handleOperationResult(
      new ItemAgedToLostEventHandler(okapiHeaders, vertxContext.owner()).handle(event),
      asyncResultHandler);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemClaimedReturned(
    ItemClaimedReturnedEvent event, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    logEventReceived(event);

    handleOperationResult(new ItemClaimedReturnedEventHandler(okapiHeaders, vertxContext.owner())
      .handle(event), asyncResultHandler);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersLoanDueDateChanged(LoanDueDateChangedEvent event,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    logEventReceived(event);

    handleOperationResult(new LoanDueDateChangedEventHandler(okapiHeaders, vertxContext.owner())
      .handle(event), asyncResultHandler);
  }

  private void handleOperationResult(Future<String> operation,
    Handler<AsyncResult<Response>> asyncResultHandler) {
    operation.onFailure(throwable -> {
      if (PgExceptionUtil.isVersionConflict(throwable)) {
        asyncResultHandler.handle(succeededFuture(
          PostAutomatedPatronBlocksHandlersFeeFineBalanceChangedResponse.respond409WithTextPlain(
            throwable.getCause())));
      } else {
        asyncResultHandler.handle(succeededFuture(
          PostAutomatedPatronBlocksHandlersFeeFineBalanceChangedResponse.respond500WithTextPlain(
            throwable.getCause())));
      }
    })
      .onSuccess(id ->
        asyncResultHandler.handle(succeededFuture(
          PostAutomatedPatronBlocksHandlersFeeFineBalanceChangedResponse.respond204())));
  }

  private static void logEventReceived(Event event) {
    log.info("Received {} event with payload:\n{}",
      EventType.getNameByEvent(event),
      Json.encodePrettily(event));
  }
}

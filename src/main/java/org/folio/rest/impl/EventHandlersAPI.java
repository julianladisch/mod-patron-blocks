package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.domain.Event;
import org.folio.domain.EventType;
import org.folio.rest.handlers.FeeFineBalanceChangedEventHandler;
import org.folio.rest.handlers.ItemAgedToLostEventHandler;
import org.folio.rest.handlers.ItemCheckedInEventHandler;
import org.folio.rest.handlers.ItemCheckedOutEventHandler;
import org.folio.rest.handlers.ItemClaimedReturnedEventHandler;
import org.folio.rest.handlers.ItemDeclaredLostEventHandler;
import org.folio.rest.handlers.LoanDueDateChangedEventHandler;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.ItemAgedToLostEvent;
import org.folio.rest.jaxrs.model.ItemCheckedInEvent;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.jaxrs.resource.AutomatedPatronBlocksHandlers;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.util.EventProcessingResultAdapter;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;

public class EventHandlersAPI implements AutomatedPatronBlocksHandlers {
  private static final Logger log = LogManager.getLogger(EventHandlersAPI.class);

  @Override
  public void postAutomatedPatronBlocksHandlersFeeFineBalanceChanged(
    FeeFineBalanceChangedEvent event, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    logEventReceived(event);

    new FeeFineBalanceChangedEventHandler(okapiHeaders, vertxContext.owner())
      .handle(event, true)
      .onComplete(result -> handleOperationResult(result, asyncResultHandler,
        EventType.FEE_FINE_BALANCE_CHANGED));
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemCheckedOut(ItemCheckedOutEvent event,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    logEventReceived(event);

    new ItemCheckedOutEventHandler(okapiHeaders, vertxContext.owner())
      .handle(event, true)
      .onComplete(
        result -> handleOperationResult(result, asyncResultHandler, EventType.ITEM_CHECKED_OUT));
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemCheckedIn(ItemCheckedInEvent event,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    logEventReceived(event);
    new ItemCheckedInEventHandler(okapiHeaders, vertxContext.owner())
      .handle(event, true)
      .onComplete(
        result -> handleOperationResult(result, asyncResultHandler, EventType.ITEM_CHECKED_IN));
    ;
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemDeclaredLost(ItemDeclaredLostEvent event,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    logEventReceived(event);

    new ItemDeclaredLostEventHandler(okapiHeaders, vertxContext.owner())
      .handle(event, true)
      .onComplete(
        result -> handleOperationResult(result, asyncResultHandler, EventType.ITEM_DECLARED_LOST));
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemAgedToLost(ItemAgedToLostEvent event,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    logEventReceived(event);

    new ItemAgedToLostEventHandler(okapiHeaders, vertxContext.owner())
      .handle(event, true)
      .onComplete(
        result -> handleOperationResult(result, asyncResultHandler, EventType.ITEM_AGED_TO_LOST));
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemClaimedReturned(
    ItemClaimedReturnedEvent event, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    logEventReceived(event);
    new ItemClaimedReturnedEventHandler(okapiHeaders, vertxContext.owner())
      .handle(event, true)
      .onComplete(result -> handleOperationResult(result, asyncResultHandler,
        EventType.ITEM_CLAIMED_RETURNED));
  }

  @Override
  public void postAutomatedPatronBlocksHandlersLoanDueDateChanged(LoanDueDateChangedEvent event,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    logEventReceived(event);

    new LoanDueDateChangedEventHandler(okapiHeaders, vertxContext.owner())
      .handle(event, true)
      .onComplete(result -> handleOperationResult(result, asyncResultHandler,
        EventType.LOAN_DUE_DATE_CHANGED));
  }

  private void handleOperationResult(AsyncResult<String> asyncResult,
    Handler<AsyncResult<Response>> asyncResultHandler, EventType eventType) {
    EventProcessingResultAdapter eventProcessingResultAdapter =
      eventType.getEventProcessingResultAdapter();
    if (asyncResult.failed()) {
      Throwable throwable = asyncResult.cause();
      if (PgExceptionUtil.isVersionConflict(throwable)) {
        asyncResultHandler.handle(
          succeededFuture(eventProcessingResultAdapter.respond409.apply(throwable)));
      } else {
        asyncResultHandler.handle(succeededFuture(
          eventProcessingResultAdapter.respond500.apply(throwable)));
      }
    } else if (asyncResult.succeeded()) {
      asyncResultHandler.handle(succeededFuture(eventProcessingResultAdapter.respond204.get()));
    }
  }

  private static void logEventReceived(Event event) {
    log.info("Received {} event with payload:\n{}",
      EventType.getNameByEvent(event),
      Json.encodePrettily(event));
  }
}

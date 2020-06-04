package org.folio.rest.impl;

import static org.folio.domain.EventType.FEE_FINE_BALANCE_CHANGED;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.domain.EventType;
import org.folio.rest.jaxrs.resource.AutomatedPatronBlocksHandlers;
import org.folio.rest.handlers.FeeFineBalanceChangedEventHandler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EventHandlersAPI implements AutomatedPatronBlocksHandlers {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void postAutomatedPatronBlocksHandlersFeeFineBalanceChanged(String payload,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    asyncResultHandler.handle(Future.succeededFuture(
      PostAutomatedPatronBlocksHandlersFeeFineBalanceChangedResponse.respond204()));

    logEventReceived(FEE_FINE_BALANCE_CHANGED, payload);

    new FeeFineBalanceChangedEventHandler(okapiHeaders, vertxContext.owner())
      .handle(payload);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemCheckedOut(String payload,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    asyncResultHandler.handle(Future.succeededFuture(
      PostAutomatedPatronBlocksHandlersItemCheckedOutResponse.respond204()));

    logEventReceived(EventType.ITEM_CHECKED_OUT, payload);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemCheckedIn(String payload,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    asyncResultHandler.handle(Future.succeededFuture(
      PostAutomatedPatronBlocksHandlersItemCheckedInResponse.respond204()));

    logEventReceived(EventType.ITEM_CHECKED_IN, payload);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemDeclaredLost(String payload,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    asyncResultHandler.handle(Future.succeededFuture(
      PostAutomatedPatronBlocksHandlersItemDeclaredLostResponse.respond204()));

    logEventReceived(EventType.ITEM_DECLARED_LOST, payload);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersLoanDueDateChanged(String payload,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    asyncResultHandler.handle(Future.succeededFuture(
      PostAutomatedPatronBlocksHandlersLoanDueDateChangedResponse.respond204()));

    logEventReceived(EventType.LOAN_DUE_DATE_CHANGED, payload);
  }

  private static void logEventReceived(EventType eventType, String payload) {
    log.info("Received {} event with payload:\n{}", eventType.name(), payload);
  }
}

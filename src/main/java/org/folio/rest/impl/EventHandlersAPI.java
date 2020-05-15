package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.resource.AutomatedPatronBlocksHandlers;
import org.folio.service.EventConsumerService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class EventHandlersAPI implements AutomatedPatronBlocksHandlers{

  @Override
  public void postAutomatedPatronBlocksHandlersFeefineBalanceChanged(String entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {


    asyncResultHandler.handle(Future.succeededFuture(
      PostAutomatedPatronBlocksHandlersFeefineBalanceChangedResponse.respond204()));


    new EventConsumerService().handleFeefineBalanceChangedEvent(entity);
  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemCheckedOut(String entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemCheckedIn(String entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

  }

  @Override
  public void postAutomatedPatronBlocksHandlersItemDeclaredLost(String entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

  }

  @Override
  public void postAutomatedPatronBlocksHandlersLoanDueDateUpdated(String entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

  }
}

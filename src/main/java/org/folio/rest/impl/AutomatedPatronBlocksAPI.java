package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.resource.AutomatedPatronBlocksUserId;
import org.folio.service.PatronBlocksService;
import org.folio.util.UuidUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class AutomatedPatronBlocksAPI implements AutomatedPatronBlocksUserId {

  @Override public void getAutomatedPatronBlocksByUserId(String userId,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    if (!UuidUtil.isUuid(userId)) {
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
}

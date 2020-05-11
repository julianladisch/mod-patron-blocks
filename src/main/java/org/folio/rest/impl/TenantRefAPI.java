package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TenantRefAPI extends TenantAPI {
  private static final Logger log = LoggerFactory.getLogger(TenantRefAPI.class);

  @Override
  public void postTenant(TenantAttributes tenantAttributes,
                         Map<String, String> headers,
                         Handler<AsyncResult<Response>> handler, Context context) {

    log.info("postTenant");
    log.info("Tenant attributes: {}", JsonObject.mapFrom(tenantAttributes));

    final Vertx vertx = context.owner();

    super.postTenant(tenantAttributes, headers, res -> {
      if (res.failed()) {
        handler.handle(res);
        return;
      }

      vertx.executeBlocking(
        promise ->
          PubSubClientUtils.registerModule(new OkapiConnectionParams(headers, vertx))
            .whenComplete((result, throwable) -> {
              if (isTrue(result) && throwable == null) {
                log.info("Module was successfully registered as subscriber in mod-pubsub");
                promise.complete(result);
              } else {
                log.error("Error during module registration in mod-pubsub", throwable);
                promise.fail(throwable);
              }
            }),
        registration -> {
          if (registration.failed()) {
            log.error("postTenant failure", registration.cause());
            handler.handle(succeededFuture(PostTenantResponse
              .respond500WithTextPlain(registration.cause().getLocalizedMessage())));
          } else {
            log.info("postTenant executed successfully");
            handler.handle(succeededFuture(PostTenantResponse
              .respond201WithApplicationJson(EMPTY)));
          }
        }
      );
    }, context);
  }
}

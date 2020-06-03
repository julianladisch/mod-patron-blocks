package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.folio.HttpStatus;
import org.folio.domain.EventType;
import org.folio.rest.client.PubsubClient;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TenantRefAPI extends TenantAPI {
  private static final Logger log = LoggerFactory.getLogger(TenantRefAPI.class);

  @Override
  public void postTenant(TenantAttributes tenantAttributes, Map<String, String> headers,
    Handler<AsyncResult<Response>> handler, Context context) {

    log.info("postTenant called with tenant attributes: {}", JsonObject.mapFrom(tenantAttributes));

    super.postTenant(tenantAttributes, headers, res -> {
      if (res.failed()) {
        handler.handle(res);
        return;
      }

      PubSubClientUtils.registerModule(new OkapiConnectionParams(headers, context.owner()))
        .whenComplete((result, throwable) -> {
          if (isTrue(result) && throwable == null) {
            log.info("postTenant executed successfully");
            handler.handle(succeededFuture(PostTenantResponse
              .respond201WithApplicationJson(EMPTY)));
          } else {
            log.error("postTenant execution failed", throwable);
            handler.handle(succeededFuture(PostTenantResponse
              .respond500WithTextPlain(throwable.getLocalizedMessage())));
          }
        });
    }, context);
  }

  @Override public void deleteTenant(Map<String, String> headers,
    Handler<AsyncResult<Response>> handler, Context context) {
    super.deleteTenant(headers, ar -> unregisterModuleFromPubsub(headers, context.owner())
      .onComplete(result -> {
        if (result.failed()) {
          log.error("deleteTenant execution failed", result.cause());
          handler.handle(succeededFuture(DeleteTenantResponse
            .respond500WithTextPlain(result.cause().getLocalizedMessage())));
        } else {
          Map<EventType, Integer> eventToStatus = result.result();
          Optional<Integer> failedEvent = eventToStatus.values().stream()
            .filter(status -> status != HttpStatus.HTTP_NO_CONTENT.toInt())
            .findAny();

          if (failedEvent.isPresent()) {
            String message = "Failed to unregister. Event types: " + eventToStatus.toString();
            log.error("deleteTenant execution failed: " + message);
            handler.handle(
              succeededFuture(DeleteTenantResponse.respond500WithTextPlain(message)));
          } else {
            log.info("deleteTenant executed successfully");
            handler.handle(succeededFuture(DeleteTenantResponse.respond204()));
          }
        }
      }), context);
  }

  public static Future<Map<EventType, Integer>> unregisterModuleFromPubsub(
    Map<String, String> headers, Vertx vertx) {

    OkapiConnectionParams params = new OkapiConnectionParams(headers, vertx);
    PubsubClient client = new PubsubClient(params.getOkapiUrl(), params.getTenantId(),
      params.getToken());

    List<Future> allFutures = new ArrayList<>();
    Map<EventType, Integer> eventToResponseStatus = new EnumMap<>(EventType.class);

    try {
      for (EventType eventType: EventType.values()) {
        Promise<EventType> subscriberUnregisteringPromise = Promise.promise();
        allFutures.add(subscriberUnregisteringPromise.future());
        Promise<EventType> publisherUnregisteringPromise = Promise.promise();
        allFutures.add(publisherUnregisteringPromise.future());

        client.deletePubsubEventTypesSubscribersByEventTypeName(
          eventType.name(), PubSubClientUtils.constructModuleName(), ar ->
            handleUnregisteringResponse(eventType, eventToResponseStatus, ar,
              subscriberUnregisteringPromise, "subscriber"));

        client.deletePubsubEventTypesPublishersByEventTypeName(
          eventType.name(), PubSubClientUtils.constructModuleName(), ar ->
            handleUnregisteringResponse(eventType, eventToResponseStatus, ar,
              publisherUnregisteringPromise, "publisher"));
      }
    } catch (Exception exception) {
      log.error("Failed to unregister module from PubSub", exception);
      return Future.failedFuture(exception);
    }

    return CompositeFuture.all(allFutures)
      .map(r -> eventToResponseStatus);
  }

  private static void handleUnregisteringResponse(EventType eventType,
    Map<EventType, Integer> eventToResponseStatus, HttpClientResponse response,
    Promise<EventType> promise, String role) {

    int statusCode = response.statusCode();
    if (statusCode != HttpStatus.HTTP_NO_CONTENT.toInt()) {
      log.error("Failed to unregister as {} {}. Response status: {}",
        eventType.name(), role, statusCode);
    } else {
      log.info("Successfully unregistered as {} {}. Response status: {}",
        eventType.name(), role, statusCode);
    }
    eventToResponseStatus.put(eventType, statusCode);
    promise.complete(eventType);
  }

}

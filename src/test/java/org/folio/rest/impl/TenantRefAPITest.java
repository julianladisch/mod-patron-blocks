package org.folio.rest.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.concurrent.CompletableFuture;

import org.apache.http.HttpStatus;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;
import org.folio.util.pubsub.exceptions.ModuleRegistrationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
@PrepareForTest(PubSubClientUtils.class)
public class TenantRefAPITest extends APITests {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @Test
  public void shouldSucceedWhenRegisteredInPubsub(final TestContext context) {
    Async async = context.async();

    mockStatic(PubSubClientUtils.class);
    when(PubSubClientUtils.registerModule(any(OkapiConnectionParams.class)))
      .thenReturn(CompletableFuture.completedFuture(true));

    try {
      tenantClient.postTenant(getTenantAttributes(), result -> {
        context.assertEquals(HttpStatus.SC_CREATED, result.statusCode());
        // start verifying behavior
        PowerMockito.verifyStatic(PubSubClientUtils.class);
        // verify that module registration method was invoked
        PubSubClientUtils.registerModule(any(OkapiConnectionParams.class));
        async.complete();
      });
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void shouldFailWhenRegistrationInPubsubFails(TestContext context) {
    Async async = context.async();

    String errorMessage = "Module registration failed";
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    future.completeExceptionally(new ModuleRegistrationException(errorMessage));

    mockStatic(PubSubClientUtils.class);
    when(PubSubClientUtils.registerModule(any(OkapiConnectionParams.class)))
      .thenReturn(future);

    try {
      tenantClient.postTenant(getTenantAttributes(), response -> {
        context.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.statusCode());
        response.bodyHandler(body -> {
          context.assertEquals(errorMessage, body.toString());
          async.complete();
        });
      });
    } catch (Exception e) {
      context.fail(e);
    }
  }

}
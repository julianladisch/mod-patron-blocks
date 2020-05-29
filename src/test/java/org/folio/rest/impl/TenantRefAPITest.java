package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import org.folio.rest.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TenantRefAPITest extends TestBase {

  @Test
  public void postTenantShouldFailWhenRegistrationInPubsubFailed(TestContext context) {
    Async async = context.async();

    wireMock.stubFor(post(urlPathMatching("/pubsub/.+"))
      .willReturn(aResponse().withStatus(500).withBody("Module registration failed")));

    try {
      tenantClient.postTenant(getTenantAttributes(), response -> {
        context.assertEquals(500, response.statusCode());
        response.bodyHandler(body -> {
          context.assertTrue(body.toString().contains(
            "Module's publishers were not registered in PubSub"));
          async.complete();
        });
      });
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void deleteTenantShouldSucceedWhenSuccessfullyUnsubscribedFromPubSub(TestContext context) {
    Async async = context.async();

    wireMock.stubFor(delete(urlPathMatching("/pubsub/event-types/.+/subscribers"))
      .willReturn(aResponse().withStatus(204)));

    try {
      tenantClient.deleteTenant(response -> {
        context.assertEquals(204, response.statusCode());
        async.complete();
      });
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void deleteTenantShouldFailWhenFailedToUnsubscribeFromPubSub(TestContext context) {
    Async async = context.async();

    wireMock.stubFor(delete(urlPathEqualTo("/pubsub/event-types/ITEM_CHECKED_IN/subscribers"))
      .atPriority(1)
      .willReturn(aResponse().withStatus(500)));
    wireMock.stubFor(delete(urlPathEqualTo("/pubsub/event-types/ITEM_CHECKED_OUT/subscribers"))
      .atPriority(1)
      .willReturn(aResponse().withStatus(400)));
    wireMock.stubFor(delete(urlPathMatching("/pubsub/event-types/\\w+/subscribers"))
      .atPriority(10)
      .willReturn(aResponse().withStatus(204)));

    try {
      tenantClient.deleteTenant(response -> {
        context.assertEquals(500, response.statusCode());
        response.bodyHandler(body -> context.assertTrue(body.toString()
          .startsWith("deleteTenant execution failed: Failed to unsubscribe from events")));
        async.complete();
      });
    } catch (Exception e) {
      context.fail(e);
    }
  }

}
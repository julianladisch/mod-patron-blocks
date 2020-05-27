package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
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
  public void shouldFailWhenRegistrationInPubsubFails(TestContext context) {
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

}
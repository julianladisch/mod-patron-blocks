package org.folio.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.okapi.common.XOkapiHeaders.TOKEN;
import static org.folio.okapi.common.XOkapiHeaders.URL;

import java.util.HashMap;
import java.util.Map;

import org.folio.rest.TestBase;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ConfigurationsClientTest extends TestBase {
  private static final String TIMEZONE = "Asia/Taipei";

  private static final ConfigurationsClient configurationsClient;

  static {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(URL, getMockedOkapiUrl());
    okapiHeaders.put(TENANT, OKAPI_TENANT);
    okapiHeaders.put(TOKEN, OKAPI_TOKEN);

    configurationsClient = new ConfigurationsClient(vertx, okapiHeaders);
  }

  @Test
  public void timezoneIsFetchedAndParsed(TestContext context) {
    Async async = context.async();

    mockUsersResponse(200, makeValidConfigurationResponseBody());

    configurationsClient.findTimeZone()
      .onFailure(context::fail)
      .onSuccess(dateTimeZone -> {
        context.assertEquals(DateTimeZone.forID(TIMEZONE), dateTimeZone);
        async.complete();
      });
  }

  @Test
  public void defaultTimezoneShouldBeUTC(TestContext context) {
    Async async = context.async();

    mockUsersResponse(200, makeEmptyConfigurationResponseBody());

    configurationsClient.findTimeZone()
      .onFailure(context::fail)
      .onSuccess(dateTimeZone -> {
        context.assertEquals(DateTimeZone.UTC, dateTimeZone);
        async.complete();
      });
  }

  @Test
  public void shouldFailWhenResponseStatusIsNot200(TestContext context) {
    mockUsersResponse(400, "");

    configurationsClient.findTimeZone()
      .onSuccess(dateTimeZone -> context.fail());
  }

  @Test
  public void shouldFailWhenResponseBodyIsInvalidJSON(TestContext context) {
    mockUsersResponse(200, "Invalid JSON");

    configurationsClient.findTimeZone()
      .onSuccess(dateTimeZone -> context.fail());
  }

  @Test
  public void shouldFailWhenTimezoneIsInvalid(TestContext context) {
    mockUsersResponse(200, makeInvalidConfigurationResponseBody());

    configurationsClient.findTimeZone()
      .onSuccess(dateTimeZone -> context.fail());
  }

  private void mockUsersResponse(int responseStatus, String responseBody) {
    wireMock.stubFor(get(urlMatching("/configurations/entries.*"))
      .atPriority(5)
      .willReturn(aResponse()
        .withStatus(responseStatus)
        .withBody(responseBody)));
  }

  private static String makeEmptyConfigurationResponseBody() {
    return new JsonObject()
      .put("configs", new JsonArray(singletonList(
        new JsonObject()
          .put("value", "")
      )))
      .encodePrettily();
  }

  private static String makeValidConfigurationResponseBody() {
    return new JsonObject()
      .put("configs", new JsonArray(singletonList(
        new JsonObject()
          .put("value",
            format("{\"locale\":\"en-GB\",\"timezone\":\"%s\",\"currency\":\"USD\"}", TIMEZONE))
      )))
      .encodePrettily();
  }

  private static String makeInvalidConfigurationResponseBody() {
    return new JsonObject()
      .put("configs", new JsonArray(singletonList(
        new JsonObject()
          .put("value",
            "{\"locale\":\"en-GB\",\"timezone\":\"not-a-timezone\",\"currency\":\"USD\"}")
      )))
      .encodePrettily();
  }
}

package org.folio.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

import java.util.HashMap;
import java.util.Map;

import javax.validation.ValidationException;

import org.folio.exception.EntityNotFoundException;
import org.folio.rest.RestVerticle;
import org.folio.rest.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class UsersClientTest extends TestBase {
  private static final String USER_ID = randomId();
  private static final String PATRON_GROUP_ID = randomId();
  private static final String OKAPI_HEADER_URL = "x-okapi-url";
  private static final UsersClient usersClient;

  static {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_HEADER_URL, getMockedOkapiUrl());
    okapiHeaders.put(OKAPI_HEADER_TENANT, OKAPI_TENANT);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TOKEN, OKAPI_TOKEN);

    usersClient = new UsersClient(vertx, okapiHeaders);
  }

  @Test
  public void getPatronGroupByExistingUserId(TestContext context) {
    Async async = context.async();

    mockUsersResponse(200, new JsonObject()
      .put("id", USER_ID)
      .put("patronGroup", PATRON_GROUP_ID)
      .encodePrettily());

    usersClient.findPatronGroupIdForUser(USER_ID)
      .onFailure(context::fail)
      .onSuccess(groupId -> {
        context.assertEquals(PATRON_GROUP_ID, groupId);
        async.complete();
      });
  }

  @Test
  public void getPatronGroupByNonExistentUserId(TestContext context) {
    Async async = context.async();

    String userId = randomId();
    int responseCode = 404;
    String responseBody = "User not found";

    mockUsersResponse(responseCode, responseBody);

    usersClient.findPatronGroupIdForUser(userId)
      .onSuccess(context::fail)
      .onFailure(throwable -> {
        context.assertTrue(throwable instanceof EntityNotFoundException);
        context.assertEquals(format("Failed to fetch user with ID %s. Response: %d %s",
          userId, responseCode, responseBody), throwable.getMessage());
        async.complete();
      });
  }

  @Test
  public void invalidJsonResponse(TestContext context) {
    Async async = context.async();

    mockUsersResponse(200, "not really json");

    usersClient.findPatronGroupIdForUser(randomId())
      .onSuccess(context::fail)
      .onFailure(throwable -> {
        context.assertTrue(throwable instanceof DecodeException);
        async.complete();
      });
  }

  @Test
  public void invalidUserIdInRequest(TestContext context) {
    Async async = context.async();

    mockUsersResponse(200, new JsonObject()
      .put("id", USER_ID)
      .put("patronGroup", PATRON_GROUP_ID)
      .encodePrettily());

    String invalidUserId = randomId() + "oops";

    usersClient.findPatronGroupIdForUser(invalidUserId)
      .onSuccess(context::fail)
      .onFailure(throwable -> {
        context.assertTrue(throwable instanceof ValidationException);
        context.assertEquals(
          format("Invalid UUID: \"%s\"", invalidUserId), throwable.getMessage());
        async.complete();
      });
  }

  @Test
  public void invalidPatronGroupIdInResponse(TestContext context) {
    Async async = context.async();

    String invalidPatronGroupId = randomId() + "oops";

    mockUsersResponse(200, new JsonObject()
      .put("id", USER_ID)
      .put("patronGroup", invalidPatronGroupId)
      .encodePrettily());


    usersClient.findPatronGroupIdForUser(USER_ID)
      .onSuccess(context::fail)
      .onFailure(throwable -> {
        context.assertTrue(throwable instanceof ValidationException);
        context.assertEquals(
          format("Invalid UUID: \"%s\"", invalidPatronGroupId), throwable.getMessage());
        async.complete();
      });
  }

  private void mockUsersResponse(int responseStatus, String responseBody) {
    wireMock.stubFor(get(urlPathMatching("/users/.+"))
      .willReturn(aResponse()
        .withStatus(responseStatus)
        .withBody(responseBody)
      ));
  }
}
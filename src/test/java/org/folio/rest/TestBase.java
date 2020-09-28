package org.folio.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.String.format;

import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.OkapiClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class TestBase {
  protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected static final int OKAPI_PORT = NetworkUtils.nextFreePort();
  protected static final String OKAPI_URL = "http://localhost:" + OKAPI_PORT;
  protected static final String OKAPI_TENANT = "test_tenant";
  protected static final String OKAPI_TOKEN = generateOkapiToken();

  protected static Vertx vertx;
  protected static OkapiClient okapiClient;
  protected static TenantClient tenantClient;
  protected static PostgresClient postgresClient;

  @ClassRule
  public static WireMockRule wireMock = new WireMockRule(
    new WireMockConfiguration().dynamicPort());

  @BeforeClass
  public static void beforeAll(final TestContext context) throws Exception {
    Async async = context.async();

    vertx = Vertx.vertx();
    okapiClient = new OkapiClient(getMockedOkapiUrl(), OKAPI_TENANT, OKAPI_TOKEN);
    tenantClient = new TenantClient(getMockedOkapiUrl(), OKAPI_TENANT, OKAPI_TOKEN);

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();

    mockEndpoints();

    DeploymentOptions deploymentOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", OKAPI_PORT));

    vertx.deployVerticle(RestVerticle.class.getName(), deploymentOptions, deployment -> {
      try {
        tenantClient.postTenant(getTenantAttributes(), result -> {
          result.exceptionHandler(context::fail);
          if (result.statusCode() != 201) {
            context.fail(format("postTenant returned %d, but expected 201", result.statusCode()));
          }
          postgresClient = PostgresClient.getInstance(vertx, OKAPI_TENANT);
          async.complete();
        });
      } catch (Exception e) {
        context.fail(e);
      }
    });
  }

  @AfterClass
  public static void afterAll(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  @Before
  public void resetMocks() {
    mockEndpoints();
  }

  private static void mockEndpoints() {
    wireMock.resetAll();

    wireMock.stubFor(post(urlEqualTo("/pubsub/event-types"))
      .atPriority(100)
      .willReturn(created()));

    wireMock.stubFor(post(urlMatching("/pubsub/event-types/declare/(publisher|subscriber)"))
      .atPriority(100)
      .willReturn(created()));

    // forward everything to Okapi
    wireMock.stubFor(any(anyUrl())
      .atPriority(Integer.MAX_VALUE)
      .willReturn(aResponse().proxiedFrom(OKAPI_URL)));
  }

  protected static TenantAttributes getTenantAttributes() {
    final Parameter loadReferenceParameter = new Parameter()
      .withKey("loadReference").withValue("true");

    return new TenantAttributes()
      .withModuleFrom("mod-patron-blocks-0.0.1")
      .withModuleTo("mod-patron-blocks-" + PomReader.INSTANCE.getVersion())
      .withParameters(Collections.singletonList(loadReferenceParameter));
  }

  protected void deleteAllFromTable(String tableName) {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    postgresClient.delete(tableName, new Criterion(), result -> future.complete(null));
    try {
      future.get(5, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static String generateOkapiToken() {
    final String payload = new JsonObject()
      .put("user_id", randomId())
      .put("tenant", OKAPI_TENANT)
      .put("sub", "admin")
      .toString();

    return format("1.%s.3", Base64.getEncoder()
      .encodeToString(payload.getBytes()));
  }

  protected static String getMockedOkapiUrl() {
    return "http://localhost:" + wireMock.port();
  }

  protected static String randomId() {
    return UUID.randomUUID().toString();
  }

  protected static <T> T waitFor(Future<T> future) {
    return waitFor(future, 3);
  }

  protected static <T> T waitFor(Future<T> future, int waitForSeconds) {
    Awaitility.await()
      .atMost(waitForSeconds, TimeUnit.SECONDS)
      .until(future::isComplete);

    return future.result();
  }

  protected static String toJson(Object event) {
    return JsonObject.mapFrom(event).encodePrettily();
  }
}

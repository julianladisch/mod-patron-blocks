package org.folio.rest.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.drools.core.util.StringUtils.EMPTY;

import java.util.Optional;

import org.awaitility.Awaitility;
import org.folio.domain.UserSummary;
import org.folio.repository.UserSummaryRepository;
import org.folio.repository.UserSummaryRepositoryImpl;
import org.folio.rest.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class EventHandlersAPITest extends TestBase {
  private static final String USER_SUMMARY_TABLE_NAME = "user_summary";

  private static final String FEE_FINE_BALANCE_CHANGED_HANDLER_URL =
    "/automated-patron-blocks/handlers/fee-fine-balance-changed";
  private static final String ITEM_CHECKED_OUT_HANDLER_URL =
    "/automated-patron-blocks/handlers/item-checked-out";
  private static final String ITEM_CHECKED_IN_HANDLER_URL =
    "/automated-patron-blocks/handlers/item-checked-in";
  private static final String ITEM_DECLARED_LOST_HANDLER_URL =
    "/automated-patron-blocks/handlers/item-declared-lost";
  private static final String LOAN_DUE_DATE_UPDATED_HANDLER_URL =
    "/automated-patron-blocks/handlers/loan-due-date-updated";

  final UserSummaryRepository userSummaryRepository = new UserSummaryRepositoryImpl(postgresClient);

  @Before
  public void beforeEach() {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void postAutomatedPatronBlocksHandlersFeeFineBalanceChanged(TestContext context) {
    String userId = randomId();

    Optional<UserSummary> userSummaryBeforeEvent =
      waitFor(userSummaryRepository.getUserSummaryByUserId(userId));

    context.assertFalse(userSummaryBeforeEvent.isPresent());

    String payload = new JsonObject()
      .put("userId", userId)
      .put("feeFineId", "d44fcc83-1d20-4a12-a11a-fc895b0f0433")
      .put("feeFineTypeId", "0a5db422-7714-4e9f-8464-8e2e893b6149")
      .put("balance", 9.0)
      .encodePrettily();

    sendEvent(FEE_FINE_BALANCE_CHANGED_HANDLER_URL, payload, context);
    assertThatUserSummaryWasCreated(userId);
  }

  @Test
  public void postAutomatedPatronBlocksHandlersItemCheckedOut(TestContext context) {
    // TODO: replace with real test once event handler is implemented
    sendEvent(ITEM_CHECKED_OUT_HANDLER_URL, EMPTY, context);
  }

  @Test
  public void postAutomatedPatronBlocksHandlersItemCheckedIn(TestContext context) {
    // TODO: replace with real test once event handler is implemented
    sendEvent(ITEM_CHECKED_IN_HANDLER_URL, EMPTY, context);
  }

  @Test
  public void postAutomatedPatronBlocksHandlersItemDeclaredLost(TestContext context) {
    // TODO: replace with real test once event handler is implemented
    sendEvent(ITEM_DECLARED_LOST_HANDLER_URL, EMPTY, context);
  }

  @Test
  public void postAutomatedPatronBlocksHandlersLoanDueDateUpdated(TestContext context) {
    // TODO: replace with real test once event handler is implemented
    sendEvent(LOAN_DUE_DATE_UPDATED_HANDLER_URL, EMPTY, context);
  }

  private void sendEvent(String url, String payload, TestContext context) {
    Response response = okapiClient.post(url, payload);
    context.assertEquals(204, response.getStatusCode());
    context.assertTrue(response.getBody().print().isEmpty());
  }

  private void assertThatUserSummaryWasCreated(String userId) {
    Awaitility.await()
      .atMost(5, SECONDS)
      .until(() -> waitFor(userSummaryRepository.getUserSummaryByUserId(userId)).isPresent());
  }

}
package org.folio.rest.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;

import java.util.Optional;

import org.awaitility.Awaitility;
import org.folio.domain.OpenLoan;
import org.folio.domain.UserSummary;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class EventHandlersAPITest extends TestBase {
  private static final String FEE_FINE_BALANCE_CHANGED_HANDLER_URL =
    "/automated-patron-blocks/handlers/fee-fine-balance-changed";
  private static final String ITEM_CHECKED_OUT_HANDLER_URL =
    "/automated-patron-blocks/handlers/item-checked-out";

  final UserSummaryRepository userSummaryRepository = new UserSummaryRepository(postgresClient);

  @Before
  public void beforeEach() {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void postAutomatedPatronBlocksHandlersFeeFineBalanceChanged(TestContext context) {
    String userId = randomId();

    Optional<UserSummary> userSummaryBeforeEvent =
      waitFor(userSummaryRepository.getByUserId(userId));

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
    String userId = randomId();
    String loanId = randomId();
    DateTime dueDate = DateTime.now();

    Optional<UserSummary> userSummaryBeforeEvent =
      waitFor(userSummaryRepository.getByUserId(userId));

    context.assertFalse(userSummaryBeforeEvent.isPresent());

    String payload = new JsonObject()
      .put("userId", userId)
      .put("loanId", loanId)
      .put("dueDate", dueDate.toString(ISODateTimeFormat.dateTime()))
      .encodePrettily();

    sendEvent(ITEM_CHECKED_OUT_HANDLER_URL, payload, context);
    assertThatUserSummaryWasCreated(userId);

    UserSummary userSummary = waitFor(userSummaryRepository.getByUserId(userId), 1).orElse(null);
    context.assertNotNull(userSummary);
    context.assertEquals(userSummary.getUserId(), userId);
    context.assertEquals(userSummary.getOpenLoans().size(), 1);

    OpenLoan openLoan = userSummary.getOpenLoans().get(0);
    context.assertEquals(openLoan.getLoanId(), loanId);
    context.assertEquals(openLoan.getRecall(), false);
    context.assertEquals(openLoan.getDueDate(), dueDate.toDate());
    context.assertEquals(openLoan.getReturnedDate(), null);
  }

  private void sendEvent(String url, String payload, TestContext context) {
    Response response = okapiClient.post(url, payload);
    context.assertEquals(204, response.getStatusCode());
    context.assertTrue(response.getBody().print().isEmpty());
  }

  private void assertThatUserSummaryWasCreated(String userId) {
    Awaitility.await()
      .atMost(5, SECONDS)
      .until(() -> waitFor(userSummaryRepository.getByUserId(userId)).isPresent());
  }

}

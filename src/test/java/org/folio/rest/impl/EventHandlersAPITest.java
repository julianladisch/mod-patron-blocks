package org.folio.rest.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;

import java.math.BigDecimal;
import java.util.Optional;

import org.awaitility.Awaitility;
import org.folio.domain.UserSummary;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.ItemCheckedInEvent;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.response.ValidatableResponse;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class EventHandlersAPITest extends TestBase {
  private static final String FEE_FINE_BALANCE_CHANGED_HANDLER_URL =
    "/automated-patron-blocks/handlers/fee-fine-balance-changed";
  private static final String ITEM_CHECKED_OUT_HANDLER_URL =
    "/automated-patron-blocks/handlers/item-checked-out";
  private static final String ITEM_CHECKED_IN_HANDLER_URL =
    "/automated-patron-blocks/handlers/item-checked-in";
  private static final String ITEM_DECLARED_LOST_HANDLER_URL =
    "/automated-patron-blocks/handlers/item-declared-lost";
  private static final String LOAN_DUE_DATE_CHANGED_HANDLER_URL =
    "/automated-patron-blocks/handlers/loan-due-date-changed";

  final UserSummaryRepository userSummaryRepository = new UserSummaryRepository(postgresClient);

  @Before
  public void beforeEach() {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void feeFineBalanceChangedEventProcessedSuccessfully(TestContext context) {
    String userId = randomId();

    Optional<UserSummary> userSummaryBeforeEvent =
      waitFor(userSummaryRepository.getByUserId(userId));

    context.assertFalse(userSummaryBeforeEvent.isPresent());

    FeeFineBalanceChangedEvent event = new FeeFineBalanceChangedEvent()
      .withUserId(userId)
      .withFeeFineId(randomId())
      .withFeeFineTypeId(randomId())
      .withBalance(BigDecimal.TEN);

    sendEvent(FEE_FINE_BALANCE_CHANGED_HANDLER_URL, toJson(event), SC_NO_CONTENT);

    assertThatUserSummaryWasCreated(userId);
  }

  @Test
  public void feeFineBalanceChangedEventWithMalformedJsonRequest(TestContext context) {
    sendEvent(FEE_FINE_BALANCE_CHANGED_HANDLER_URL, "not json", SC_BAD_REQUEST);
  }

  @Test
  public void feeFineBalanceChangedEventWithInvalidValue(TestContext context) {
    FeeFineBalanceChangedEvent event = new FeeFineBalanceChangedEvent()
      .withUserId(randomId() + "oops")
      .withFeeFineId(randomId())
      .withFeeFineTypeId(randomId())
      .withBalance(BigDecimal.TEN);

    sendEvent(FEE_FINE_BALANCE_CHANGED_HANDLER_URL, toJson(event), SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void postAutomatedPatronBlocksHandlersItemCheckedOut(TestContext context) {
    String userId = randomId();
    String loanId = randomId();
    DateTime dueDate = DateTime.now();

    Optional<UserSummary> userSummaryBeforeEvent =
      waitFor(userSummaryRepository.getByUserId(userId));

    context.assertFalse(userSummaryBeforeEvent.isPresent());

    ItemCheckedOutEvent event = new ItemCheckedOutEvent()
      .withUserId(userId)
      .withLoanId(loanId)
      .withDueDate(dueDate.toDate());

    sendEvent(ITEM_CHECKED_OUT_HANDLER_URL, toJson(event), SC_NO_CONTENT);
    assertThatUserSummaryWasCreated(userId);
  }

  @Test
  public void postAutomatedPatronBlocksHandlersItemCheckedIn(TestContext context) {
    // TODO: replace with real test once event handler is implemented
    sendEvent(ITEM_CHECKED_IN_HANDLER_URL, toJson(new ItemCheckedInEvent()), SC_NO_CONTENT);
  }

  @Test
  public void postAutomatedPatronBlocksHandlersItemDeclaredLost(TestContext context) {
    String userId = randomId();
    String loanId = randomId();

    Optional<UserSummary> userSummaryBeforeEvent =
      waitFor(userSummaryRepository.getByUserId(userId));

    context.assertFalse(userSummaryBeforeEvent.isPresent());

    ItemDeclaredLostEvent event = new ItemDeclaredLostEvent()
      .withUserId(userId)
      .withLoanId(loanId);

    sendEvent(ITEM_DECLARED_LOST_HANDLER_URL, toJson(event), SC_NO_CONTENT);
    assertThatUserSummaryWasCreated(userId);
  }

  @Test
  public void postAutomatedPatronBlocksHandlersLoanDueDateUpdated(TestContext context) {
    // TODO: replace with real test once event handler is implemented
    sendEvent(LOAN_DUE_DATE_CHANGED_HANDLER_URL, toJson(new LoanDueDateChangedEvent()), SC_NO_CONTENT);
  }

  private ValidatableResponse sendEvent(String url, String payload, int expectedStatus) {
    return okapiClient.post(url, payload)
      .then()
      .statusCode(expectedStatus);
  }

  private void assertThatUserSummaryWasCreated(String userId) {
    Awaitility.await()
      .atMost(5, SECONDS)
      .until(() -> waitFor(userSummaryRepository.getByUserId(userId)).isPresent());
  }

}

package org.folio.rest.impl;

import static java.util.Map.entry;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;
import static org.folio.rest.utils.EntityBuilder.buildFeeFine;
import static org.folio.rest.utils.EntityBuilder.buildFeeFineBalanceChangedEvent;
import static org.folio.rest.utils.EntityBuilder.buildItemAgedToLostEvent;
import static org.folio.rest.utils.EntityBuilder.buildItemCheckedInEvent;
import static org.folio.rest.utils.EntityBuilder.buildItemCheckedOutEvent;
import static org.folio.rest.utils.EntityBuilder.buildItemClaimedReturnedEvent;
import static org.folio.rest.utils.EntityBuilder.buildItemDeclaredLostEvent;
import static org.folio.rest.utils.EntityBuilder.buildLoanDueDateChangedEvent;
import static org.folio.rest.utils.EntityBuilder.buildUserSummary;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.folio.domain.Event;
import org.folio.domain.FeeFineType;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.ItemAgedToLostEvent;
import org.folio.rest.jaxrs.model.ItemCheckedInEvent;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.jaxrs.model.OpenFeeFine;
import org.folio.rest.jaxrs.model.UserSummary;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.response.ValidatableResponse;
import io.vertx.core.Future;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class EventHandlersAPITest extends TestBase {
  public static final String USER_ID = randomId();
  public static final String INVALID_USER_ID = USER_ID + "xyz";
  private static final String LOAN_ID = randomId();


  private static final String EVENT_HANDLERS_ROOT_URL = "/automated-patron-blocks/handlers/";

  private static final String FEE_FINE_BALANCE_CHANGED_HANDLER_URL =
    EVENT_HANDLERS_ROOT_URL + "fee-fine-balance-changed";
  private static final String ITEM_CHECKED_OUT_HANDLER_URL =
    EVENT_HANDLERS_ROOT_URL + "item-checked-out";
  private static final String ITEM_CHECKED_IN_HANDLER_URL =
    EVENT_HANDLERS_ROOT_URL + "item-checked-in";
  private static final String ITEM_DECLARED_LOST_HANDLER_URL =
    EVENT_HANDLERS_ROOT_URL + "item-declared-lost";
  private static final String ITEM_AGED_TO_LOST_HANDLER_URL =
    EVENT_HANDLERS_ROOT_URL + "item-aged-to-lost";
  private static final String ITEM_CLAIMED_RETURNED_HANDLER_URL =
    EVENT_HANDLERS_ROOT_URL + "item-claimed-returned";
  private static final String LOAN_DUE_DATE_CHANGED_HANDLER_URL =
    EVENT_HANDLERS_ROOT_URL + "loan-due-date-changed";

  private static final Map<Class<? extends Event>, String> eventTypeToHandlerUrl =
    Map.ofEntries(
      entry(FeeFineBalanceChangedEvent.class, FEE_FINE_BALANCE_CHANGED_HANDLER_URL),
      entry(ItemCheckedOutEvent.class, ITEM_CHECKED_OUT_HANDLER_URL),
      entry(ItemCheckedInEvent.class, ITEM_CHECKED_IN_HANDLER_URL),
      entry(ItemDeclaredLostEvent.class, ITEM_DECLARED_LOST_HANDLER_URL),
      entry(ItemAgedToLostEvent.class, ITEM_AGED_TO_LOST_HANDLER_URL),
      entry(ItemClaimedReturnedEvent.class, ITEM_CLAIMED_RETURNED_HANDLER_URL),
      entry(LoanDueDateChangedEvent.class, LOAN_DUE_DATE_CHANGED_HANDLER_URL)
    );

  private final UserSummaryRepository userSummaryRepository =
    new UserSummaryRepository(postgresClient);

  @After
  public void afterEach() {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }
  //TODO test all events created and all failed

  @Test
  public void feeFineBalanceChangedEventProcessedSuccessfully(TestContext context) {
    String feeFineId = randomId();
    userSummaryRepository.save(buildUserSummary(USER_ID, Collections.singletonList(
        buildFeeFine(LOAN_ID, feeFineId, FeeFineType.LOST_ITEM_FEE.getId(), BigDecimal.ONE)),
      Collections.EMPTY_LIST));
    sendItemCheckOutEventAndEvent(buildFeeFineBalanceChangedEvent(USER_ID, LOAN_ID, feeFineId,
      FeeFineType.LOST_ITEM_FEE.getId(), BigDecimal.TEN));

    getUserSummary()
      .ifPresentOrElse(userSummary -> {
        userSummary.getOpenFeesFines().stream()
          .findFirst()
          .ifPresentOrElse(feeFine -> {
            assertEquals(BigDecimal.TEN, feeFine.getBalance());
            }, context::fail);
      }, context::fail);
  }

  @Test
  public void feeFineBalanceChangedEventValidationFails() {
    sendEventAndVerifyValidationFailure(
      createFeeFineBalanceChangedEvent().withUserId(INVALID_USER_ID));
  }

  @Test
  public void itemCheckedInEventProcessedSuccessfully() {
    assertFalse(getUserSummary().isPresent());
    sendEvent(createItemCheckedInEvent(), SC_NO_CONTENT);
  }

  @Test
  public void itemCheckedInEventValidationFails() {
    sendEventAndVerifyValidationFailure(
      createItemCheckedInEvent().withUserId(INVALID_USER_ID));
  }

  @Test
  public void itemCheckedOutEventProcessedSuccessfully() {
    sendItemCheckOutEventAndEvent(createItemCheckedOutEvent());
  }

  @Test
  public void itemCheckedOutEventValidationFails() {
    sendEventAndVerifyValidationFailure(
      createItemCheckedOutEvent().withUserId(INVALID_USER_ID));
  }

  @Test
  public void loanDueDateChangedEventProcessedSuccessfully() {
    sendItemCheckOutEventAndEvent(createLoanDueDateChangedEvent());
  }

  @Test
  public void loanDueDateChangedEventValidationFails() {
    sendEventAndVerifyValidationFailure(
      createLoanDueDateChangedEvent().withUserId(INVALID_USER_ID));
  }

  @Test
  public void itemDeclaredLostEventProcessedSuccessfully() {
    sendItemCheckOutEventAndEvent(createItemDeclaredLostEvent());
  }

  @Test
  public void itemDeclaredLostEventValidationFails() {
    sendEventAndVerifyValidationFailure(
      createItemDeclaredLostEvent().withUserId(INVALID_USER_ID));
  }

  @Test
  public void itemAgedToLostEventProcessedSuccessfully() {
    sendEvent(createItemCheckedOutEvent(),204);
    sendItemCheckOutEventAndEvent(createItemAgedToLostEvent());
  }

  @Test
  public void itemAgedToLostEventValidationFails() {
    sendEventAndVerifyValidationFailure(
      createItemAgedToLostEvent().withUserId(INVALID_USER_ID));
  }

  @Test
  public void itemClaimedReturnedEventProcessedSuccessfully() {
    sendItemCheckOutEventAndEvent(createItemClaimedReturnedEvent());
  }

  @Test
  public void itemClaimedReturnedEventValidationFails() {
    sendEventAndVerifyValidationFailure(
      createItemClaimedReturnedEvent().withUserId(INVALID_USER_ID));
  }

  @Test
  public void eventHandlingFailsWhenEventJsonIsInvalid() {
    sendEvent("not json", FEE_FINE_BALANCE_CHANGED_HANDLER_URL, SC_BAD_REQUEST);
  }

  @Test
  public void loanDueDateChangedEventWithMissingRequiredDueDateProperty() {
    sendEventAndVerifyValidationFailure(
      createLoanDueDateChangedEvent().withDueDate(null));
  }

  private static FeeFineBalanceChangedEvent createFeeFineBalanceChangedEvent() {
    return buildFeeFineBalanceChangedEvent(
      USER_ID, randomId(), randomId(), randomId(), BigDecimal.TEN);
  }

  private static ItemCheckedInEvent createItemCheckedInEvent() {
    return buildItemCheckedInEvent(USER_ID, randomId(), new Date());
  }

  private static ItemCheckedOutEvent createItemCheckedOutEvent() {
    return buildItemCheckedOutEvent(USER_ID, randomId(), new Date());
  }

  private static LoanDueDateChangedEvent createLoanDueDateChangedEvent() {
    return buildLoanDueDateChangedEvent(USER_ID, randomId(), new Date(), false);
  }

  private static ItemDeclaredLostEvent createItemDeclaredLostEvent() {
    return buildItemDeclaredLostEvent(USER_ID, randomId());
  }

  private static ItemAgedToLostEvent createItemAgedToLostEvent() {
    return buildItemAgedToLostEvent(USER_ID, randomId());
  }

  private static ItemClaimedReturnedEvent createItemClaimedReturnedEvent() {
    return buildItemClaimedReturnedEvent(USER_ID, randomId());
  }

  private void sendItemCheckOutEventAndEvent(Event event) {
    //assertFalse(getUserSummary().isPresent());

    // sending item check out event to create user summary
    sendEvent(buildItemCheckedOutEvent(USER_ID, LOAN_ID, Date.from(LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault()).toInstant())), SC_NO_CONTENT);

    sendEvent(event, SC_NO_CONTENT);
  }

  private ValidatableResponse sendEventAndVerifyValidationFailure(Event event) {
    return sendEvent(event, SC_UNPROCESSABLE_ENTITY);
  }

  private ValidatableResponse sendEvent(Event event, int expectedStatus) {
    return waitFor(Future.succeededFuture(sendEvent(toJson(event), getHandlerUrlForEvent(event),
      expectedStatus)));
  }

  private ValidatableResponse sendEvent(String eventPayload, String handlerUrl, int expectedStatus) {
    return okapiClient.post(handlerUrl, eventPayload)
      .then()
      .statusCode(expectedStatus);
  }

  private static String getHandlerUrlForEvent(Event event) {
    final String eventHandlerUrl = eventTypeToHandlerUrl.get(event.getClass());

    if (eventHandlerUrl == null) {
      fail("Failed to resolve handler URL for event of type " + event.getClass().getSimpleName());
    }

    return eventHandlerUrl;
  }

  private Optional<UserSummary> getUserSummary() {
    return waitFor(userSummaryRepository.getByUserId(USER_ID));
  }
}

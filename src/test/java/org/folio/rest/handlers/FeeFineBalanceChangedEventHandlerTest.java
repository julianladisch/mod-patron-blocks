package org.folio.rest.handlers;

import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;

import java.math.BigDecimal;
import java.util.Arrays;

import org.folio.domain.OpenFeeFine;
import org.folio.domain.UserSummary;
import org.folio.exception.EntityNotFoundException;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class FeeFineBalanceChangedEventHandlerTest extends EventHandlerTest {
  private static final FeeFineBalanceChangedEventHandler eventHandler =
    new FeeFineBalanceChangedEventHandler(postgresClient);

  @Before
  public void beforeEach(TestContext context) {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void createNewUserSummary(TestContext context) {
    Async async = context.async();

    final String userId = randomId();
    final String feeFineId = randomId();
    final String feeFineTypeId = randomId();
    final BigDecimal balance = new BigDecimal("1.55");

    FeeFineBalanceChangedEvent event = createEvent(userId, feeFineId, feeFineTypeId, balance);

    eventHandler.handle(event)
      .onFailure(context::fail)
      .onSuccess(summaryId -> {
        checkResult(summaryId, userId, balance, 1, feeFineId, feeFineTypeId, balance, context);
        async.complete();
      });
  }

  @Test
  public void addNewFeeFineToExistingUserSummary(TestContext context) {
    Async async = context.async();

    final String userId = randomId();
    final BigDecimal initialOutstandingFeeFineBalance = new BigDecimal("2.55");

    UserSummary initialUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(initialOutstandingFeeFineBalance);

    OpenFeeFine existingFeeFine = new OpenFeeFine()
      .withBalance(initialOutstandingFeeFineBalance)
      .withFeeFineTypeId(randomId())
      .withFeeFineId(randomId());

    initialUserSummary.getOpenFeeFines().add(existingFeeFine);

    userSummaryRepository.save(initialUserSummary)
      .onFailure(context::fail)
      .onSuccess(summaryId -> {
        final String feeFineId = randomId();
        final String feeFineTypeId = randomId();
        final BigDecimal eventBalance = new BigDecimal("7.45");
        FeeFineBalanceChangedEvent event = createEvent(userId, feeFineId, feeFineTypeId, eventBalance);

        eventHandler.handle(event)
          .onFailure(context::fail)
          .onSuccess(id -> {
            checkResult(id, userId, initialOutstandingFeeFineBalance.add(eventBalance),
              2, feeFineId, feeFineTypeId, eventBalance, context);
            async.complete();
          });
      });
  }

  @Test
  public void updateFeeFineBalanceInExistingUserSummary(TestContext context) {
    Async async = context.async();

    final String userId = randomId();
    final String feeFineId = randomId();
    final String feeFineTypeId = randomId();
    final BigDecimal initialFeeFineBalance = new BigDecimal("1.25");

    UserSummary existingUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(initialFeeFineBalance);

    OpenFeeFine existingFeeFine = new OpenFeeFine()
      .withBalance(initialFeeFineBalance)
      .withFeeFineTypeId(feeFineTypeId)
      .withFeeFineId(feeFineId);

    existingUserSummary.getOpenFeeFines().add(existingFeeFine);

    userSummaryRepository.save(existingUserSummary)
      .onFailure(context::fail)
      .onSuccess(summaryId -> {
        final BigDecimal eventBalance = new BigDecimal("2.75");
        FeeFineBalanceChangedEvent event = createEvent(userId, feeFineId, feeFineTypeId, eventBalance);

        eventHandler.handle(event)
          .onFailure(context::fail)
          .onSuccess(id -> {
            checkResult(id, userId, eventBalance, 1, feeFineId, feeFineTypeId, eventBalance, context);
            async.complete();
          });
      });
  }

  @Test
  public void deleteClosedFeeFineFromExistingUserSummary(TestContext context) {
    Async async = context.async();

    final String userId = randomId();
    final String feeFineId1 = randomId();
    final String feeFineTypeId1 = randomId();
    final BigDecimal feeFineBalance1 = new BigDecimal("1.25");

    OpenFeeFine existingFeeFine1 = new OpenFeeFine()
      .withFeeFineId(feeFineId1)
      .withFeeFineTypeId(feeFineTypeId1)
      .withBalance(feeFineBalance1);

    final String feeFineId2 = randomId();
    final String feeFineTypeId2 = randomId();
    final BigDecimal feeFineBalance2 = new BigDecimal("2.55");

    OpenFeeFine existingFeeFine2 = new OpenFeeFine()
      .withBalance(feeFineBalance2)
      .withFeeFineTypeId(feeFineTypeId2)
      .withFeeFineId(feeFineId2);

    UserSummary existingUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(feeFineBalance1.add(feeFineBalance2))
      .withOpenFeesFines(Arrays.asList(existingFeeFine1, existingFeeFine2));

    userSummaryRepository.save(existingUserSummary)
      .onFailure(context::fail)
      .onSuccess(summaryId -> {
        FeeFineBalanceChangedEvent event = createEvent(null, feeFineId2, null, BigDecimal.ZERO);

        eventHandler.handle(event)
          .onFailure(context::fail)
          .onSuccess(id -> {
            checkResult(id, userId, feeFineBalance1, 1, feeFineId1,
              feeFineTypeId1, feeFineBalance1, context);
            async.complete();
          });
      });
  }

  @Test
  public void closedFeeFineEventForNonExistingSummaryShouldBeIgnored(TestContext context) {
    Async async = context.async();

    FeeFineBalanceChangedEvent event = createEvent(null, randomId(), null, BigDecimal.ZERO);

    eventHandler.handle(event)
      .onSuccess(context::fail)
      .onFailure(throwable -> {
        context.assertTrue(throwable instanceof EntityNotFoundException);
        context.assertTrue(throwable.getMessage().contains("event is ignored"));
        async.complete();
      });
  }

  private static FeeFineBalanceChangedEvent createEvent(String userId, String feeFineId,
    String feeFineTypeId, BigDecimal balance) {

    return new FeeFineBalanceChangedEvent()
      .withUserId(userId)
      .withFeeFineId(feeFineId)
      .withFeeFineTypeId(feeFineTypeId)
      .withBalance(balance);
  }

  private void checkResult(String summaryId, String userId,
    BigDecimal expectedOutstandingFeeFineBalance, int expectedNumberOfOpenFeesFines,
    String expectedFeeFineId, String expectedFeeFineTypeId,
    BigDecimal expectedFeeFineBalance, TestContext context) {

    userSummaryRepository.get(summaryId)
      .onFailure(context::fail)
      .onSuccess(optionalSummary -> {
        UserSummary userSummary = optionalSummary.orElseThrow(() ->
          new AssertionError("User summary was not found: " + summaryId));

        context.assertEquals(userId, userSummary.getUserId());
        context.assertEquals(0, expectedOutstandingFeeFineBalance.compareTo(
          userSummary.getOutstandingFeeFineBalance()));
        context.assertTrue(userSummary.getOpenLoans().isEmpty());
        context.assertEquals(0, userSummary.getNumberOfLostItems());
        context.assertEquals(expectedNumberOfOpenFeesFines, userSummary.getOpenFeeFines().size());

        OpenFeeFine openFeeFine = userSummary.getOpenFeeFines().stream()
          .filter(feeFine -> feeFine.getFeeFineId().equals(expectedFeeFineId))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Fee/fine was not found: " + expectedFeeFineId));

        context.assertEquals(expectedFeeFineId, openFeeFine.getFeeFineId());
        context.assertEquals(expectedFeeFineTypeId, openFeeFine.getFeeFineTypeId());
        context.assertEquals(expectedFeeFineBalance, openFeeFine.getBalance());
      });
  }

}

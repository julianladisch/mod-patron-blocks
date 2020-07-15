package org.folio.rest.handlers;

import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.folio.domain.FeeFineType;
import org.folio.exception.EntityNotFoundException;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.OpenFeeFine;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class FeeFineBalanceChangedEventHandlerTest extends EventHandlerTestBase {
  private static final FeeFineBalanceChangedEventHandler eventHandler =
    new FeeFineBalanceChangedEventHandler(postgresClient);

  @Before
  public void beforeEach() {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void createNewUserSummary(TestContext context) {
    Async async = context.async();

    final String userId = randomId();
    final String loanId = randomId();
    final String feeFineId = randomId();
    final String feeFineTypeId = randomId();
    final BigDecimal balance = new BigDecimal("1.55");

    List<OpenFeeFine> expectedFeeFines = singletonList(
      new OpenFeeFine()
        .withFeeFineId(feeFineId)
        .withFeeFineTypeId(feeFineTypeId)
        .withLoanId(loanId)
        .withBalance(balance));

    FeeFineBalanceChangedEvent event = createEvent(userId, loanId, feeFineId, feeFineTypeId, balance);

    eventHandler.handle(event)
      .onFailure(context::fail)
      .onSuccess(summaryId -> {
        checkResult(summaryId, userId, expectedFeeFines, context);
        async.complete();
      });
  }

  @Test
  public void addNewFeeFineToExistingUserSummary(TestContext context) {
    Async async = context.async();

    final String userId = randomId();
    final String loanId = randomId();

    UserSummary initialUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId);

    OpenFeeFine existingFeeFine = new OpenFeeFine()
      .withBalance(new BigDecimal("2.55"))
      .withLoanId(loanId)
      .withFeeFineTypeId(randomId())
      .withFeeFineId(randomId());

    initialUserSummary.getOpenFeesFines().add(existingFeeFine);

    userSummaryRepository.save(initialUserSummary)
      .onFailure(context::fail)
      .onSuccess(summaryId -> {
        final String eventFeeFineId = randomId();
        final String eventFeeFineTypeId = randomId();
        final BigDecimal eventBalance = new BigDecimal("1.55");

        FeeFineBalanceChangedEvent event =
          createEvent(userId, loanId, eventFeeFineId, eventFeeFineTypeId, eventBalance);

        eventHandler.handle(event)
          .onFailure(context::fail)
          .onSuccess(id -> {
            List<OpenFeeFine> expectedFeeFines = Arrays.asList(existingFeeFine,
              new OpenFeeFine()
                .withFeeFineId(eventFeeFineId)
                .withFeeFineTypeId(eventFeeFineTypeId)
                .withLoanId(loanId)
                .withBalance(eventBalance));

            checkResult(id, userId, expectedFeeFines, context);
            async.complete();
          });
      });
  }

  @Test
  public void updateFeeFineBalanceInExistingUserSummary(TestContext context) {
    Async async = context.async();

    final String userId = randomId();
    final String loanId = randomId();
    final String feeFineId = randomId();
    final String feeFineTypeId = randomId();
    final BigDecimal initialFeeFineBalance = new BigDecimal("1.25");

    UserSummary existingUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId);

    OpenFeeFine existingFeeFine = new OpenFeeFine()
      .withLoanId(loanId)
      .withBalance(initialFeeFineBalance)
      .withFeeFineTypeId(feeFineTypeId)
      .withFeeFineId(feeFineId);

    existingUserSummary.getOpenFeesFines().add(existingFeeFine);

    userSummaryRepository.save(existingUserSummary)
      .onFailure(context::fail)
      .onSuccess(summaryId -> {
        final BigDecimal eventBalance = new BigDecimal("2.75");
        FeeFineBalanceChangedEvent event =
          createEvent(userId, loanId, feeFineId, feeFineTypeId, eventBalance);

        eventHandler.handle(event)
          .onFailure(context::fail)
          .onSuccess(id -> {
            List<OpenFeeFine> expectedFeeFines = singletonList(
              new OpenFeeFine()
                .withFeeFineId(feeFineId)
                .withFeeFineTypeId(feeFineTypeId)
                .withLoanId(loanId)
                .withBalance(eventBalance));

            checkResult(id, userId, expectedFeeFines, context);
            async.complete();
          });
      });
  }

  @Test
  public void deleteClosedFeeFineFromExistingUserSummary(TestContext context) {
    Async async = context.async();

    final String userId = randomId();
    final String loanId = randomId();

    final String feeFineId1 = randomId();
    final String feeFineTypeId1 = randomId();
    final BigDecimal feeFineBalance1 = new BigDecimal("1.25");

    OpenFeeFine existingFeeFine1 = new OpenFeeFine()
      .withLoanId(randomId())
      .withFeeFineId(feeFineId1)
      .withFeeFineTypeId(feeFineTypeId1)
      .withBalance(feeFineBalance1);

    final String feeFineId2 = randomId();
    final String feeFineTypeId2 = randomId();
    final BigDecimal feeFineBalance2 = new BigDecimal("2.55");

    OpenFeeFine existingFeeFine2 = new OpenFeeFine()
      .withLoanId(loanId)
      .withBalance(feeFineBalance2)
      .withFeeFineTypeId(feeFineTypeId2)
      .withFeeFineId(feeFineId2);

    UserSummary existingUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withOpenFeesFines(Arrays.asList(existingFeeFine1, existingFeeFine2));

    userSummaryRepository.save(existingUserSummary)
      .onFailure(context::fail)
      .onSuccess(summaryId -> {
        FeeFineBalanceChangedEvent event =
          createEvent(userId, loanId, feeFineId2, feeFineTypeId1, ZERO);

        eventHandler.handle(event)
          .onFailure(context::fail)
          .onSuccess(id -> {
            checkResult(id, userId, emptyList(), context);
            async.complete();
          });
      });
  }

  @Test
  public void removeDeletedFeeFineFromExistingUserSummary(TestContext context) {
    Async async = context.async();

    final String userId = randomId();
    final String loanId = randomId();

    final String feeFineId1 = randomId();
    final String feeFineTypeId1 = randomId();
    final BigDecimal feeFineBalance1 = new BigDecimal("1.25");

    OpenFeeFine existingFeeFine1 = new OpenFeeFine()
      .withLoanId(randomId())
      .withFeeFineId(feeFineId1)
      .withFeeFineTypeId(feeFineTypeId1)
      .withBalance(feeFineBalance1);

    final String feeFineId2 = randomId();
    final String feeFineTypeId2 = randomId();
    final BigDecimal feeFineBalance2 = new BigDecimal("2.55");

    OpenFeeFine existingFeeFine2 = new OpenFeeFine()
      .withLoanId(loanId)
      .withBalance(feeFineBalance2)
      .withFeeFineTypeId(feeFineTypeId2)
      .withFeeFineId(feeFineId2);

    UserSummary existingUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withOpenFeesFines(Arrays.asList(existingFeeFine1, existingFeeFine2));

    userSummaryRepository.save(existingUserSummary)
      .onFailure(context::fail)
      .onSuccess(summaryId -> {
        FeeFineBalanceChangedEvent event =
          createEvent(null, null, feeFineId2, null, ZERO);

        eventHandler.handle(event)
          .onFailure(context::fail)
          .onSuccess(id -> {
            checkResult(id, userId, emptyList(), context);
            async.complete();
          });
      });
  }

  @Test
  public void eventForDeletedFeeFineAndNonExistingSummaryShouldBeIgnored(TestContext context) {
    Async async = context.async();

    FeeFineBalanceChangedEvent event =
      createEvent(null, null, randomId(), null, ZERO);

    eventHandler.handle(event)
      .onSuccess(context::fail)
      .onFailure(throwable -> {
        context.assertTrue(throwable instanceof EntityNotFoundException);
        context.assertTrue(throwable.getMessage().contains("event is ignored"));
        async.complete();
      });
  }

  @Test
  public void closedFeeFineEventForNonExistingSummaryCreatesAnEmptySummary(TestContext context) {
    Async async = context.async();

    String userId = randomId();

    FeeFineBalanceChangedEvent event =
      createEvent(userId, randomId(), randomId(), randomId(), ZERO);

    eventHandler.handle(event)
      .onFailure(context::fail)
      .onSuccess(summaryId -> {
        checkResult(summaryId, userId, emptyList(), context);
        async.complete();
      });
  }

  @Test
  public void deleteLoanWhenLastRelatedLostItemFeeIsClosed(TestContext context) {
    final String userId = randomId();
    final String loanId = randomId();

    final String feeFineId1 = randomId();
    final String feeFineId2 = randomId();
    final String feeFineTypeId1 = FeeFineType.LOST_ITEM_FEE.getId();
    final String feeFineTypeId2 = FeeFineType.LOST_ITEM_PROCESSING_FEE.getId();
    final BigDecimal feeFineBalance1 = new BigDecimal("1.25");
    final BigDecimal feeFineBalance2 = new BigDecimal("2.55");

    OpenFeeFine existingFeeFine1 = new OpenFeeFine()
      .withLoanId(loanId)
      .withFeeFineId(feeFineId1)
      .withFeeFineTypeId(feeFineTypeId1)
      .withBalance(feeFineBalance1);

    OpenFeeFine existingFeeFine2 = new OpenFeeFine()
      .withLoanId(loanId)
      .withFeeFineId(feeFineId2)
      .withFeeFineTypeId(feeFineTypeId2)
      .withBalance(feeFineBalance2);

    OpenLoan openLoan = new OpenLoan()
      .withLoanId(loanId)
      .withDueDate(new Date())
      .withRecall(false)
      .withItemLost(true);

    UserSummary existingUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withOpenFeesFines(Arrays.asList(existingFeeFine1, existingFeeFine2))
      .withOpenLoans(singletonList(openLoan));

    String savedSummaryId = waitFor(userSummaryRepository.save(existingUserSummary));

    // CLOSE FIRST FEE

    FeeFineBalanceChangedEvent closeFirstFee =
      createEvent(userId, loanId, feeFineId1, feeFineTypeId1, ZERO);
    String updatedSummaryId1 = waitFor(eventHandler.handle(closeFirstFee));
    context.assertEquals(savedSummaryId, updatedSummaryId1);

    Optional<UserSummary> optionalSummary1 = waitFor(userSummaryRepository.get(savedSummaryId));
    context.assertTrue(optionalSummary1.isPresent());

    UserSummary updatedSummary1 = optionalSummary1.get();
    context.assertEquals(1, updatedSummary1.getOpenFeesFines().size());
    context.assertEquals(feeFineId2, updatedSummary1.getOpenFeesFines().get(0).getFeeFineId());
    context.assertEquals(1, updatedSummary1.getOpenLoans().size());

    // CLOSE SECOND FEE

    FeeFineBalanceChangedEvent closeSecondFee =
      createEvent(userId, loanId, feeFineId2, feeFineTypeId2, ZERO);
    String updatedSummaryId2 = waitFor(eventHandler.handle(closeSecondFee));
    context.assertEquals(savedSummaryId, updatedSummaryId2);

    Optional<UserSummary> optionalSummary2 = waitFor(userSummaryRepository.get(savedSummaryId));
    context.assertTrue(optionalSummary2.isPresent());

    UserSummary updatedSummary2 = optionalSummary2.get();
    context.assertTrue(updatedSummary2.getOpenLoans().isEmpty());
    context.assertTrue(updatedSummary2.getOpenFeesFines().isEmpty());
  }

  private static FeeFineBalanceChangedEvent createEvent(String userId, String loanId,
    String feeFineId, String feeFineTypeId, BigDecimal balance) {

    return new FeeFineBalanceChangedEvent()
      .withUserId(userId)
      .withLoanId(loanId)
      .withFeeFineId(feeFineId)
      .withFeeFineTypeId(feeFineTypeId)
      .withBalance(balance);
  }

  private void checkResult(String summaryId, String userId, List<OpenFeeFine> expectedFeeFines,
    TestContext context) {

    userSummaryRepository.get(summaryId)
      .onFailure(context::fail)
      .onSuccess(optionalSummary -> {
        UserSummary userSummary = optionalSummary.orElseThrow(() ->
          new AssertionError("User summary was not found: " + summaryId));

        context.assertEquals(userId, userSummary.getUserId());
        context.assertEquals(expectedFeeFines.size(), userSummary.getOpenFeesFines().size());

        for (OpenFeeFine expectedFeeFine : expectedFeeFines) {
          OpenFeeFine existingFeeFine = userSummary.getOpenFeesFines().stream()
            .filter(feeFine -> feeFine.getFeeFineId().equals(expectedFeeFine.getFeeFineId()))
            .findFirst()
            .orElseThrow(() ->
              new AssertionError("Fee/fine was not found: " + expectedFeeFine.getFeeFineId()));

          context.assertEquals(expectedFeeFine.getLoanId(), existingFeeFine.getLoanId());
          context.assertEquals(expectedFeeFine.getFeeFineId(), existingFeeFine.getFeeFineId());
          context.assertEquals(expectedFeeFine.getFeeFineTypeId(), existingFeeFine.getFeeFineTypeId());
          context.assertEquals(0, expectedFeeFine.getBalance().compareTo(existingFeeFine.getBalance()));
        }


      });
  }

}

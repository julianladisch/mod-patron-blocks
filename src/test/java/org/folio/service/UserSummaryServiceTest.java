package org.folio.service;

import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;
import static org.folio.rest.utils.EntityBuilder.buildFeeFineBalanceChangedEvent;

import java.math.BigDecimal;

import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.UserSummary;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class UserSummaryServiceTest extends TestBase {
  private final UserSummaryService userSummaryService =
    new UserSummaryService(postgresClient);

  protected final UserSummaryRepository userSummaryRepository =
    new UserSummaryRepository(postgresClient);

  @Before
  public void beforeEach() {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void shouldAddEvent(TestContext context) {
    final String userId = randomId();

    String summaryId = randomId();
    UserSummary userSummaryToSave = createUserSummary(summaryId, randomId());
    waitFor(userSummaryRepository.save(userSummaryToSave));

    waitFor(userSummaryRepository.get(summaryId)).ifPresent(userSummary -> {
      final String feeFineId = randomId();
      final String loanId2 = randomId();
      final String feeFineTypeId = randomId();
      BigDecimal balance = new BigDecimal("3.33");
      FeeFineBalanceChangedEvent feeFineBalanceChangedEvent = buildFeeFineBalanceChangedEvent(
        userId, loanId2, feeFineId, feeFineTypeId, balance);
      userSummaryService.updateUserSummaryWithEvent(userSummary, feeFineBalanceChangedEvent);
      waitFor(userSummaryRepository.get(summaryId)).ifPresent(
        updatedUserSummary -> context.assertTrue(
          updatedUserSummary.getOpenFeesFines().stream()
            .anyMatch(openFeeFine -> openFeeFine.getFeeFineId().equals(feeFineId))));
    });
  }

  @Test
  public void shouldDeleteFeeFineAfterRetryingInCaseOfOptimisticLockingError(
    TestContext context) {
    final String userId = randomId();

    String summaryId = randomId();
    UserSummary userSummaryToSave = createUserSummary(summaryId, randomId());
    waitFor(userSummaryRepository.save(userSummaryToSave));

    waitFor(userSummaryRepository.get(summaryId)).ifPresent(userSummary -> {
      final String feeFineId = randomId();
      final String loanId = randomId();
      final String feeFineTypeId = randomId();
      BigDecimal balance = new BigDecimal("3.33");
      FeeFineBalanceChangedEvent feeFineBalanceChangedEvent = buildFeeFineBalanceChangedEvent(
        userId, loanId, feeFineId, feeFineTypeId, balance);
      waitFor(userSummaryService.updateUserSummaryWithEvent(userSummary, feeFineBalanceChangedEvent));
      waitFor(userSummaryRepository.get(summaryId)).ifPresent(userSummaryAfterFirstUpdate -> {
        userSummaryAfterFirstUpdate.setVersion(1);
        BigDecimal newBalance = new BigDecimal("7.77");
        feeFineBalanceChangedEvent.setBalance(newBalance);
        waitFor(
          userSummaryService.updateUserSummaryWithEvent(userSummaryAfterFirstUpdate, feeFineBalanceChangedEvent));
        waitFor(userSummaryRepository.get(summaryId)).ifPresent(
          userSummaryAfterSecondUpdate -> context.assertTrue(
            userSummaryAfterSecondUpdate.getOpenFeesFines().get(0).getBalance().equals(newBalance)));
      });
    });
  }

  @Test
  public void shouldUpdateNewUserSummaryIfUserSummaryDidNotExist(TestContext context) {
    final String userId = randomId();
    final String summaryId = randomId();
    final String feeFineId1 = randomId();
    final String feeFineId2 = randomId();
    final String loanId1 = randomId();
    final String loanId2 = randomId();
    final String feeFineTypeId1 = randomId();
    final String feeFineTypeId2 = randomId();
    BigDecimal balance1 = new BigDecimal("3.33");
    BigDecimal balance2 = new BigDecimal("7.77");
    FeeFineBalanceChangedEvent feeFineBalanceChangedEvent1 = buildFeeFineBalanceChangedEvent(
      userId, loanId1, feeFineId1, feeFineTypeId1, balance1);
    FeeFineBalanceChangedEvent feeFineBalanceChangedEvent2 = buildFeeFineBalanceChangedEvent(
      userId, loanId2, feeFineId2, feeFineTypeId2, balance2);

    UserSummary userSummary = createUserSummary(summaryId, userId);
    waitFor(userSummaryService.updateUserSummaryWithEvent(userSummary, feeFineBalanceChangedEvent1));
    waitFor(userSummaryService.updateUserSummaryWithEvent(userSummary, feeFineBalanceChangedEvent2));
    waitFor(userSummaryRepository.get(summaryId)).ifPresent(userSummaryAfterBothUpdates ->
      context.assertTrue(userSummaryAfterBothUpdates.getOpenFeesFines().stream()
        .allMatch(openFeeFine -> openFeeFine.getFeeFineId()
          .equals(feeFineBalanceChangedEvent1.getFeeFineId()) || openFeeFine.getFeeFineId()
          .equals(feeFineBalanceChangedEvent2.getFeeFineId()))
      ));
  }

  private UserSummary createUserSummary(String id, String userId) {
    return new UserSummary()
      .withId(id)
      .withUserId(userId);
  }
}

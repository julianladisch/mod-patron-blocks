package org.folio.rest.handlers;

import static java.util.Collections.singletonList;
import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;

import java.util.Date;

import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ItemClaimedReturnedEventHandlerTest extends EventHandlerTestBase {
  private static final ItemClaimedReturnedEventHandler eventHandler =
    new ItemClaimedReturnedEventHandler(postgresClient);

  @Before
  public void beforeEach(TestContext context) {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void userSummaryShouldBeCreatedWhenDoesNotExist(TestContext context) {
    String userId = randomId();
    String loanId = randomId();

    ItemClaimedReturnedEvent event = new ItemClaimedReturnedEvent()
      .withUserId(userId)
      .withLoanId(loanId);

    String summaryId = waitFor(eventHandler.handle(event));

    UserSummary userSummaryToCompare = new UserSummary()
      .withUserId(userId)
      .withOpenLoans(singletonList(new OpenLoan()
        .withLoanId(loanId)
        .withRecall(false)
        .withItemClaimedReturned(true)
        .withItemLost(false)));

    checkUserSummary(summaryId, userSummaryToCompare, context);
  }

  @Test
  public void newOpenLoanShouldBeCreatedForExistingSummaryWhenDoesNotExist(TestContext context) {
    String userId = randomId();
    String loanId = randomId();

    UserSummary userSummary = new UserSummary()
      .withUserId(userId);

    waitFor(userSummaryRepository.save(userSummary));

    ItemClaimedReturnedEvent event = new ItemClaimedReturnedEvent()
      .withUserId(userId)
      .withLoanId(loanId);

    String summaryId = waitFor(eventHandler.handle(event));

    UserSummary userSummaryToCompare = new UserSummary()
      .withUserId(userId)
      .withOpenLoans(singletonList(new OpenLoan()
        .withLoanId(loanId)
        .withRecall(false)
        .withItemClaimedReturned(true)
        .withItemLost(false)));

    checkUserSummary(summaryId, userSummaryToCompare, context);
  }

  @Test
  public void shouldFlipItemLostFlagWhenUserSummaryExists(TestContext context) {
    String userId = randomId();
    String loanId = randomId();

    UserSummary userSummary = new UserSummary()
      .withUserId(userId)
      .withOpenLoans(singletonList(
        new OpenLoan()
          .withDueDate(new Date())
          .withLoanId(loanId)
          .withRecall(false)
          .withItemClaimedReturned(false)
          .withItemLost(false)));

    waitFor(userSummaryRepository.save(userSummary));

    ItemClaimedReturnedEvent event = new ItemClaimedReturnedEvent()
      .withUserId(userId)
      .withLoanId(loanId);

    String summaryId = waitFor(eventHandler.handle(event));

    userSummary.getOpenLoans().get(0).setItemClaimedReturned(true);

    checkUserSummary(summaryId, userSummary, context);
  }
}

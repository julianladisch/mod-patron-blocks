package org.folio.rest.handlers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.folio.domain.OpenLoan;
import org.folio.domain.UserSummary;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ItemCheckedOutEventHandlerTest extends AbstractEventHandlerTest {
  private static final ItemCheckedOutEventHandler eventHandler =
    new ItemCheckedOutEventHandler(postgresClient);
  protected static final UserSummaryRepository userSummaryRepository =
    new UserSummaryRepository(postgresClient);

  @Test
  public void userSummaryShouldBeCreatedWhenDoesntExist(TestContext context) {
    String userId = randomId();
    String loanId = randomId();
    DateTime dueDate = DateTime.now();

    ItemCheckedOutEvent event = new ItemCheckedOutEvent()
      .withUserId(userId)
      .withLoanId(loanId)
      .withDueDate(dueDate.toDate());

    String summaryId = waitFor(eventHandler.handle(event));

    UserSummary userSummaryToCompare = new UserSummary()
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(BigDecimal.ZERO)
      .withOpenLoans(Collections.singletonList(new OpenLoan()
        .withLoanId(loanId)
        .withDueDate(dueDate.toDate())
        .withRecall(false)));

    checkUserSummary(summaryId, userSummaryToCompare, context, userSummaryRepository);
  }

  @Test
  public void shouldAddOpenLoanWhenUserSummaryExists(TestContext context) {
    String userId = randomId();
    String loanId = randomId();
    DateTime dueDate = DateTime.now();

    List<OpenLoan> existingOpenLoans = new ArrayList<>();
    existingOpenLoans.add(new OpenLoan()
      .withLoanId(randomId())
      .withDueDate(dueDate.toDate())
      .withRecall(false));

    UserSummary existingUserSummary = new UserSummary()
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(BigDecimal.ZERO)
      .withOpenLoans(existingOpenLoans);

    waitFor(userSummaryRepository.save(existingUserSummary));

    ItemCheckedOutEvent event = new ItemCheckedOutEvent()
      .withUserId(userId)
      .withLoanId(loanId)
      .withDueDate(dueDate.toDate());

    String summaryId = waitFor(eventHandler.handle(event));

    existingOpenLoans.add(new OpenLoan()
      .withLoanId(loanId)
      .withDueDate(dueDate.toDate())
      .withRecall(false));

    checkUserSummary(summaryId, existingUserSummary, context, userSummaryRepository);
  }

  @Test
  public void shouldFailWhenOpenLoanWithTheSameLoanIdExists(TestContext context) {
    String userId = randomId();
    String loanId = randomId();
    DateTime dueDate = DateTime.now();

    List<OpenLoan> existingOpenLoans = new ArrayList<>();
    existingOpenLoans.add(new OpenLoan()
      .withLoanId(loanId)
      .withDueDate(dueDate.toDate())
      .withRecall(false));

    UserSummary existingUserSummary = new UserSummary()
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(BigDecimal.ZERO)
      .withOpenLoans(existingOpenLoans);

    waitFor(userSummaryRepository.save(existingUserSummary));

    ItemCheckedOutEvent event = new ItemCheckedOutEvent()
      .withUserId(userId)
      .withLoanId(loanId)
      .withDueDate(dueDate.toDate());

    context.assertNull(waitFor(eventHandler.handle(event)));
  }
}

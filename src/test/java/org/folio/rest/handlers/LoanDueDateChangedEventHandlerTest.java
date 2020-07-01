package org.folio.rest.handlers;

import static java.util.Collections.singletonList;
import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;

import java.util.Date;
import java.util.Optional;

import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class LoanDueDateChangedEventHandlerTest extends EventHandlerTestBase {

  private static final LoanDueDateChangedEventHandler eventHandler =
    new LoanDueDateChangedEventHandler(postgresClient);

  @Before
  public void beforeEach(TestContext context) {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void createNewSummaryIfDoesNotExist(TestContext context) {
    String userId = randomId();

    Optional<UserSummary> summaryBeforeEvent = waitFor(userSummaryRepository.getByUserId(userId));
    context.assertFalse(summaryBeforeEvent.isPresent());

    LoanDueDateChangedEvent event = new LoanDueDateChangedEvent()
      .withUserId(userId)
      .withLoanId(randomId())
      .withDueDate(new Date())
      .withDueDateChangedByRecall(false);

    String summaryId = waitFor(eventHandler.handle(event));

    UserSummary expectedSummary = new UserSummary()
      .withId(summaryId)
      .withUserId(userId)
      .withOpenLoans(singletonList(new OpenLoan()
        .withLoanId(event.getLoanId())
        .withDueDate(event.getDueDate())
        .withRecall(event.getDueDateChangedByRecall())));

    checkUserSummary(summaryId, expectedSummary, context);
  }

  @Test
  public void createNewLoanInExistingSummaryIfItDoesNotExist(TestContext context) {
    String userId = randomId();

    UserSummary existingSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId);

    String createdSummaryId = waitFor(userSummaryRepository.save(existingSummary));

    Optional<UserSummary> summaryBeforeEvent = waitFor(userSummaryRepository.get(createdSummaryId));
    context.assertTrue(summaryBeforeEvent.isPresent());
    context.assertTrue(summaryBeforeEvent.get().getOpenLoans().isEmpty());

    LoanDueDateChangedEvent event = new LoanDueDateChangedEvent()
      .withUserId(userId)
      .withLoanId(randomId())
      .withDueDate(new Date())
      .withDueDateChangedByRecall(false);

    String updatedSummaryId = waitFor(eventHandler.handle(event));
    context.assertEquals(createdSummaryId, updatedSummaryId);

    UserSummary expectedSummary = existingSummary
      .withOpenLoans(singletonList(new OpenLoan()
        .withLoanId(event.getLoanId())
        .withDueDate(event.getDueDate())
        .withRecall(event.getDueDateChangedByRecall())));

    checkUserSummary(updatedSummaryId, expectedSummary, context);
  }

  @Test
  public void existingLoanIsUpdated(TestContext context) {
    String userId = randomId();

    OpenLoan existingLoan = new OpenLoan()
      .withLoanId(randomId())
      .withDueDate(new Date())
      .withRecall(false);

    UserSummary existingSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId);

    existingSummary.getOpenLoans().add(existingLoan);

    String createdSummaryId = waitFor(userSummaryRepository.save(existingSummary));

    Optional<UserSummary> summaryBeforeEvent = waitFor(userSummaryRepository.get(createdSummaryId));
    context.assertTrue(summaryBeforeEvent.isPresent());

    Date newDueDate = new DateTime(existingLoan.getDueDate())
      .plusDays(1)
      .toDate();

    LoanDueDateChangedEvent event = new LoanDueDateChangedEvent()
      .withUserId(userId)
      .withLoanId(existingLoan.getLoanId())
      .withDueDate(newDueDate)
      .withDueDateChangedByRecall(!existingLoan.getRecall());

    String updatedSummaryId = waitFor(eventHandler.handle(event));
    context.assertEquals(createdSummaryId, updatedSummaryId);

    UserSummary updatedSummary = existingSummary
      .withOpenLoans(singletonList(
        existingLoan.withDueDate(event.getDueDate())
          .withRecall(event.getDueDateChangedByRecall())));

    checkUserSummary(updatedSummaryId, updatedSummary, context);
  }
}
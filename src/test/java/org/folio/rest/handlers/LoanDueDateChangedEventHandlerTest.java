package org.folio.rest.handlers;

import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
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
public class LoanDueDateChangedEventHandlerTest extends TestBase {

  private static final LoanDueDateChangedEventHandler eventHandler =
    new LoanDueDateChangedEventHandler(postgresClient);
  private static final UserSummaryRepository userSummaryRepository =
    new UserSummaryRepository(postgresClient);

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

    Optional<UserSummary> optionalSummary = waitFor(userSummaryRepository.get(summaryId));
    context.assertTrue(optionalSummary.isPresent());

    UserSummary createdSummary = optionalSummary.get();

    context.assertEquals(userId, createdSummary.getUserId());
    context.assertEquals(1, createdSummary.getOpenLoans().size());

    OpenLoan openLoan = createdSummary.getOpenLoans().get(0);

    context.assertEquals(event.getLoanId(), openLoan.getLoanId());
    context.assertEquals(event.getDueDate(), openLoan.getDueDate());
    context.assertEquals(event.getDueDateChangedByRecall(), openLoan.getRecall());
  }

  @Test
  public void createNewLoanInExistingSummaryIfItDoesNotExist(TestContext context) {
    String userId = randomId();

    UserSummary existingSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(BigDecimal.ZERO);

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

    Optional<UserSummary> optionalSummary = waitFor(userSummaryRepository.get(updatedSummaryId));
    context.assertTrue(optionalSummary.isPresent());

    UserSummary updatedSummary = optionalSummary.get();

    context.assertEquals(userId, updatedSummary.getUserId());
    context.assertEquals(1, updatedSummary.getOpenLoans().size());

    OpenLoan openLoan = updatedSummary.getOpenLoans().get(0);

    context.assertEquals(event.getLoanId(), openLoan.getLoanId());
    context.assertEquals(event.getDueDate(), openLoan.getDueDate());
    context.assertEquals(event.getDueDateChangedByRecall(), openLoan.getRecall());
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
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(BigDecimal.ZERO);

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

    Optional<UserSummary> optionalSummary = waitFor(userSummaryRepository.get(updatedSummaryId));
    context.assertTrue(optionalSummary.isPresent());

    UserSummary updatedSummary = optionalSummary.get();

    context.assertEquals(userId, updatedSummary.getUserId());
    context.assertEquals(1, updatedSummary.getOpenLoans().size());

    OpenLoan openLoan = updatedSummary.getOpenLoans().get(0);

    context.assertEquals(event.getLoanId(), openLoan.getLoanId());
    context.assertEquals(event.getDueDate(), openLoan.getDueDate());
    context.assertEquals(event.getDueDateChangedByRecall(), openLoan.getRecall());
  }
}
package org.folio.rest.handlers;

import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
import org.folio.rest.jaxrs.model.ItemCheckedInEvent;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ItemCheckedInHandlerTest extends TestBase {
  private static final ItemCheckedInEventHandler eventHandler =
    new ItemCheckedInEventHandler(postgresClient);
  private static final UserSummaryRepository userSummaryRepository =
    new UserSummaryRepository(postgresClient);

  @Before
  public void beforeEach(TestContext context) {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void existingLoanIsRemovedFromSummary(TestContext context) {
    OpenLoan existingLoan1 = new OpenLoan()
      .withLoanId(randomId())
      .withDueDate(new Date())
      .withRecall(false);

    OpenLoan existingLoan2 = new OpenLoan()
      .withLoanId(randomId())
      .withDueDate(new Date())
      .withRecall(false);

    final String userId = randomId();

    UserSummary initialUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(BigDecimal.ZERO)
      .withOpenLoans(Arrays.asList(existingLoan1, existingLoan2));

    String savedSummaryId = waitFor(userSummaryRepository.save(initialUserSummary));

    ItemCheckedInEvent event = createEvent(userId, existingLoan2.getLoanId(), new Date());
    String handledSummaryId = waitFor(eventHandler.handle(event));
    context.assertEquals(savedSummaryId, handledSummaryId);

    Optional<UserSummary> optionalSummary = waitFor(userSummaryRepository.get(savedSummaryId));

    context.assertTrue(optionalSummary.isPresent());
    List<OpenLoan> openLoans = optionalSummary.get().getOpenLoans();
    context.assertEquals(1, openLoans.size());
    context.assertEquals(existingLoan1.getLoanId(), openLoans.get(0).getLoanId());
  }

  @Test
  public void existingSummaryRemainsIntactWhenWhenLoanDoesNotExist(TestContext context) {
    final String userId = randomId();

    OpenLoan existingLoan = new OpenLoan()
      .withLoanId(randomId())
      .withDueDate(new Date())
      .withRecall(false);

    UserSummary initialUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(BigDecimal.ZERO);

    initialUserSummary.getOpenLoans().add(existingLoan);

    String savedSummaryId = waitFor(userSummaryRepository.save(initialUserSummary));
    ItemCheckedInEvent event = createEvent(userId, randomId(), new Date());
    String handledSummaryId = waitFor(eventHandler.handle(event));

    context.assertNull(handledSummaryId);

    Optional<UserSummary> optionalSummary = waitFor(userSummaryRepository.get(savedSummaryId));

    context.assertTrue(optionalSummary.isPresent());
    List<OpenLoan> openLoans = optionalSummary.get().getOpenLoans();
    context.assertEquals(1, openLoans.size());
    context.assertEquals(existingLoan.getLoanId(), openLoans.get(0).getLoanId());
  }

  @Test
  public void eventIsIgnoredWhenSummaryForUserDoesNotExist(TestContext context) {
    String userId = randomId();
    ItemCheckedInEvent event = createEvent(userId, randomId(), new Date());

    String handledSummaryId = waitFor(eventHandler.handle(event));
    context.assertNull(handledSummaryId);

    Optional<UserSummary> optionalSummary = waitFor(userSummaryRepository.getByUserId(userId));
    context.assertFalse(optionalSummary.isPresent());
  }

  private static ItemCheckedInEvent createEvent(String userId, String loanId, Date returnDate) {
    return new ItemCheckedInEvent()
      .withUserId(userId)
      .withLoanId(loanId)
      .withReturnDate(returnDate);
  }

}
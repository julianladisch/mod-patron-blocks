package org.folio.rest.handlers;

import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;

import java.util.stream.IntStream;

import org.folio.domain.OpenLoan;
import org.folio.domain.UserSummary;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
import org.junit.Before;

import io.vertx.ext.unit.TestContext;

public class AbstractEventHandlerTest extends TestBase {
  protected static final UserSummaryRepository userSummaryRepository =
    new UserSummaryRepository(postgresClient);

  @Before
  public void beforeEach(TestContext context) {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  protected void checkUserSummary(String summaryId, UserSummary userSummaryToCompare,
    TestContext context) {

    UserSummary userSummary = waitFor(userSummaryRepository.get(summaryId)).orElseThrow(() ->
      new AssertionError("User summary was not found: " + summaryId));

    context.assertEquals(userSummaryToCompare.getUserId(), userSummary.getUserId());
    context.assertEquals(0, userSummaryToCompare.getOutstandingFeeFineBalance().compareTo(
      userSummary.getOutstandingFeeFineBalance()));
    context.assertEquals(userSummaryToCompare.getNumberOfLostItems(),
      userSummary.getNumberOfLostItems());
    context.assertEquals(userSummaryToCompare.getOpenLoans().size(),
      userSummary.getOpenLoans().size());

    IntStream.range(0, userSummary.getOpenLoans().size())
      .forEach(i -> {
        OpenLoan openLoan = userSummary.getOpenLoans().get(i);
        OpenLoan openLoanToCompare = userSummaryToCompare.getOpenLoans().get(i);
        context.assertEquals(openLoanToCompare.getLoanId(), openLoan.getLoanId());
        context.assertEquals(openLoanToCompare.getDueDate(), openLoan.getDueDate());
        context.assertEquals(openLoanToCompare.getReturnedDate(), openLoan.getReturnedDate());
        context.assertEquals(openLoanToCompare.getRecall(), openLoan.getRecall());
      });
  }
}

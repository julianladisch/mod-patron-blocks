package org.folio.rest.handlers;

import java.util.stream.IntStream;

import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;

import io.vertx.ext.unit.TestContext;

public class EventHandlerTestBase extends TestBase {

  protected final UserSummaryRepository userSummaryRepository =
    new UserSummaryRepository(postgresClient);

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
        context.assertEquals(openLoanToCompare.getRecall(), openLoan.getRecall());
      });
  }

}

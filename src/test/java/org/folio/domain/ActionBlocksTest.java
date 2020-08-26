package org.folio.domain;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.folio.domain.ActionBlocks.byLimit;
import static org.folio.domain.Condition.MAX_NUMBER_OF_LOST_ITEMS;
import static org.folio.domain.Condition.MAX_OUTSTANDING_FEE_FINE_BALANCE;
import static org.folio.util.UuidHelper.randomId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Date;

import org.folio.rest.jaxrs.model.OpenFeeFine;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.PatronBlockLimit;
import org.folio.rest.jaxrs.model.UserSummary;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class ActionBlocksTest {

  @Test
  @Parameters
  public void byLimitShouldReturnNoBlocks(UserSummary summary, PatronBlockLimit limit) {
    ActionBlocks actionBlocks = byLimit(summary, limit);
    assertAllBlocksAreFalse(actionBlocks);
  }

  public Object[] parametersForByLimitShouldReturnNoBlocks() {
    final UserSummary userSummary = new UserSummary();
    final PatronBlockLimit limit = new PatronBlockLimit();

    return new Object[] {
      new Object[] { null, null },
      new Object[] { null, limit.withValue(1.23).withConditionId(MAX_NUMBER_OF_LOST_ITEMS.getId()) },
      new Object[] { userSummary, null },
      new Object[] { userSummary, limit },
      new Object[] { userSummary, limit.withValue(1.23) }
    };
  }

  @Test
  public void emptyReturnsNoBlocks() {
    assertAllBlocksAreFalse(ActionBlocks.empty());
  }

  @Test
  public void byLimitReturnsEmptyBlocksWhenCalledWithUnknownConditionId() {
    PatronBlockLimit limit = new PatronBlockLimit()
      .withValue(1.23)
      .withConditionId(randomId());

    ActionBlocks actionBlocks = byLimit(new UserSummary(), limit);

    assertAllBlocksAreFalse(actionBlocks);
  }

  @Test
  @Parameters({
    "false | false | false | false",
    "true  | true  | true  | true",
    "true  | false | false | true",
    "false | true  | false | true",
    "false | false | true  | true",
    "true  | true  | false | true",
    "false | true  | true  | true",
    "true  | false | true  | true"
  })
  public void isNotEmptyTest(boolean blockBorrowing, boolean blockRenewals, boolean blockRequests,
    boolean expectedResult) {

    ActionBlocks actionBlocks = new ActionBlocks(blockBorrowing, blockRenewals, blockRequests);
    boolean isNotEmpty = actionBlocks.isNotEmpty();

    if (expectedResult) {
      assertTrue(isNotEmpty);
    } else {
      assertFalse(isNotEmpty);
    }
  }

  @Test
  public void byLimitReturnsNoBlocksForOutstandingFeeFineBalanceWhenItemIsClaimedReturned() {
    final PatronBlockLimit limit = new PatronBlockLimit()
      .withPatronGroupId(randomId())
      .withConditionId(MAX_OUTSTANDING_FEE_FINE_BALANCE.getId())
      .withValue(0.33);

    String firstLoanId = randomId();
    String secondLoanId = randomId();

    final UserSummary userSummary = new UserSummary()
      .withOpenLoans(asList(
        new OpenLoan()
          .withLoanId(firstLoanId)
          .withDueDate(new Date())
          .withItemLost(false)
          .withRecall(false)
          .withItemClaimedReturned(true),
        new OpenLoan()
          .withLoanId(secondLoanId)
          .withDueDate(new Date())
          .withItemLost(false)
          .withRecall(false)
          .withItemClaimedReturned(true)))
      .withOpenFeesFines(asList(
        new OpenFeeFine()
          .withBalance(BigDecimal.ONE)
          .withFeeFineId(randomId())
          .withFeeFineTypeId(randomId())
          .withLoanId(secondLoanId),
        new OpenFeeFine()
          .withBalance(BigDecimal.TEN)
          .withFeeFineId(randomId())
          .withFeeFineTypeId(randomId())
          .withLoanId(firstLoanId)));

    assertAllBlocksAreFalse(byLimit(userSummary, limit));
  }

  @Test
  public void byLimitReturnsAllBlocksForBalanceWhenItemClaimedReturnedButFeeFineHasNoLoanId() {
    final PatronBlockLimit limit = new PatronBlockLimit()
      .withPatronGroupId(randomId())
      .withConditionId(MAX_OUTSTANDING_FEE_FINE_BALANCE.getId())
      .withValue(5.00);

    String loanId = randomId();

    final UserSummary userSummary = new UserSummary()
      .withOpenLoans(singletonList(
        new OpenLoan()
          .withLoanId(loanId)
          .withDueDate(new Date())
          .withItemLost(false)
          .withRecall(false)
          .withItemClaimedReturned(true)))
      .withOpenFeesFines(singletonList(
        new OpenFeeFine()
          .withBalance(BigDecimal.TEN)
          .withFeeFineId(randomId())
          .withFeeFineTypeId(randomId())
          .withLoanId(null)));

    assertAllBlocksAreTrue(byLimit(userSummary, limit));
  }

  private static void assertAllBlocksAreFalse(ActionBlocks actionBlocks) {
    assertFalse(actionBlocks.getBlockBorrowing());
    assertFalse(actionBlocks.getBlockRenewals());
    assertFalse(actionBlocks.getBlockRequests());
  }

  private static void assertAllBlocksAreTrue(ActionBlocks actionBlocks) {
    assertTrue(actionBlocks.getBlockBorrowing());
    assertTrue(actionBlocks.getBlockRenewals());
    assertTrue(actionBlocks.getBlockRequests());
  }
}

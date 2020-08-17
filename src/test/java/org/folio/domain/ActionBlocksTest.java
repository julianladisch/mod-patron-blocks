package org.folio.domain;

import static org.folio.domain.Condition.MAX_NUMBER_OF_LOST_ITEMS;
import static org.folio.util.UuidHelper.randomId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
  public void shouldReturnEmptyBlocks(UserSummary summary, PatronBlockLimit limit) {
    ActionBlocks actionBlocks = ActionBlocks.byLimit(summary, limit);
    assertAllBlocksAreFalse(actionBlocks);
  }

  public Object[] parametersForShouldReturnEmptyBlocks() {
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
  public void emptyReturnsEmptyBlocks() {
    assertAllBlocksAreFalse(ActionBlocks.empty());
  }

  @Test
  public void byLimitReturnsEmptyBlocksWhenCalledWithUnknownConditionId() {
    PatronBlockLimit limit = new PatronBlockLimit()
      .withValue(1.23)
      .withConditionId(randomId());

    ActionBlocks actionBlocks = ActionBlocks.byLimit(new UserSummary(), limit);

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

  private static void assertAllBlocksAreFalse(ActionBlocks actionBlocks) {
    assertFalse(actionBlocks.getBlockBorrowing());
    assertFalse(actionBlocks.getBlockRenewals());
    assertFalse(actionBlocks.getBlockRequests());
  }
}

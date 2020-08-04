package org.folio.domain;

import static org.folio.util.UuidHelper.randomId;
import static org.junit.Assert.assertFalse;

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
      new Object[] { userSummary, null },
      new Object[] { userSummary, limit },
      new Object[] { userSummary, limit.withValue(1.23) },
      new Object[] { userSummary, limit.withValue(1.23).withConditionId(randomId()) }
    };
  }

  @Test
  public void emptyReturnsEmptyBlocks() {
    assertAllBlocksAreFalse(ActionBlocks.empty());
  }

  private static void assertAllBlocksAreFalse(ActionBlocks actionBlocks) {
    assertFalse(actionBlocks.getBlockBorrowing());
    assertFalse(actionBlocks.getBlockRenewals());
    assertFalse(actionBlocks.getBlockRequests());
  }
}
package org.folio.domain;

import static org.folio.domain.Condition.MAX_NUMBER_OF_ITEMS_CHARGED_OUT;
import static org.folio.domain.Condition.MAX_NUMBER_OF_LOST_ITEMS;
import static org.folio.domain.Condition.MAX_NUMBER_OF_OVERDUE_ITEMS;
import static org.folio.domain.Condition.MAX_NUMBER_OF_OVERDUE_RECALLS;
import static org.folio.domain.Condition.MAX_OUTSTANDING_FEE_FINE_BALANCE;
import static org.folio.domain.Condition.RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import org.folio.rest.jaxrs.model.OpenFeeFine;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.PatronBlockLimit;
import org.folio.rest.jaxrs.model.UserSummary;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ActionBlocks {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final boolean blockBorrowing;
  private final boolean blockRenewals;
  private final boolean blockRequests;

  public static ActionBlocks byLimit(UserSummary userSummary, PatronBlockLimit limit) {
    if (userSummary == null || limit == null || limit.getValue() == null
      || limit.getConditionId() == null) {

      log.error("Failed to determine blocks: one of the parameters is null");
      return empty();
    }

    Condition condition = Condition.getById(limit.getConditionId());

    boolean blockBorrowing = false;
    boolean blockRenewals = false;
    boolean blockRequests = false;

    double limitValue = limit.getValue();

    if (condition == MAX_NUMBER_OF_ITEMS_CHARGED_OUT) {
      int numberOfOpenLoans = (int) userSummary.getOpenLoans().stream()
        .filter(ActionBlocks::itemIsNotClaimedReturned)
        .count();

      blockBorrowing = numberOfOpenLoans >= limitValue;
      blockRenewals = blockRequests = numberOfOpenLoans > limitValue;
    }
    else if (condition == MAX_NUMBER_OF_LOST_ITEMS) {
      blockBorrowing = blockRenewals = blockRequests = userSummary.getOpenLoans().stream()
        .filter(ActionBlocks::itemIsNotClaimedReturned)
        .filter(OpenLoan::getItemLost)
        .count() > limitValue;
    }
    else if (condition == MAX_NUMBER_OF_OVERDUE_ITEMS) {
      blockBorrowing = blockRenewals = blockRequests = userSummary.getOpenLoans().stream()
        .filter(ActionBlocks::itemIsNotClaimedReturned)
        .filter(ActionBlocks::isLoanOverdue)
        .count() > limitValue;
    }
    else if (condition == MAX_NUMBER_OF_OVERDUE_RECALLS) {
      blockBorrowing = blockRenewals = blockRequests = userSummary.getOpenLoans().stream()
        .filter(ActionBlocks::itemIsNotClaimedReturned)
        .filter(ActionBlocks::isLoanOverdue)
        .filter(OpenLoan::getRecall)
        .count() > limitValue;
    }
    else if (condition == RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS) {
      blockBorrowing = blockRenewals = blockRequests = userSummary.getOpenLoans().stream()
        .filter(ActionBlocks::itemIsNotClaimedReturned)
        .filter(ActionBlocks::isLoanOverdue)
        .filter(OpenLoan::getRecall)
        .map(ActionBlocks::getLoanOverdueDays)
        .anyMatch(days -> days > limitValue);
    }
    else if (condition == MAX_OUTSTANDING_FEE_FINE_BALANCE) {
      blockBorrowing = blockRenewals = blockRequests = userSummary.getOpenFeesFines().stream()
        .filter(feeFine -> feeFineIsNotRelatedToItemClaimedReturned(feeFine, userSummary))
        .map(OpenFeeFine::getBalance)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .compareTo(BigDecimal.valueOf(limitValue)) > 0;
    }

    return new ActionBlocks(blockBorrowing, blockRenewals, blockRequests);
  }

  public ActionBlocks(boolean blockBorrowing, boolean blockRenewals, boolean blockRequests) {
    this.blockBorrowing = blockBorrowing;
    this.blockRenewals = blockRenewals;
    this.blockRequests = blockRequests;
  }

  public boolean getBlockBorrowing() {
    return blockBorrowing;
  }

  public boolean getBlockRenewals() {
    return blockRenewals;
  }

  public boolean getBlockRequests() {
    return blockRequests;
  }

  public boolean isNotEmpty() {
    return blockBorrowing || blockRenewals || blockRequests;
  }

  public static ActionBlocks and(ActionBlocks left, ActionBlocks right) {
    return new ActionBlocks(left.blockBorrowing && right.blockBorrowing,
      left.blockRenewals && right.blockRenewals,
      left.blockRequests && right.blockRequests);
  }

  public static ActionBlocks empty() {
    return new ActionBlocks(false, false, false);
  }

  private static boolean isLoanOverdue(OpenLoan loan) {
    Date dueDate = loan.getDueDate();
    return dueDate != null && dueDate.before(new Date());
  }

  private static int getLoanOverdueDays(OpenLoan loan) {
    return isLoanOverdue(loan)
      ? (int) Math.round(((double) (new Date().getTime() - loan.getDueDate().getTime()))
      / 1000.0 / 60.0 / 60.0 / 24.0)
      : 0;
  }

  private static boolean itemIsNotClaimedReturned(OpenLoan loan) {
    return !loan.getItemClaimedReturned();
  }

  private static boolean feeFineIsNotRelatedToItemClaimedReturned(OpenFeeFine feeFine,
    UserSummary userSummary) {

    if (feeFine.getLoanId() == null) {
      return true;
    }

    return userSummary.getOpenLoans().stream()
      .filter(OpenLoan::getItemClaimedReturned)
      .map(OpenLoan::getLoanId)
      .filter(Objects::nonNull)
      .noneMatch(loanId -> loanId.equals(feeFine.getLoanId()));
  }
}

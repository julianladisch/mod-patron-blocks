package org.folio.rest.utils;

import static org.folio.util.UuidHelper.randomId;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.rest.jaxrs.model.OpenFeeFine;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.util.UuidHelper;

public class EntityBuilder {

  public static OpenLoan buildLoan(boolean recall, boolean itemLost, Date dueDate) {
    return new OpenLoan()
      .withLoanId(randomId())
      .withDueDate(dueDate)
      .withRecall(recall)
      .withItemLost(itemLost);
  }

  public static OpenFeeFine buildFeeFine(String loanId, String feeFineId, String feeFineTypeId,
    BigDecimal balance) {

    return new OpenFeeFine()
      .withLoanId(loanId)
      .withFeeFineId(feeFineId)
      .withFeeFineTypeId(feeFineTypeId)
      .withBalance(balance);
  }

  public static UserSummary buildUserSummary(String userId, List<OpenFeeFine> feesFines,
    List<OpenLoan> openLoans) {

    return new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withOpenFeesFines(feesFines)
      .withOpenLoans(openLoans);
  }
}

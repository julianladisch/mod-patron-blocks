package org.folio.service;

import static java.util.Optional.ofNullable;
import static org.joda.time.Minutes.minutesBetween;

import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.util.Period;
import org.joda.time.DateTime;

public class OverduePeriodCalculator {
  private static final int ZERO_MINUTES = 0;

  private OverduePeriodCalculator() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  public static int calculateOverdueMinutes(OpenLoan loan) {
    final DateTime systemTime = DateTime.now();

    return loanIsOverdue(loan, systemTime)
      ? calculateOverdueMinutes(loan, systemTime)
      : ZERO_MINUTES;
  }

  private static boolean loanIsOverdue(OpenLoan loan, DateTime systemTime) {
    return loan.getDueDate().before(systemTime.toDate());
  }

  private static Integer calculateOverdueMinutes(OpenLoan loan, DateTime systemTime) {
    DateTime dueDate = new DateTime(loan.getDueDate());
    int overdueMinutes = minutesBetween(dueDate, systemTime).getMinutes();

    return overdueMinutes > getGracePeriodMinutes(loan)
      ? overdueMinutes
      : ZERO_MINUTES;
  }

  private static int getGracePeriodMinutes(OpenLoan loan) {
    return ofNullable(loan.getGracePeriod())
      .map(Period::from)
      .map(Period::toMinutes)
      .orElse(ZERO_MINUTES);
  }
}

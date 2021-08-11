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

  public static int calculateOverdueMinutes(OpenLoan openLoan) {
    final DateTime systemTime = DateTime.now();

    return loanIsOverdue(openLoan, systemTime)
      ? calculateOverdueMinutes(openLoan, systemTime)
      : ZERO_MINUTES;
  }

  private static boolean loanIsOverdue(OpenLoan openLoan, DateTime systemTime) {
    return openLoan.getDueDate().before(systemTime.toDate());
  }

  private static Integer calculateOverdueMinutes(OpenLoan openLoan, DateTime systemTime) {
    DateTime dueDate = new DateTime(openLoan.getDueDate());
    int overdueMinutes = minutesBetween(dueDate, systemTime).getMinutes();

    return overdueMinutes > getGracePeriodMinutes(openLoan)
      ? overdueMinutes
      : ZERO_MINUTES;
  }

  private static int getGracePeriodMinutes(OpenLoan openLoan) {
    return ofNullable(openLoan.getGracePeriod())
      .map(Period::from)
      .map(Period::toMinutes)
      .orElse(ZERO_MINUTES);
  }
}

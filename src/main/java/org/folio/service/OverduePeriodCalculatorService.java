package org.folio.service;

import static org.joda.time.Minutes.minutesBetween;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.exception.OverduePeriodCalculatorException;
import org.folio.rest.jaxrs.model.GracePeriod;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.util.Period;
import org.joda.time.DateTime;

public class OverduePeriodCalculatorService {
  private static final Logger log = LogManager.getLogger(OverduePeriodCalculatorService.class);

  private static final int ZERO_MINUTES = 0;

  public int getMinutes(OpenLoan loan, DateTime systemTime, GracePeriod gracePeriod)
    throws OverduePeriodCalculatorException {
    if (loan == null || loan.getDueDate() == null || systemTime == null) {

      String message = "Failed to calculate overdue minutes. One of the parameters is null: " +
        "loan, overdue fine policy, loan policy, due date, system time";
      log.error(message);
      throw new OverduePeriodCalculatorException(message);
    }

    if (loanIsOverdue(loan, systemTime)) {
      return adjustOverdueWithGracePeriod(gracePeriod, calculateOverdueMinutes(loan, systemTime));
    } else {
      return ZERO_MINUTES;
    }
  }

  private boolean loanIsOverdue(OpenLoan loan, DateTime systemTime) {
    return loan.getDueDate().before(systemTime.toDate());
  }

  private int calculateOverdueMinutes(OpenLoan loan, DateTime systemTime) {
    return minutesBetween(new DateTime(loan.getDueDate()), systemTime).getMinutes();
  }

  private Integer adjustOverdueWithGracePeriod(GracePeriod gracePeriod, int overdueMinutes) {
    return overdueMinutes > getGracePeriodMinutes(gracePeriod) ? overdueMinutes : ZERO_MINUTES;
  }

  private int getGracePeriodMinutes(GracePeriod gracePeriod) {
    return Optional.ofNullable(gracePeriod)
      .map(gp -> Period.from(gp.getDuration(), gp.getIntervalId().value()))
      .map(Period::toMinutes)
      .orElse(ZERO_MINUTES);
  }
}

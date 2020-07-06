package org.folio.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;
import static org.joda.time.DateTimeConstants.MINUTES_PER_HOUR;
import static org.joda.time.DateTimeZone.UTC;
import static org.joda.time.Minutes.minutesBetween;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.domain.OpeningDayWithTimeZone;
import org.folio.exception.OverduePeriodCalculatorException;
import org.folio.rest.client.CalendarClient;
import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.client.FeesFinesClient;
import org.folio.rest.client.InventoryStorageClient;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;
import org.folio.rest.jaxrs.model.LoansPolicy;
import org.folio.rest.jaxrs.model.OpeningHour;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.util.Period;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OverduePeriodCalculatorService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int ZERO_MINUTES = 0;
  private static final Double NUMBER_OF_MINUTES_IN_ONE_DAY = 1440.0;

  private final CalendarClient calendarClient;
  private final CirculationStorageClient circulationStorageClient;
  private final FeesFinesClient feesFinesClient;
  private final InventoryStorageClient inventoryStorageClient;

  public OverduePeriodCalculatorService(Vertx vertx, Map<String, String> okapiHeaders) {
    this.calendarClient = new CalendarClient(vertx, okapiHeaders);
    this.circulationStorageClient = new CirculationStorageClient(vertx, okapiHeaders);
    this.feesFinesClient = new FeesFinesClient(vertx, okapiHeaders);
    this.inventoryStorageClient = new InventoryStorageClient(vertx, okapiHeaders);
  }

  public Future<Integer> getMinutes(Loan loan, DateTime systemTime) {
    if (loan.getOverdueFinePolicyId() == null || loan.getLoanPolicyId() == null) {
      String message = format("Loan ID %s - overdue fine policy or loan policy is missing",
        loan.getId());
      log.error(message);
      return failedFuture(new OverduePeriodCalculatorException(message));
    }

    return succeededFuture(new CalculationContext())
      .map(ctx -> ctx.withLoan(loan))
      .compose(ctx -> feesFinesClient.findOverdueFinePolicyById(loan.getOverdueFinePolicyId())
        .map(ctx::withOverdueFinePolicy))
      .compose(ctx -> circulationStorageClient.findLoanPolicyById(loan.getLoanPolicyId())
        .map(ctx::withLoanPolicy))
      .compose(ctx -> {
        if (preconditionsAreMet(ctx, systemTime)) {
          return succeededFuture(ctx)
            .compose(r -> getOverdueMinutes(ctx, systemTime)
              .map(om -> adjustOverdueWithGracePeriod(ctx, om)));
        }
        else {
          return succeededFuture(ZERO_MINUTES);
        }
      });
  }

  private boolean preconditionsAreMet(CalculationContext context, DateTime systemTime) {
    return context.getOverdueFinePolicy().getCountClosed() != null
      && loanIsOverdue(context.getLoan(), systemTime);
  }

  private boolean loanIsOverdue(Loan loan, DateTime systemTime) {
    return ObjectUtils.allNotNull(loan, systemTime)
      && loan.getDueDate().before(systemTime.toDate());
  }

  Future<Integer> getOverdueMinutes(CalculationContext calculationContext, DateTime systemTime) {
    boolean shouldCountClosedPeriods = calculationContext.getOverdueFinePolicy().getCountClosed();
    Loan loan = calculationContext.getLoan();

    return getItemLocationPrimaryServicePoint(loan)
      .compose(servicePointId -> shouldCountClosedPeriods || servicePointId == null
          ? minutesOverdueIncludingClosedPeriods(loan, systemTime)
          : minutesOverdueExcludingClosedPeriods(loan, servicePointId, systemTime));
  }

  private Future<Integer> minutesOverdueIncludingClosedPeriods(Loan loan, DateTime systemTime) {
    int overdueMinutes = minutesBetween(new DateTime(loan.getDueDate()), systemTime).getMinutes();
    return succeededFuture(overdueMinutes);
  }

  private Future<Integer> minutesOverdueExcludingClosedPeriods(Loan loan,
    UUID primaryServicePointId, DateTime returnDate) {

    DateTime dueDate = new DateTime(loan.getDueDate());
    return calendarClient
        .fetchOpeningDaysBetweenDates(primaryServicePointId.toString(), dueDate, returnDate, false)
        .map(openingDays -> getOpeningDaysDurationMinutes(openingDays, dueDate.toLocalDateTime(),
          returnDate.toLocalDateTime()));
  }

  private Integer getOpeningDaysDurationMinutes(
    Collection<OpeningDayWithTimeZone> openingDays, LocalDateTime dueDate, LocalDateTime returnDate) {

    return openingDays.stream()
        .mapToInt(day -> getOpeningDayDurationMinutes(day, dueDate, returnDate))
        .sum();
  }

  private int getOpeningDayDurationMinutes(OpeningDayWithTimeZone openingDayWithTimeZone,
    LocalDateTime dueDate, LocalDateTime systemTime) {

    DateTime datePart = openingDayWithTimeZone.getDateTime();

    return openingDayWithTimeZone.getOpeningDay().getOpeningHour()
      .stream()
      .mapToInt(openingHour -> getOpeningHourDurationMinutes(
        openingHour, datePart, dueDate, systemTime))
      .sum();
  }

  private int getOpeningHourDurationMinutes(OpeningHour openingHour,
    DateTime datePart, LocalDateTime dueDate, LocalDateTime returnDate) {

    if (allNotNull(datePart, dueDate, openingHour.getStartTime(), openingHour.getEndTime())) {

      LocalDateTime startTime =  datePart.withTime(LocalTime.parse(openingHour.getStartTime()))
        .withZone(UTC).toLocalDateTime();
      LocalDateTime endTime = datePart.withTime(LocalTime.parse(openingHour.getEndTime()))
        .withZone(UTC).toLocalDateTime();

      if (dueDate.isAfter(startTime) && dueDate.isBefore(endTime)) {
        startTime = dueDate;
      }

      if (returnDate.isAfter(startTime) && returnDate.isBefore(endTime)) {
        endTime = returnDate;
      }

      if (endTime.isAfter(startTime) && endTime.isAfter(dueDate)
        && startTime.isBefore(returnDate)) {

        return calculateDiffInMinutes(startTime, endTime);
      }
    }

    return ZERO_MINUTES;
  }

  private int calculateDiffInMinutes(LocalDateTime start, LocalDateTime end) {
    org.joda.time.Period period = new org.joda.time.Period(start, end);
    return period.getHours() * MINUTES_PER_HOUR + period.getMinutes();
  }

  private Integer adjustOverdueWithGracePeriod(CalculationContext context, int overdueMinutes) {
    int result;

    if (shouldIgnoreGracePeriod(context)) {
      result = overdueMinutes;
    }
    else {
      result = overdueMinutes > getGracePeriodMinutes(context) ? overdueMinutes : ZERO_MINUTES;
    }

    return result;
  }

  private boolean shouldIgnoreGracePeriod(CalculationContext context) {
    boolean dueDateChangedByRecall = context.getLoan().getDueDateChangedByRecall();
    if (!dueDateChangedByRecall) {
      return false;
    }

    Boolean ignoreGracePeriodForRecalls = context.getOverdueFinePolicy().getGracePeriodRecall();

    return ignoreGracePeriodForRecalls == null || ignoreGracePeriodForRecalls;
  }

  private int getGracePeriodMinutes(CalculationContext context) {
    return Optional.ofNullable(context)
      .map(CalculationContext::getLoanPolicy)
      .map(LoanPolicy::getLoansPolicy)
      .map(LoansPolicy::getGracePeriod)
      .map(gp -> Period.from(gp.getDuration(), gp.getIntervalId().value()))
      .map(Period::toMinutes)
      .orElse(ZERO_MINUTES);
  }

  private Future<UUID> getItemLocationPrimaryServicePoint(Loan loan) {
    return succeededFuture(loan)
      .compose(l -> inventoryStorageClient.findItemById(l.getItemId()))
      .compose(i -> inventoryStorageClient.findLocationById(i.getEffectiveLocationId()))
      .map(location -> Optional.ofNullable(location.getPrimaryServicePoint())
        .map(UUID::fromString)
        .orElse(null));
  }

  public static int getLoanOverdueDays(Integer overdueMinutes) {
    return (int) Math.ceil(overdueMinutes.doubleValue() / NUMBER_OF_MINUTES_IN_ONE_DAY);
  }

  private static class CalculationContext {
    final Loan loan;
    final OverdueFinePolicy overdueFinePolicy;
    final LoanPolicy loanPolicy;

    public CalculationContext() {
      this.loan = null;
      this.overdueFinePolicy = null;
      this.loanPolicy = null;
    }

    public CalculationContext(Loan loan, OverdueFinePolicy overdueFinePolicy,
      LoanPolicy loanPolicy) {

      this.loan = loan;
      this.overdueFinePolicy = overdueFinePolicy;
      this.loanPolicy = loanPolicy;
    }

    public CalculationContext withLoan(Loan loan) {
      return new CalculationContext(loan, this.overdueFinePolicy, this.loanPolicy);
    }

    public CalculationContext withOverdueFinePolicy(OverdueFinePolicy overdueFinePolicy) {
      return new CalculationContext(this.loan, overdueFinePolicy, this.loanPolicy);
    }

    public CalculationContext withLoanPolicy(LoanPolicy loanPolicy) {
      return new CalculationContext(this.loan, this.overdueFinePolicy, loanPolicy);
    }

    public Loan getLoan() {
      return loan;
    }

    public OverdueFinePolicy getOverdueFinePolicy() {
      return overdueFinePolicy;
    }

    public LoanPolicy getLoanPolicy() {
      return loanPolicy;
    }
  }
}

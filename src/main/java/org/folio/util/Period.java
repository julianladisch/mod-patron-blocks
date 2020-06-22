package org.folio.util;


import static org.joda.time.DateTimeConstants.MINUTES_PER_DAY;
import static org.joda.time.DateTimeConstants.MINUTES_PER_HOUR;
import static org.joda.time.DateTimeConstants.MINUTES_PER_WEEK;

public class Period {
  private static final String MONTHS = "Months";
  private static final String WEEKS = "Weeks";
  private static final String DAYS = "Days";
  private static final String HOURS = "Hours";
  private static final String MINUTES = "Minutes";

  private static final int MINUTES_PER_MONTH = MINUTES_PER_DAY * 31;

  private final Integer duration;
  private final String interval;

  private Period(Integer duration, String interval) {
    this.duration = duration;
    this.interval = interval;
  }

  public static Period from(Integer duration, String interval) {
    return new Period(duration, interval);
  }

  public int toMinutes() {
    if (duration == null || interval == null) {
      return 0;
    }

    switch (interval) {
      case MONTHS:
        return duration * MINUTES_PER_MONTH;
      case WEEKS:
        return duration * MINUTES_PER_WEEK;
      case DAYS:
        return duration * MINUTES_PER_DAY;
      case HOURS:
        return duration * MINUTES_PER_HOUR;
      case MINUTES:
        return duration;
      default:
        return 0;
    }
  }

}

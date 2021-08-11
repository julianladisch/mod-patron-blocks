package org.folio.util;


import static org.joda.time.DateTimeConstants.MINUTES_PER_DAY;
import static org.joda.time.DateTimeConstants.MINUTES_PER_HOUR;
import static org.joda.time.DateTimeConstants.MINUTES_PER_WEEK;

import java.util.HashMap;
import java.util.Map;

import org.folio.rest.jaxrs.model.GracePeriod;

public class Period {
  private static final String MONTHS = "Months";
  private static final String WEEKS = "Weeks";
  private static final String DAYS = "Days";
  private static final String HOURS = "Hours";
  private static final String MINUTES = "Minutes";

  private static final int MINUTES_PER_MONTH = MINUTES_PER_DAY * 31;
  private static final Map<String, Integer> MINUTES_IN_PERIOD;

  static {
    MINUTES_IN_PERIOD = new HashMap<>();
    MINUTES_IN_PERIOD.put(MONTHS, MINUTES_PER_MONTH);
    MINUTES_IN_PERIOD.put(WEEKS, MINUTES_PER_WEEK);
    MINUTES_IN_PERIOD.put(DAYS, MINUTES_PER_DAY);
    MINUTES_IN_PERIOD.put(HOURS, MINUTES_PER_HOUR);
    MINUTES_IN_PERIOD.put(MINUTES, 1);
  }

  private final Integer duration;
  private final String interval;

  private Period(Integer duration, String interval) {
    this.duration = duration;
    this.interval = interval;
  }

  public static Period from(Integer duration, String interval) {
    return new Period(duration, interval);
  }

  public static Period from(GracePeriod gracePeriod) {
    return new Period(gracePeriod.getDuration(), gracePeriod.getIntervalId().value());
  }

  public int toMinutes() {
    if (duration == null || interval == null) {
      return 0;
    }

    Integer minutesInPeriod = MINUTES_IN_PERIOD.get(interval);

    if (minutesInPeriod == null) {
      return 0;
    }

    return duration * minutesInPeriod;
  }

}

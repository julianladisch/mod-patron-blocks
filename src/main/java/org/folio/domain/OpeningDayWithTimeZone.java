package org.folio.domain;

import org.folio.rest.jaxrs.model.OpeningDay;
import org.joda.time.DateTime;

public class OpeningDayWithTimeZone {
  private final OpeningDay openingDay;
  private final DateTime dateTime;

  public OpeningDayWithTimeZone(OpeningDay openingDay, DateTime dateTime) {
    this.openingDay = openingDay;
    this.dateTime = dateTime;
  }

  public OpeningDay getOpeningDay() {
    return openingDay;
  }

  public DateTime getDateTime() {
    return dateTime;
  }
}

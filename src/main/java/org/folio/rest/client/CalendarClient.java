package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.folio.domain.OpeningDayWithTimeZone;
import org.folio.exception.EntityNotFoundException;
import org.folio.rest.jaxrs.model.OpeningDay;
import org.folio.rest.jaxrs.model.OpeningPeriod;
import org.folio.rest.jaxrs.model.OpeningPeriods;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class CalendarClient extends OkapiClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PERIODS_QUERY_PARAMS = "servicePointId=%s&startDate=%s&endDate=%s&includeClosedDays=%s";
  private static final String OPENING_PERIODS = "openingPeriods";
  private static final String DATE_KEY = "date";
  private static final String ALL_DAY_KEY = "allDay";
  private static final String OPEN_KEY = "open";
  private static final String OPENING_HOUR_KEY = "openingHour";
  private static final String OPENING_DAY_KEY = "openingDay";
  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private static final String DATE_PART_FORMAT = "yyyy-MM-dd";
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
    DateTimeFormat.forPattern(DATE_TIME_FORMAT).withZoneUTC();

  private final ConfigurationsClient configurationsClient;

  public CalendarClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(WebClient.create(vertx), okapiHeaders);

    configurationsClient = new ConfigurationsClient(vertx, okapiHeaders);
  }

  public Future<Collection<OpeningDayWithTimeZone>> fetchOpeningDaysBetweenDates(String servicePointId,
    DateTime startDate, DateTime endDate, boolean includeClosedDays) {

    Promise<HttpResponse<Buffer>> promise = Promise.promise();

    String params = String.format(PERIODS_QUERY_PARAMS,
      servicePointId, startDate.toLocalDate(), endDate.toLocalDate().plusDays(1), includeClosedDays);
    String path = String.format("/calendar/periods?%s", params);

    getAbs(path).send(promise);

    return promise.future().compose(response -> {
      int responseStatus = response.statusCode();
      if (responseStatus != 200) {
        String errorMessage = String.format("Failed to fetch by ID: %s. Response: %d %s",
          path, responseStatus, response.bodyAsString());
        log.error(errorMessage);
        return failedFuture(new EntityNotFoundException(errorMessage));
      } else {
        try {
          OpeningPeriods openingPeriods = objectMapper.readValue(
            response.bodyAsString(), OpeningPeriods.class);

          List<OpeningPeriod> openingPeriodList = Optional.ofNullable(openingPeriods)
            .map(OpeningPeriods::getOpeningPeriods)
            .orElse(new ArrayList<>());

          return configurationsClient.findTimeZone()
            .compose(tz -> succeededFuture(openingPeriodList.stream()
                .map(period -> fromOpeningPeriodJson(period, tz))
                .collect(Collectors.toList())));

        } catch (JsonProcessingException e) {
          log.error("Failed to parse response: " + response.bodyAsString());
          return failedFuture(e);
        }
      }
    });
  }

  public static OpeningDayWithTimeZone fromOpeningPeriodJson(OpeningPeriod openingPeriod,
    DateTimeZone zone) {

    OpeningDay openingDay = openingPeriod.getOpeningDay();
    Date dateProperty = openingPeriod.getDate();

    DateTime date = null;
    if (dateProperty != null) {
      date = new DateTime(dateProperty).withZoneRetainFields(zone);
    }

    return new OpeningDayWithTimeZone(openingDay, date);
  }
}

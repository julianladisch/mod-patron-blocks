package org.folio.rest.handlers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.folio.domain.EventType;
import org.folio.repository.BaseRepository;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.UuidHelper;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class LoanDueDateChangedEventHandler extends EventHandler<LoanDueDateChangedEvent> {
  private static final String EVENT_TABLE_NAME = EventType.LOAN_DUE_DATE_CHANGED.getTableName();

  private final BaseRepository<LoanDueDateChangedEvent> eventRepository;

  public LoanDueDateChangedEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
    eventRepository = new BaseRepository<>(getPostgresClient(okapiHeaders, vertx), EVENT_TABLE_NAME,
      LoanDueDateChangedEvent.class);
  }

  public LoanDueDateChangedEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
    eventRepository = new BaseRepository<>(postgresClient, EVENT_TABLE_NAME,
      LoanDueDateChangedEvent.class);
  }

  @Override
  public Future<String> handle(LoanDueDateChangedEvent event) {
    return eventRepository.save(event, UuidHelper.randomId())
      .map(eventId -> event.getUserId())
      .compose(userSummaryRepository::findByUserIdOrBuildNew)
      .compose(summary -> updateUserSummary(summary, event))
      .onComplete(result -> logResult(result, event));
  }

  private Future<String> updateUserSummary(UserSummary summary, LoanDueDateChangedEvent event) {
    List<OpenLoan> openLoans = summary.getOpenLoans();

    Optional<OpenLoan> loanMatch = openLoans.stream()
      .filter(loan -> StringUtils.equals(loan.getLoanId(), event.getLoanId()))
      .findFirst();

    if (loanMatch.isPresent()) {
      OpenLoan loan = loanMatch.get();
      loan.setDueDate(event.getDueDate());
      loan.setRecall(event.getDueDateChangedByRecall());
    } else {
      openLoans.add(new OpenLoan()
        .withLoanId(event.getLoanId())
        .withDueDate(event.getDueDate())
        .withRecall(event.getDueDateChangedByRecall()));
    }

    return userSummaryRepository.upsert(summary);
  }
}

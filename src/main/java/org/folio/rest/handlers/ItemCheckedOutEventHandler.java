package org.folio.rest.handlers;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.domain.EventType.ITEM_CHECKED_OUT;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.folio.domain.EventType;
import org.folio.repository.BaseRepository;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.UuidHelper;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ItemCheckedOutEventHandler extends EventHandler<ItemCheckedOutEvent> {
  private static final String EVENT_TABLE_NAME = EventType.ITEM_CHECKED_OUT.getTableName();

  private final BaseRepository<ItemCheckedOutEvent> eventRepository;

  public ItemCheckedOutEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
    eventRepository = new BaseRepository<>(getPostgresClient(okapiHeaders, vertx), EVENT_TABLE_NAME,
      ItemCheckedOutEvent.class);
  }

  public ItemCheckedOutEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
    eventRepository = new BaseRepository<>(postgresClient, EVENT_TABLE_NAME,
      ItemCheckedOutEvent.class);
  }

  @Override
  public Future<String> handle(ItemCheckedOutEvent event) {
    return eventRepository.save(event, UuidHelper.randomId())
      .compose(eventId -> userSummaryRepository.findByUserIdOrBuildNew(event.getUserId()))
      .compose(summary -> updateUserSummary(summary, event))
      .onComplete(result -> logResult(result, event));
  }

  private Future<String> updateUserSummary(UserSummary userSummary,
    ItemCheckedOutEvent event) {

    List<OpenLoan> openLoans = userSummary.getOpenLoans();

    if (openLoans.stream()
      .noneMatch(loan -> StringUtils.equals(loan.getLoanId(), event.getLoanId()))) {

      openLoans.add(new OpenLoan()
        .withLoanId(event.getLoanId())
        .withDueDate(event.getDueDate()));

      return userSummaryRepository.upsert(userSummary, userSummary.getId());
    }
    else {
      log.error("{} event is ignored, open loan ID {} already exists", ITEM_CHECKED_OUT.name(),
        event.getLoanId());

      return succeededFuture();
    }
  }
}

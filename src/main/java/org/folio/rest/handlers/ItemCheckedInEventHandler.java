package org.folio.rest.handlers;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.folio.domain.EventType;
import org.folio.repository.BaseRepository;
import org.folio.rest.jaxrs.model.ItemCheckedInEvent;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.UuidHelper;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ItemCheckedInEventHandler extends EventHandler<ItemCheckedInEvent> {
  private static final String EVENT_TABLE_NAME = EventType.ITEM_CHECKED_IN.getTableName();

  private final BaseRepository<ItemCheckedInEvent> eventRepository;

  public ItemCheckedInEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
    eventRepository = new BaseRepository<>(getPostgresClient(okapiHeaders, vertx), EVENT_TABLE_NAME,
      ItemCheckedInEvent.class);
  }

  public ItemCheckedInEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
    eventRepository = new BaseRepository<>(postgresClient, EVENT_TABLE_NAME,
      ItemCheckedInEvent.class);
  }

  @Override
  public Future<String> handle(ItemCheckedInEvent event) {
    return eventRepository.save(event, UuidHelper.randomId())
      .map(eventId -> event.getUserId())
      .compose(userSummaryRepository::getByUserId)
      .compose(optionalSummary -> optionalSummary
        .map(summary -> updateUserSummary(summary, event))
        .orElseGet(() -> {
          log.info("Summary for user {} was not found. Event is ignored.", event.getUserId());
          return succeededFuture();
        }))
      .onComplete(result -> logResult(result, event));
  }

  private Future<String> updateUserSummary(UserSummary userSummary, ItemCheckedInEvent event) {
    boolean loanRemoved = userSummary.getOpenLoans()
      .removeIf(loan -> StringUtils.equals(loan.getLoanId(), event.getLoanId()));

    if (!loanRemoved) {
      log.info("Open loan {} was not found in summary for user {}. Event is ignored.",
        event.getLoanId(), event.getUserId());
      return succeededFuture();
    }

    return userSummaryRepository.update(userSummary)
      .map(userSummary.getId());
  }
}

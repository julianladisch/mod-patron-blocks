package org.folio.rest.handlers;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.folio.domain.EventType;
import org.folio.repository.BaseRepository;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.UuidHelper;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ItemClaimedReturnedEventHandler extends EventHandler<ItemClaimedReturnedEvent> {
  private static final String EVENT_TABLE_NAME = EventType.ITEM_CLAIMED_RETURNED.getTableName();

  private final BaseRepository<ItemClaimedReturnedEvent> eventRepository;

  public ItemClaimedReturnedEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
    eventRepository = new BaseRepository<>(getPostgresClient(okapiHeaders, vertx), EVENT_TABLE_NAME,
      ItemClaimedReturnedEvent.class);
  }

  public ItemClaimedReturnedEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
    eventRepository = new BaseRepository<>(postgresClient, EVENT_TABLE_NAME,
      ItemClaimedReturnedEvent.class);
  }

  @Override
  public Future<String> handle(ItemClaimedReturnedEvent event) {
    return eventRepository.save(event, UuidHelper.randomId())
      .compose(eventId -> userSummaryRepository.findByUserIdOrBuildNew(event.getUserId()))
      .compose(summary -> updateUserSummary(summary, event))
      .onComplete(result -> logResult(result, event));
  }

  private Future<String> updateUserSummary(UserSummary userSummary, ItemClaimedReturnedEvent event) {
    List<OpenLoan> openLoans = userSummary.getOpenLoans();

    final OpenLoan openLoan = openLoans.stream()
      .filter(loan -> StringUtils.equals(loan.getLoanId(), event.getLoanId()))
      .findFirst()
      .orElseGet(() -> {
        OpenLoan newOpenLoan = new OpenLoan().withLoanId(event.getLoanId());
        openLoans.add(newOpenLoan);
        return newOpenLoan;
      });

    openLoan.setItemClaimedReturned(true);
    openLoan.setItemLost(false);

    return userSummaryRepository.upsert(userSummary, userSummary.getId());
  }
}

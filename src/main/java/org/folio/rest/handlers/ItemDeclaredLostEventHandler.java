package org.folio.rest.handlers;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.folio.domain.EventType;
import org.folio.repository.BaseRepository;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.UuidHelper;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ItemDeclaredLostEventHandler extends EventHandler<ItemDeclaredLostEvent> {
  private static final String EVENT_TABLE_NAME = EventType.ITEM_DECLARED_LOST.getTableName();

  private final BaseRepository<ItemDeclaredLostEvent> eventRepository;

  public ItemDeclaredLostEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
    eventRepository = new BaseRepository<>(getPostgresClient(okapiHeaders, vertx), EVENT_TABLE_NAME,
      ItemDeclaredLostEvent.class);
  }

  public ItemDeclaredLostEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
    eventRepository = new BaseRepository<>(postgresClient, EVENT_TABLE_NAME,
      ItemDeclaredLostEvent.class);
  }

  @Override
  public Future<String> handle(ItemDeclaredLostEvent event) {
    return eventRepository.save(event, UuidHelper.randomId())
      .compose(eventId -> userSummaryRepository.findByUserIdOrBuildNew(event.getUserId()))
      .compose(summary -> updateUserSummary(summary, event))
      .onComplete(result -> logResult(result, event));
  }

  private Future<String> updateUserSummary(UserSummary userSummary, ItemDeclaredLostEvent event) {
    List<OpenLoan> openLoans = userSummary.getOpenLoans();

    final OpenLoan openLoan = openLoans.stream()
      .filter(loan -> StringUtils.equals(loan.getLoanId(), event.getLoanId()))
      .findAny()
      .orElseGet(() -> {
        OpenLoan newOpenLoan = new OpenLoan().withLoanId(event.getLoanId());
        openLoans.add(newOpenLoan);
        return newOpenLoan;
      });

    openLoan.setItemLost(true);
    openLoan.setItemClaimedReturned(false);

    return userSummaryRepository.upsert(userSummary, userSummary.getId());
  }
}

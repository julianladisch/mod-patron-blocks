package org.folio.rest.handlers;

import static java.lang.String.format;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.folio.domain.EventType;
import org.folio.domain.FeeFineType;
import org.folio.exception.EntityNotFoundException;
import org.folio.repository.BaseRepository;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.OpenFeeFine;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.UuidHelper;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class FeeFineBalanceChangedEventHandler extends EventHandler<FeeFineBalanceChangedEvent> {
  private static final String EVENT_TABLE_NAME = EventType.FEE_FINE_BALANCE_CHANGED.getTableName();
  private static final List<String> LOST_ITEM_FEE_TYPE_IDS = Arrays.asList(
    FeeFineType.LOST_ITEM_FEE.getId(),
    FeeFineType.LOST_ITEM_PROCESSING_FEE.getId()
  );

  private final BaseRepository<FeeFineBalanceChangedEvent> eventRepository;

  public FeeFineBalanceChangedEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
    eventRepository = new BaseRepository<>(getPostgresClient(okapiHeaders, vertx), EVENT_TABLE_NAME,
      FeeFineBalanceChangedEvent.class);
  }

  public FeeFineBalanceChangedEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
    eventRepository = new BaseRepository<>(postgresClient, EVENT_TABLE_NAME,
      FeeFineBalanceChangedEvent.class);
  }

  @Override
  public Future<String> handle(FeeFineBalanceChangedEvent event) {
    return eventRepository.save(event, UuidHelper.randomId())
      .compose(eventId -> getUserSummary(event))
      .compose(summary -> updateUserSummary(summary, event))
      .onComplete(result -> logResult(result, event));
  }

  private Future<String> updateUserSummary(UserSummary userSummary,
    FeeFineBalanceChangedEvent event) {

    List<OpenFeeFine> openFeesFines = userSummary.getOpenFeesFines();

    OpenFeeFine openFeeFine = openFeesFines.stream()
      .filter(feeFine -> StringUtils.equals(feeFine.getFeeFineId(), event.getFeeFineId()))
      .findFirst()
      .orElseGet(() -> {
        OpenFeeFine newFeeFine = new OpenFeeFine()
          .withFeeFineId( event.getFeeFineId())
          .withFeeFineTypeId(event.getFeeFineTypeId())
          .withBalance(event.getBalance());
        openFeesFines.add(newFeeFine);
        return newFeeFine;
      });

    if (feeFineIsClosed(event)) {
      openFeesFines.remove(openFeeFine);
      removeLoanIfLastLostItemFeeWasClosed(userSummary, event);
    } else {
      openFeeFine.setBalance(event.getBalance());
      openFeeFine.setLoanId(event.getLoanId());
    }

    return userSummaryRepository.upsert(userSummary, userSummary.getId());
  }

  private boolean feeFineIsClosed(FeeFineBalanceChangedEvent event) {
    return BigDecimal.ZERO.compareTo(event.getBalance()) == 0;
  }

  private Future<UserSummary> getUserSummary(FeeFineBalanceChangedEvent event) {
    return event.getUserId() != null
      ? userSummaryRepository.findByUserIdOrBuildNew(event.getUserId())
      : findSummaryByFeeFineIdOrFail(event.getFeeFineId());
  }

  private Future<UserSummary> findSummaryByFeeFineIdOrFail(String feeFineId) {
    return userSummaryRepository.findByFeeFineId(feeFineId)
      .map(summary -> summary.orElseThrow(() -> new EntityNotFoundException(
        format("User summary with fee/fine %s was not found, event is ignored", feeFineId))));
  }

  private void removeLoanIfLastLostItemFeeWasClosed(UserSummary userSummary,
    FeeFineBalanceChangedEvent event) {

    if (!isLostItemFeeId(event.getFeeFineTypeId())) {
      return;
    }

    userSummary.getOpenLoans().stream()
      .filter(OpenLoan::getItemLost)
      .filter(loan -> StringUtils.equals(loan.getLoanId(), event.getLoanId()))
      .findAny()
      .ifPresent(loan -> {
        boolean noLostItemFeesForLoanExist = userSummary.getOpenFeesFines().stream()
          .filter(fee -> StringUtils.equals(fee.getLoanId(), event.getLoanId()))
          .map(OpenFeeFine::getFeeFineTypeId)
          .noneMatch(this::isLostItemFeeId);

        if (noLostItemFeesForLoanExist) {
          userSummary.getOpenLoans().remove(loan);
        }
      });
  }

  private boolean isLostItemFeeId(String feeFineTypeId) {
    return LOST_ITEM_FEE_TYPE_IDS.contains(feeFineTypeId);
  }

}

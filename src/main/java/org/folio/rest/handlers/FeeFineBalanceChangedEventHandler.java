package org.folio.rest.handlers;

import static java.lang.String.format;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

import org.apache.commons.lang3.StringUtils;
import org.folio.exception.EntityNotFoundException;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.OpenFeeFine;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;

public class FeeFineBalanceChangedEventHandler extends EventHandler<FeeFineBalanceChangedEvent> {

  public FeeFineBalanceChangedEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
  }

  public FeeFineBalanceChangedEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
  }

  @Override
  public Future<String> handle(FeeFineBalanceChangedEvent event) {
    return getUserSummary(event)
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
    } else {
      openFeeFine.setBalance(event.getBalance());
    }

    refreshOutstandingFeeFineBalance(userSummary);

    return userSummaryRepository.upsert(userSummary, userSummary.getId());
  }

  private boolean feeFineIsClosed(FeeFineBalanceChangedEvent event) {
    return BigDecimal.ZERO.compareTo(event.getBalance()) == 0;
  }

  private void refreshOutstandingFeeFineBalance(UserSummary userSummary) {
    userSummary.setOutstandingFeeFineBalance(
      userSummary.getOpenFeesFines().stream()
      .map(OpenFeeFine::getBalance)
      .reduce(BigDecimal.ZERO, BigDecimal::add)
    );
  }

  private Future<UserSummary> getUserSummary(FeeFineBalanceChangedEvent event) {
    return event.getUserId() != null
      ? userSummaryRepository.findByUserIdOrBuildNew(event.getUserId())
      : findSummaryByFeeFineIdOrFail(event.getFeeFineId());
  }

  private Future<UserSummary> findSummaryByFeeFineIdOrFail(String feeFineId) {
    return userSummaryRepository.findByFeeFineId(feeFineId)
      .map(summary -> summary.orElseThrow(() -> new EntityNotFoundException(
        format("User summary with feeFine %s was not found, event is ignored", feeFineId))));
  }

}

package org.folio.rest.handlers;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ItemClaimedReturnedEventHandler extends EventHandler<ItemClaimedReturnedEvent> {

  public ItemClaimedReturnedEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
  }

  public ItemClaimedReturnedEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
  }

  @Override
  public Future<String> handle(ItemClaimedReturnedEvent event) {
    return userSummaryRepository.findByUserIdOrBuildNew(event.getUserId())
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

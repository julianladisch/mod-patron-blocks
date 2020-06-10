package org.folio.rest.handlers;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.domain.EventType.ITEM_CHECKED_OUT;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ItemCheckedOutEventHandler extends EventHandler<ItemCheckedOutEvent> {

  public ItemCheckedOutEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
  }

  public ItemCheckedOutEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
  }

  @Override
  public Future<String> handle(ItemCheckedOutEvent event) {
    return userSummaryRepository.findByUserIdOrBuildNew(event.getUserId())
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
        .withDueDate(event.getDueDate())
        .withRecall(false));

      return userSummaryRepository.upsert(userSummary, userSummary.getId());
    }
    else {
      log.error("{} event is ignored, open loan ID {} already exists", ITEM_CHECKED_OUT.name(),
        event.getLoanId());

      return succeededFuture();
    }
  }
}

package org.folio.rest.handlers;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static org.folio.domain.EventType.FEE_FINE_BALANCE_CHANGED;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.apache.commons.lang3.StringUtils;
import org.folio.domain.OpenFeeFine;
import org.folio.domain.UserSummary;
import org.folio.exception.EntityNotFoundException;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

public class FeeFineBalanceChangedEventHandler implements EventHandler<FeeFineBalanceChangedEvent> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";

  private final UserSummaryRepository userSummaryRepository;

  public FeeFineBalanceChangedEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
    userSummaryRepository = new UserSummaryRepository(
      PostgresClient.getInstance(vertx, tenantId));
  }

  public FeeFineBalanceChangedEventHandler(PostgresClient postgresClient) {
    userSummaryRepository = new UserSummaryRepository(postgresClient);
  }

  public Future<String> handle(FeeFineBalanceChangedEvent event) {
    return succeededFuture(event)
      .compose(this::updateUserSummary)
      .onComplete(this::logResult);
  }

  private Future<String> updateUserSummary(FeeFineBalanceChangedEvent event) {
    return getUserSummary(event)
      .compose(summary -> updateUserSummary(summary, event));
  }

  private Future<String> updateUserSummary(UserSummary userSummary,
    FeeFineBalanceChangedEvent event) {

    List<OpenFeeFine> openFeeFines = userSummary.getOpenFeeFines();

    OpenFeeFine openFeeFine = openFeeFines.stream()
      .filter(feeFine -> StringUtils.equals(feeFine.getFeeFineId(), event.getFeeFineId()))
      .findFirst()
      .orElseGet(() -> {
        OpenFeeFine newFeeFine = new OpenFeeFine()
          .withFeeFineId( event.getFeeFineId())
          .withFeeFineTypeId(event.getFeeFineTypeId())
          .withBalance(event.getBalance());
        openFeeFines.add(newFeeFine);
        return newFeeFine;
      });

    if (feeFineIsClosed(event)) {
      openFeeFines.remove(openFeeFine);
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
      userSummary.getOpenFeeFines().stream()
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

  protected void logResult(AsyncResult<String> result) {
    String eventType = FEE_FINE_BALANCE_CHANGED.name();

    if (result.failed()) {
      log.error("Failed to process event {}", result.cause(), eventType);
    } else {
      log.info("Event {} processed successfully. Affected user summary: {}",
        eventType, result.result());
    }
  }
}

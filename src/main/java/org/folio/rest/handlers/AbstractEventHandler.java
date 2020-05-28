package org.folio.rest.handlers;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.folio.domain.EventType;
import org.folio.domain.UserSummary;
import org.folio.repository.UserSummaryRepository;
import org.folio.repository.UserSummaryRepositoryImpl;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractEventHandler {
  protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
  protected final UserSummaryRepository userSummaryRepository;

  public abstract Future<String> handle(String payload);

  public AbstractEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
    userSummaryRepository = new UserSummaryRepositoryImpl(
      PostgresClient.getInstance(vertx, tenantId));
  }

  public AbstractEventHandler(PostgresClient postgresClient) {
    userSummaryRepository = new UserSummaryRepositoryImpl(postgresClient);
  }

  protected Future<UserSummary> getSummaryForUser(String userId) {
    return userSummaryRepository.getUserSummaryByUserId(userId)
      .map(summary -> summary.orElseGet(() -> buildEmptyUserSummary(userId)));
  }

  protected UserSummary buildEmptyUserSummary(String userId) {
    return new UserSummary()
      .withId(UUID.randomUUID().toString())
      .withUserId(userId)
      .withOutstandingFeeFineBalance(BigDecimal.ZERO)
      .withNumberOfLostItems(0);
  }

  protected static void logSuccess(EventType eventType, String userSummaryId) {
    log.info("Event {} processed successfully. Affected user summary: {}",
      eventType.name(), userSummaryId);
  }

  protected static void logError(EventType eventType, String payload, Throwable throwable) {
    log.error("Failed to process event {}. Event payload:\n\"{}\"",
      throwable, eventType.name(), payload);
  }
}

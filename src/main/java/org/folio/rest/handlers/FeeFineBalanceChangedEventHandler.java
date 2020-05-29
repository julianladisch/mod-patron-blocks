package org.folio.rest.handlers;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.domain.EventType.FEE_FINE_BALANCE_CHANGED;
import static org.folio.util.UuidHelper.validateUUID;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.folio.domain.OpenFeeFine;
import org.folio.domain.UserSummary;
import org.folio.rest.persist.PostgresClient;

public class FeeFineBalanceChangedEventHandler extends AbstractEventHandler {
  private static final String FIND_SUMMARY_BY_FEE_FINE_ID_QUERY_TEMPLATE =
    "openFeesFines == \"*\\\"feeFineId\\\": \\\"%s\\\"*\"";

  public FeeFineBalanceChangedEventHandler(Map<String, String> okapiHeaders, Vertx vertx) {
    super(okapiHeaders, vertx);
  }

  public FeeFineBalanceChangedEventHandler(PostgresClient postgresClient) {
    super(postgresClient);
  }

  public Future<String> handle(String payload) {
    return succeededFuture(payload)
      .map(Payload::from)
      .compose(this::updateUserSummary)
      .onComplete(this::logResult);
  }

  private Future<String> updateUserSummary(Payload payload) {
    return getUserSummary(payload)
      .compose(summary -> updateUserSummary(summary, payload));
  }

  private Future<String> updateUserSummary(UserSummary userSummary, Payload payload) {
    List<OpenFeeFine> openFeeFines = userSummary.getOpenFeeFines();

    OpenFeeFine openFeeFine = openFeeFines.stream()
      .filter(feeFine -> StringUtils.equals(feeFine.getFeeFineId(), payload.feeFineId))
      .findFirst()
      .orElseGet(() -> {
        OpenFeeFine newFeeFine = new OpenFeeFine()
          .withFeeFineId(payload.feeFineId)
          .withFeeFineTypeId(payload.feeFineTypeId)
          .withBalance(payload.balance);
        openFeeFines.add(newFeeFine);
        return newFeeFine;
      });

    if (feeFineIsClosed(payload)) {
      openFeeFines.remove(openFeeFine);
    } else {
      openFeeFine.setBalance(payload.balance);
    }

    refreshOutstandingFeeFineBalance(userSummary);

    return userSummaryRepository.upsertUserSummary(userSummary);
  }

  private boolean feeFineIsClosed(Payload payload) {
    return BigDecimal.ZERO.compareTo(payload.balance) == 0;
  }

  private void refreshOutstandingFeeFineBalance(UserSummary userSummary) {
    userSummary.setOutstandingFeeFineBalance(
      userSummary.getOpenFeeFines().stream()
      .map(OpenFeeFine::getBalance)
      .reduce(BigDecimal.ZERO, BigDecimal::add)
    );
  }

  private Future<UserSummary> getUserSummary(Payload payload) {
    return payload.userId != null
      ? getSummaryForUser(payload.userId)
      : getSummaryByFeeFineId(payload);
  }

  private Future<UserSummary> getSummaryByFeeFineId(Payload payload) {
    String query = String.format(FIND_SUMMARY_BY_FEE_FINE_ID_QUERY_TEMPLATE, payload.feeFineId);

    return userSummaryRepository.getUserSummaries(query, 0, 1)
      .map(result -> result.stream()
        .findFirst()
        .orElseGet(() -> buildEmptyUserSummary(payload.userId)));
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

  private static class Payload {
    private final String userId;
    private final String feeFineId;
    private final String feeFineTypeId;
    private final BigDecimal balance;

    private Payload(String userId, String feeFineId, String feeFineTypeId, BigDecimal balance) {
      this.userId = userId;
      this.feeFineId = feeFineId;
      this.feeFineTypeId = feeFineTypeId;
      this.balance = balance;
    }

    private static Payload from(String payload) {
      final JsonObject payloadJson = new JsonObject(payload);

      final String userId = payloadJson.getString("userId");
      final String feeFineId = payloadJson.getString("feeFineId");
      final String feeFineTypeId = payloadJson.getString("feeFineTypeId");
      final String balanceString = String.valueOf(payloadJson.getValue("balance"));
      final BigDecimal balance;

      try {
        balance = new BigDecimal(balanceString);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
          "Invalid fee/fine balance value in event payload: " + balanceString, e);
      }

      validateUUID(feeFineId, true);
      validateUUID(feeFineTypeId, false);
      validateUUID(userId, false);

      return new Payload(userId, feeFineId, feeFineTypeId, balance);
    }

  }
}

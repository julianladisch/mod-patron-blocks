package org.folio.rest.handlers;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.domain.EventType.FEE_FINE_BALANCE_CHANGED;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import org.folio.domain.OpenFeeFine;
import org.folio.domain.UserSummary;
import org.folio.rest.persist.PostgresClient;

public class FeeFineBalanceChangedEventHandler extends AbstractEventHandler {

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
      .onSuccess(userSummaryId -> logSuccess(FEE_FINE_BALANCE_CHANGED, userSummaryId))
      .onFailure(throwable -> logError(FEE_FINE_BALANCE_CHANGED, payload, throwable));
  }

  private Future<String> updateUserSummary(Payload payload) {
    return getSummaryForUser(payload.userId)
      .compose(summary -> updateUserSummary(summary, payload));
  }

  private Future<String> updateUserSummary(UserSummary userSummary, Payload payload) {
    List<OpenFeeFine> openFeeFines = userSummary.getOpenFeeFines();

    OpenFeeFine openFeeFine = openFeeFines.stream()
      .filter(feeFine -> feeFine.getFeeFineId().equals(payload.feeFineId))
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

      UUID.fromString(userId); // to make sure that userId value is a proper UUID

      return new Payload(userId, feeFineId, feeFineTypeId, balance);
    }

  }
}

package org.folio.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.PatronBlockLimit;
import org.folio.rest.jaxrs.model.UserSummary;

public enum Condition {
  // IDs come from resources\templates\db_scripts\populate-patron-block-conditions.sql
  MAX_NUMBER_OF_ITEMS_CHARGED_OUT("3d7c52dc-c732-4223-8bf8-e5917801386f",
    (ctx) -> ctx.getUserSummary().getOpenLoans().size() >= ctx.getLimit().getValue()
  ),

  MAX_NUMBER_OF_LOST_ITEMS("72b67965-5b73-4840-bc0b-be8f3f6e047e",
    (ctx) -> ctx.getUserSummary().getNumberOfLostItems() >= ctx.getLimit().getValue()
  ),

  MAX_NUMBER_OF_OVERDUE_ITEMS("584fbd4f-6a34-4730-a6ca-73a6a6a9d845",
    (ctx) -> ctx.getUserSummary().getOpenLoans().stream()
      .filter(openLoan -> ctx.getOverdueMinutes().get(openLoan.getLoanId()) > 0)
      .count() >= ctx.getLimit().getValue()
  ),

  MAX_NUMBER_OF_OVERDUE_RECALLS("e5b45031-a202-4abb-917b-e1df9346fe2c",
    (ctx) -> ctx.getUserSummary().getOpenLoans().stream()
      .filter(openLoan -> ctx.getOverdueMinutes().get(openLoan.getLoanId()) > 0)
      .filter(OpenLoan::getRecall)
      .count() >= ctx.getLimit().getValue()
  ),

  RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS("08530ac4-07f2-48e6-9dda-a97bc2bf7053",
    (ctx) -> ctx.getUserSummary().getOpenLoans().stream()
      .filter(openLoan -> ctx.getOverdueMinutes().get(openLoan.getLoanId()) > 0)
      .filter(OpenLoan::getRecall)
      .map(openLoan -> getLoanOverdueDays(ctx.getOverdueMinutes().get(openLoan.getLoanId())))
      .anyMatch(days -> days >= ctx.getLimit().getValue())
  ),

  MAX_OUTSTANDING_FEE_FINE_BALANCE("cf7a0d5f-a327-4ca1-aa9e-dc55ec006b8a",
    (ctx) -> ctx.getUserSummary().getOutstandingFeeFineBalance()
      .compareTo(BigDecimal.valueOf(ctx.getLimit().getValue())) >= 0
  );

  private final String id;
  private final Predicate<LimitEvaluationContext> limitEvaluator;
  private static final Map<String, Condition> idIndex = new HashMap<>(Condition.values().length);
  private static final Double NUMBER_OF_MINUTES_IN_ONE_DAY = 1440.0;

  static {
    for (Condition condition : Condition.values()) {
      idIndex.put(condition.id, condition);
    }
  }

  Condition(String id, Predicate<LimitEvaluationContext> limitEvaluator) {
    this.id = id;
    this.limitEvaluator = limitEvaluator;
  }

  public String getId() {
    return id;
  }

  public static Condition getById(String id) {
    return idIndex.get(id);
  }

  public static boolean isConditionLimitExceeded(LimitEvaluationContext ctx) {
    return idIndex.get(ctx.getLimit().getConditionId()).limitEvaluator.test(ctx);
  }

  private static int getLoanOverdueDays(Integer overdueMinutes) {
    return (int) Math.ceil(overdueMinutes.doubleValue() / NUMBER_OF_MINUTES_IN_ONE_DAY);
  }

  public static class LimitEvaluationContext {
    private final UserSummary userSummary;
    private final PatronBlockLimit limit;
    private final Map<String, Integer> overdueMinutes;

    public LimitEvaluationContext(UserSummary userSummary, PatronBlockLimit limit,
      Map<String, Integer> overdueMinutes) {

      this.userSummary = userSummary;
      this.limit = limit;
      this.overdueMinutes = overdueMinutes;
    }

    public UserSummary getUserSummary() {
      return userSummary;
    }

    public PatronBlockLimit getLimit() {
      return limit;
    }

    public Map<String, Integer> getOverdueMinutes() {
      return overdueMinutes;
    }
  }
}

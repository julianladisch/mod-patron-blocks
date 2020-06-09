package org.folio.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;

public class UserSummaryRepository extends BaseRepository<UserSummary> {
  public static final String USER_SUMMARY_TABLE_NAME = "user_summary";
  private static final String USER_ID_FIELD = "'userId'";
  private static final String OPERATION_EQUALS = "=";
  private static final String FIND_SUMMARY_BY_FEE_FINE_ID_QUERY_TEMPLATE =
    "openFeesFines == \"*\\\"feeFineId\\\": \\\"%s\\\"*\"";

  public UserSummaryRepository(PostgresClient pgClient) {
    super(pgClient, USER_SUMMARY_TABLE_NAME, UserSummary.class);
  }

  public Future<String> upsert(UserSummary entity) {
    return super.upsert(entity, entity.getId());
  }

  public Future<String> save(UserSummary entity) {
    return super.save(entity, entity.getId());
  }

  public Future<Boolean> update(UserSummary entity) {
    return super.update(entity, entity.getId());
  }

  public Future<Optional<UserSummary>> getByUserId(String userId) {
    Criterion criterion = new Criterion(new Criteria()
        .addField(USER_ID_FIELD)
        .setOperation(OPERATION_EQUALS)
        .setVal(userId)
        .setJSONB(true));

    return this.get(criterion)
      .map(results -> results.stream().findFirst());
  }

  public Future<UserSummary> findByUserIdOrBuildNew(String userId) {
    return getByUserId(userId)
      .map(summary -> summary.orElseGet(() -> buildEmptyUserSummary(userId)));
  }

  public Future<Optional<UserSummary>> findByFeeFineId(String feeFineId) {
    String query = String.format(FIND_SUMMARY_BY_FEE_FINE_ID_QUERY_TEMPLATE, feeFineId);

    return get(query, 0, 1)
      .map(result -> result.stream().findFirst());
  }

  private UserSummary buildEmptyUserSummary(String userId) {
    return new UserSummary()
      .withId(UUID.randomUUID().toString())
      .withUserId(userId)
      .withOutstandingFeeFineBalance(BigDecimal.ZERO)
      .withNumberOfLostItems(0);
  }

}

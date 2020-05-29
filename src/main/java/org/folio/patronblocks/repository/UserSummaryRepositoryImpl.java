package org.folio.patronblocks.repository;

import java.util.List;
import java.util.Optional;

import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.patronblocks.model.UserSummary;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.sql.UpdateResult;

public class UserSummaryRepositoryImpl implements UserSummaryRepository {

  private static final String USER_SUMMARY_TABLE = "user_summary";
  private PostgresClient pgClient;

  public UserSummaryRepositoryImpl(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  @Override
  public Future<List<UserSummary>> getUserSummaries(String query, int offset, int limit) {

    Promise<Results<UserSummary>> promise = Promise.promise();
    try {
      String[] fieldList = {"*"};
      CQLWrapper cql = getCQL(query, limit, offset);
      pgClient.get(USER_SUMMARY_TABLE, UserSummary.class, fieldList, cql, true, false, promise);
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future().map(Results::getResults);
  }

  @Override
  public Future<Optional<UserSummary>> getUserSummaryById(String id) {
    Promise<UserSummary> promise = Promise.promise();
    pgClient.getById(USER_SUMMARY_TABLE, id, UserSummary.class, promise);
    return promise.future().map(Optional::ofNullable);
  }

  @Override
  public Future<String> saveUserSummary(UserSummary userSummary) {
    Promise<String> promise = Promise.promise();
    pgClient.save(USER_SUMMARY_TABLE, userSummary.getId(), userSummary, promise);
    return promise.future();
  }

  @Override
  public Future<Boolean> updateUserSummary(UserSummary userSummary) {
    Promise<UpdateResult> promise = Promise.promise();
    pgClient.update(USER_SUMMARY_TABLE, userSummary, userSummary.getId(), promise);
    return promise.future().map(updateResult -> updateResult.getUpdated() == 1);
  }

  @Override
  public Future<Boolean> deleteUserSummary(String id) {
    Promise<UpdateResult> promise = Promise.promise();
    pgClient.delete(USER_SUMMARY_TABLE, id, promise);
    return promise.future().map(updateResult -> updateResult.getUpdated() == 1);
  }

  /**
   * Build CQL from request URL query
   *
   * @param query - query from URL
   * @param limit - limit of records for pagination
   * @return - CQL wrapper for building postgres request to database
   * @throws org.folio.cql2pgjson.exception.FieldException field exception
   */
  private CQLWrapper getCQL(String query, int limit, int offset)
    throws FieldException {
    CQL2PgJSON cql2pgJson = new CQL2PgJSON(USER_SUMMARY_TABLE + ".jsonb");
    return new CQLWrapper(cql2pgJson, query)
      .setLimit(new Limit(limit))
      .setOffset(new Offset(offset));
  }
}

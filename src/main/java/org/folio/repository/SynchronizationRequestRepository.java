package org.folio.repository;

import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;

import java.util.List;

import org.folio.domain.SynchronizationStatus;
import org.folio.rest.jaxrs.model.SynchronizationJob;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class SynchronizationRequestRepository extends BaseRepository<SynchronizationJob> {

  private static final String SYNC_REQUESTS_TABLE = "sync_requests";
  private static final int SYNC_REQUESTS_LIMIT = 1;

  public SynchronizationRequestRepository(PostgresClient pgClient) {
    super(pgClient, SYNC_REQUESTS_TABLE, SynchronizationJob.class);
  }

  public Future<String> save(SynchronizationJob entity) {
    return save(entity, entity.getId());
  }
//
//  public Future<List<SynchronizationJob>> checkIfSynchronizationIsAllowed() {
//
//    Criterion criterion = new Criterion(new Criteria()
//      .addField("'status'")
//      .setOperation("=")
//      .setVal("in-progress")
//      .setJSONB(true));
//
//    return get(criterion)
//      .map(requestList -> {
//        if (!requestList.isEmpty()) {
//          throw new RuntimeException("There is a synchJob in progress now");
//        }
//        return requestList;
//      });
//  }

  public Future<List<SynchronizationJob>> getJobsByStatus(
    SynchronizationStatus syncStatus) {

    Criterion criterion = new Criterion(new Criteria()
      .addField("'status'")
      .setOperation("=")
      .setVal(syncStatus.getValue())
      .setJSONB(true));

    return get(criterion);
  }

  public Future<SynchronizationJob> getTheOldestSyncRequest(String tenantId) {
    String tableName = String.format("%s.%s", convertToPsqlStandard(tenantId),
      SYNC_REQUESTS_TABLE);

    String sql = String.format("SELECT jsonb FROM %s WHERE jsonb->>'status' = 'open' " +
      "ORDER BY(jsonb #>> '{metadata,createdDate}') ASC LIMIT '%d'", tableName,
      SYNC_REQUESTS_LIMIT);

    return select(sql)
      .map(requests -> {
        if (requests.size() == 0) {
          throw new RuntimeException("There are no open requests");
        }
        return requests.iterator().next();
      })
      .map(row -> row.getValue(0))
      .map(JsonObject.class::cast)
      .map(this::mapSynchronizationJob);
  }

  public Future<RowSet<Row>> select(String sql) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient.select(sql, promise);
    return promise.future();
  }

  private SynchronizationJob mapSynchronizationJob(JsonObject json) {
    SynchronizationJob synchronizationJob = new SynchronizationJob();
    synchronizationJob
      .withId(json.getString("id"))
      .withScope(json.getString("scope"))
      .withStatus(json.getString("status"))
      .withTotalNumberOfLoans(json.getInteger("totalNumberOfLoans"))
      .withTotalNumberOfFeesFines(json.getInteger("totalNumberOfFeesFines"))
      .withNumberOfProcessedLoans(json.getInteger("numberOfProcessedLoans"))
      .withNumberOfProcessedFeesFines(json.getInteger("numberOfProcessedFeesFines"));

    String userId = json.getString("userId");
    if (userId != null && !userId.isBlank()) {
      synchronizationJob.withUserId(userId);
    }

    return synchronizationJob;
  }
}

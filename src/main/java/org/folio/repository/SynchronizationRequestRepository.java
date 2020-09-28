package org.folio.repository;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;

import java.util.List;

import org.folio.domain.SynchronizationStatus;
import org.folio.rest.jaxrs.model.SynchronizationJob;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
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

  public Future<List<SynchronizationJob>> checkIfSynchronizationIsAllowed() {

    Criterion criterion = new Criterion(new Criteria()
      .addField("'status'")
      .setOperation("=")
      .setVal("in-progress")
      .setJSONB(true));

    return get(criterion)
      .map(requestList -> {
        if (!requestList.isEmpty()) {
          throw new RuntimeException("");
        }
        return requestList;
      });
  }

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

    String sql = String.format("SELECT ALL FROM %s ORDER BY max(" +
      "jsonb #>> '{metadata,createdDate}') ASC", tableName, SYNC_REQUESTS_LIMIT);

    return select(sql)
      .map(requests -> {
        if (requests.size() == 0) {
          throw new RuntimeException("");
        }
        return requests.iterator().next();
      })
    .map(row -> mapSynchronizationJob(row));
  }

  public Future<RowSet<Row>> select(String sql) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient.select(sql, promise);
    return promise.future();
  }

  private SynchronizationJob mapSynchronizationJob(Row row) {
    SynchronizationJob synchronizationJob = new SynchronizationJob();
    synchronizationJob
      .withId(row.getString("id"))
      .withScope(row.getString("scope"))
      .withStatus(row.getString("status"))
      .withTotalNumberOfLoans(row.getDouble("totalNumberOfLoans"))
      .withTotalNumberOfFeesFines(row.getDouble("totalNumberOfFeesFines"))
      .withNumberOfProcessedLoans(row.getDouble("numberOfProcessedLoans"))
      .withNumberOfProcessedFeesFines(row.getDouble("numberOfProcessedFeesFines"));

    String userId = row.getString("userId");
    if (userId != null && !userId.isBlank()) {
      synchronizationJob.withUserId(userId);
    }

    return synchronizationJob;
  }
}

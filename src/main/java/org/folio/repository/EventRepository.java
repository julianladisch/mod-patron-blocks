package org.folio.repository;

import static io.vertx.core.Future.succeededFuture;

import java.util.List;
import java.util.Optional;

import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;

public class EventRepository<T> extends BaseRepository<T> {
  private static final int NUMBER_OF_EVENTS_LIMIT = 10000;

  public EventRepository(PostgresClient pgClient, String tableName, Class<T> entityType) {
    super(pgClient, tableName, entityType);
  }

  public Future<List<T>> getByUserId(String userId) {
    return this.get(new Criterion(new Criteria()
      .addField("'userId'")
      .setOperation("=")
      .setVal(userId)
      .setJSONB(true)
    ).setLimit(new Limit(NUMBER_OF_EVENTS_LIMIT)));
  }
}

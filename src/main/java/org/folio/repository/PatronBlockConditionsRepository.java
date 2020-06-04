package org.folio.repository;

import org.folio.rest.jaxrs.model.PatronBlockCondition;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;

public class PatronBlockConditionsRepository extends BaseRepository<PatronBlockCondition> {
  public static final String PATRON_BLOCK_CONDITIONS_TABLE_NAME = "patron_block_conditions";

  public PatronBlockConditionsRepository(PostgresClient pgClient) {
    super(pgClient, PATRON_BLOCK_CONDITIONS_TABLE_NAME, PatronBlockCondition.class);
  }

  public Future<Boolean> update(PatronBlockCondition condition) {
    return super.update(condition, condition.getId());
  }
}

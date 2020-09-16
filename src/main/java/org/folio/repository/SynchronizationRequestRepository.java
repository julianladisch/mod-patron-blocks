package org.folio.repository;

import org.folio.rest.jaxrs.model.SynchronizationResponse;
import org.folio.rest.persist.PostgresClient;

public class SynchronizationRequestRepository extends BaseRepository<SynchronizationResponse> {

  private static final String SYNC_REQUESTS = "sync-requests";

  public SynchronizationRequestRepository(PostgresClient pgClient) {
    super(pgClient, SYNC_REQUESTS, SynchronizationResponse.class);
  }
}

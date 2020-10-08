package org.folio.service;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.folio.repository.SynchronizationRequestRepository;
import org.folio.rest.client.OkapiClient;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.SynchronizationJob;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class EventsGenerationService {

  protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected final SynchronizationRequestRepository syncRepository;
  protected final OkapiClient okapiClient;
  protected final Vertx vertx;

  protected EventsGenerationService(Map<String, String> okapiHeaders, Vertx vertx,
    SynchronizationRequestRepository syncRepository) {

    this.vertx = vertx;
    this.okapiClient = new OkapiClient(vertx, okapiHeaders);
    this.syncRepository = syncRepository;
  }

  public abstract Future<SynchronizationJob> generateEvents(SynchronizationJob syncJob,
    String path);

  protected Metadata mapMetadataFromJson(JsonObject jsonMetadata) {
    return new Metadata()
      .withCreatedDate(parseDateFromJson(jsonMetadata, "createdDate"))
      .withUpdatedDate(parseDateFromJson(jsonMetadata, "updatedDate"));
  }

  protected Date parseDateFromJson(JsonObject representation, String fieldName) {
    Date date = null;
    try {
      String dueDate = representation.getString(fieldName);
      date = DateUtils.parseDate(dueDate, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    } catch (Exception e) {
      log.error("Date parsing error for field: " + fieldName);
    }
    return date;
  }

  protected void updateSyncJobWithError(SynchronizationJob syncJob, String localizedMessage) {
    List<String> errors = syncJob.getErrors();
    errors.add(localizedMessage);
    syncRepository.update(syncJob.withErrors(errors), syncJob.getId());
  }
}

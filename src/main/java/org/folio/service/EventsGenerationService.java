package org.folio.service;

import static org.folio.domain.SynchronizationStatus.DONE;

import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.folio.domain.SynchronizationStatus;
import org.folio.repository.SynchronizationRequestRepository;
import org.folio.rest.client.OkapiClient;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.SynchronizationJob;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class EventsGenerationService {

  protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static final int PAGE_LIMIT = 100;
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
    if (representation == null || representation.getString(fieldName) == null) {
      return null;
    }
    String dueDate = representation.getString(fieldName);
    Date date = null;
    try {
      date = DateUtils.parseDate(dueDate, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    } catch (ParseException e) {
      log.error("Date parsing error for field: " + fieldName);
    }
    return date;
  }

  protected void updateSyncJobWithError(SynchronizationJob syncJob, String localizedMessage) {
    List<String> errors = syncJob.getErrors();
    errors.add(localizedMessage);
    syncRepository.update(syncJob.withErrors(errors), syncJob.getId());
  }

  protected int calculateNumberOfPages(int totalRecords) {
    return (int) Math.ceil((totalRecords / (double) PAGE_LIMIT));
  }

  protected void updateJobWhenGenerationsCompleted(SynchronizationJob syncJob,
    List<Future> generatedEventsForPages) {

    CompositeFuture.all(generatedEventsForPages)
      .onComplete(result -> {
        if (result.succeeded()) {
          updateStatusOfJob(syncJob, DONE);
        } else {
          updateSyncJobWithError(syncJob, result.cause().getLocalizedMessage());
        }
      });
  }

  public SynchronizationJob updateStatusOfJob(SynchronizationJob syncJob,
    SynchronizationStatus syncStatus) {

    syncJob.setStatus(syncStatus.getValue());
    log.debug("Status of synchronization job has been updated: " + syncStatus.getValue());
    syncRepository.update(syncJob, syncJob.getId());

    return syncJob;
  }
}

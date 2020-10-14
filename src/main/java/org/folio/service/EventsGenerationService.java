package org.folio.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.folio.repository.SynchronizationJobRepository;
import org.folio.rest.client.OkapiClient;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.SynchronizationJob;
import org.joda.time.DateTime;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class EventsGenerationService {

  protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static final int PAGE_LIMIT = 50;
  protected final SynchronizationJobRepository syncRepository;
  protected final OkapiClient okapiClient;

  protected EventsGenerationService(Map<String, String> okapiHeaders, Vertx vertx,
    SynchronizationJobRepository syncRepository) {

    this.okapiClient = new OkapiClient(vertx, okapiHeaders);
    this.syncRepository = syncRepository;
  }

  public Future<SynchronizationJob> generateEvents(SynchronizationJob syncJob, String path) {
    return okapiClient.getMany(path, 0, 0)
      .compose(response -> {
        int totalRecords = response.getInteger("totalRecords");
        int numberOfPages = calculateNumberOfPages(totalRecords);

        List<Future> generatedEventsForPages = new ArrayList<>();
        for (int i = 0; i < numberOfPages; i++) {
          addGeneratedEventsForEachPagesToList(syncJob, path, totalRecords,
            generatedEventsForPages, i);
        }
        return CompositeFuture.all(generatedEventsForPages)
          .map(syncJob);
      });
  }

  protected abstract void addGeneratedEventsForEachPagesToList(SynchronizationJob syncJob,
    String path, int totalRecords, List<Future> generatedEventsForPages, int pageNumber);

  protected Metadata mapMetadataFromJson(JsonObject jsonMetadata) {
    return new Metadata()
      .withCreatedDate(getDateFromJson(jsonMetadata, "createdDate"))
      .withUpdatedDate(getDateFromJson(jsonMetadata, "updatedDate"));
  }

  protected Date getDateFromJson(JsonObject representation, String fieldName) {
    if (representation == null || representation.getString(fieldName) == null) {
      return null;
    }

    return DateTime.parse(representation.getString(fieldName)).toDate();
  }

  protected void updateSyncJobWithError(SynchronizationJob syncJob, String localizedMessage) {
    List<String> errors = syncJob.getErrors();
    errors.add(localizedMessage);
    syncRepository.update(syncJob.withErrors(errors), syncJob.getId());
  }

  protected int calculateNumberOfPages(int totalRecords) {
    return (int) Math.ceil((totalRecords / (double) PAGE_LIMIT));
  }
}

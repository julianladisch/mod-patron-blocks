package org.folio.rest.impl;

import static java.lang.String.format;
import static org.folio.rest.utils.EntityBuilder.buildSynchronizationRequest;
import static org.folio.rest.utils.matcher.SynchronizationRequestMatchers.newSynchronizationRequestByUser;
import static org.folio.rest.utils.matcher.SynchronizationRequestMatchers.newSynchronizationRequestFull;
import static org.hamcrest.Matchers.is;

import org.folio.domain.SynchronizationStatus;
import org.folio.repository.SynchronizationRequestRepository;
import org.folio.rest.TestBase;
import org.folio.rest.jaxrs.model.SynchronizationJob;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class SynchronizationAPITests extends TestBase {
  private static final String SCOPE_FULL = "full";
  private static final String SCOPE_USER = "user";
  private static final String USER_ID = randomId();

  private final SynchronizationRequestRepository synchronizationRequestRepository =
    new SynchronizationRequestRepository(postgresClient);

  @Test
  public void shouldRespondWithSynchronizationRequestFull() {
    String synchronizationRequestId = createOpenSynchronizationRequestFull();

    sendGetSynchronizationRequest(synchronizationRequestId)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(is(newSynchronizationRequestFull(synchronizationRequestId)));
  }

  @Test
  public void shouldRespondWithSynchronizationRequestByUser() {
    String synchronizationRequestId = createOpenSynchronizationRequestByUser();

    sendGetSynchronizationRequest(synchronizationRequestId)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(is(newSynchronizationRequestByUser(synchronizationRequestId, USER_ID)));
  }

  private String createOpenSynchronizationRequestFull() {
    SynchronizationJob synchronizationRequest = buildSynchronizationRequest(SCOPE_FULL, null,
      SynchronizationStatus.OPEN, 0, 0, 0, 0);
    return waitFor(synchronizationRequestRepository.save(synchronizationRequest));
  }

  private String createOpenSynchronizationRequestByUser() {
    SynchronizationJob synchronizationRequest = buildSynchronizationRequest(SCOPE_USER, USER_ID,
      SynchronizationStatus.OPEN, 0, 0, 0, 0);
    return waitFor(synchronizationRequestRepository.save(synchronizationRequest));
  }

  private Response sendGetSynchronizationRequest(String id) {
    return okapiClient.get(format("automated-patron-blocks/synchronization/request/%s", id));
  }
}

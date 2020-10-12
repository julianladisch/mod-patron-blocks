package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;
import static org.folio.rest.utils.EntityBuilder.buildSynchronizationRequest;
import static org.folio.rest.utils.matcher.SynchronizationRequestMatchers.newSynchronizationRequestByUser;
import static org.folio.rest.utils.matcher.SynchronizationRequestMatchers.newSynchronizationRequestFull;
import static org.folio.rest.utils.matcher.SynchronizationRequestMatchers.synchronizationJobMatcher;
import static org.folio.rest.utils.matcher.SynchronizationRequestMatchers.synchronizationRequest;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.joda.time.LocalDateTime.now;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.folio.domain.SynchronizationStatus;
import org.folio.repository.EventRepository;
import org.folio.repository.SynchronizationRequestRepository;
import org.folio.rest.TestBase;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.jaxrs.model.SynchronizationJob;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class SynchronizationAPITests extends TestBase {
  private static final String SCOPE_FULL = "full";
  private static final String SCOPE_USER = "user";
  private static final String USER_ID = randomId();
  private static final String SYNC_REQUESTS = "sync_requests";
  private static final String ITEM_CHECKED_OUT_EVENT_TABLE_NAME = "item_checked_out_event";
  private static final String ITEM_DECLARED_LOST_EVENT_TABLE_NAME = "item_declared_lost_event";
  private static final String ITEM_CLAIMED_RETURNED_EVENT_TABLE_NAME = "item_claimed_returned_event";
  private static final String LOAN_DUE_DATE_CHANGED_EVENT_TABLE_NAME = "loan_due_date_changed_event";
  private static final String FEE_FINE_BALANCE_CHANGED_EVENT_TABLE_NAME = "fee_fine_balance_changed_event";
  private static final String DONE_STATUS = "done";

  private final SynchronizationRequestRepository synchronizationRequestRepository =
    new SynchronizationRequestRepository(postgresClient);

  @Before
  public void beforeEach() {
    super.resetMocks();
    deleteAllFromTable(ITEM_CHECKED_OUT_EVENT_TABLE_NAME);
    deleteAllFromTable(SYNC_REQUESTS);
  }

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

  @Test
  public void checkOutEventShouldBeCreatedAfterSynchronization() {
    Date dueDate = now().plusHours(1).toDate();
    stubLoans(dueDate, false, "Checked out");
    stubAccountsWithEmptyResponse();
    String syncRequestId = createOpenSynchronizationRequestByUser();
    EventRepository<ItemCheckedOutEvent> checkOutEventRepository = new EventRepository<>(
      postgresClient, ITEM_CHECKED_OUT_EVENT_TABLE_NAME, ItemCheckedOutEvent.class);

    runSynchronization();

    Awaitility.await()
      .atMost(5, SECONDS)
      .until(() -> waitFor(checkOutEventRepository.getByUserId(USER_ID)).size(), is(1));

    checkSyncJobUpdatedByLoanEvent(syncRequestId);
  }

  @Test
  public void claimedReturnedEventShouldBeCreatedAfterSynchronization() {
    stubLoans(now().plusHours(1).toDate(), false, "Claimed returned");
    stubAccountsWithEmptyResponse();
    String syncRequestId = createOpenSynchronizationRequestByUser();
    EventRepository<ItemClaimedReturnedEvent> itemClaimedReturnedEventRepository =
      new EventRepository<>(postgresClient, ITEM_CLAIMED_RETURNED_EVENT_TABLE_NAME,
        ItemClaimedReturnedEvent.class);

    runSynchronization();

    Awaitility.await()
      .atMost(5, SECONDS)
      .until(() -> waitFor(itemClaimedReturnedEventRepository.getByUserId(USER_ID)).size(), is(1));

    checkSyncJobUpdatedByLoanEvent(syncRequestId);
  }

  @Test
  public void declaredLostEventShouldBeCreatedAfterSynchronization() {
    stubLoans(now().plusHours(1).toDate(), false, "Declared lost");
    stubAccountsWithEmptyResponse();
    String syncRequestId = createOpenSynchronizationRequestByUser();
    EventRepository<ItemDeclaredLostEvent> itemDeclaredLostEventRepository =
      new EventRepository<>(postgresClient, ITEM_DECLARED_LOST_EVENT_TABLE_NAME,
        ItemDeclaredLostEvent.class);

    runSynchronization();

    Awaitility.await()
      .atMost(5, SECONDS)
      .until(() -> waitFor(itemDeclaredLostEventRepository.getByUserId(USER_ID)).size(), is(1));

    checkSyncJobUpdatedByLoanEvent(syncRequestId);
  }

  @Test
  public void dueDateChangedEventShouldBeCreatedAfterSynchronization() {
    Date dueDate = now().plusHours(1).toDate();
    stubLoans(dueDate, true, "Checked out");
    stubAccountsWithEmptyResponse();
    String syncRequestId = createOpenSynchronizationRequestByUser();
    EventRepository<LoanDueDateChangedEvent> loanDueDateChangedEventRepository = new EventRepository<>(
      postgresClient, LOAN_DUE_DATE_CHANGED_EVENT_TABLE_NAME, LoanDueDateChangedEvent.class);

    runSynchronization();

    Awaitility.await()
      .atMost(5, SECONDS)
      .until(() -> waitFor(loanDueDateChangedEventRepository.getByUserId(USER_ID)).size(), is(1));

    checkSyncJobUpdatedByLoanEvent(syncRequestId);
  }

  protected void runSynchronization() {
    okapiClient.post("/automated-patron-blocks/synchronization/run", EMPTY)
      .then()
      .statusCode(202);
  }

  protected void checkSyncJobUpdatedByLoanEvent(String syncRequestId) {
    Awaitility.await()
      .atMost(30, SECONDS)
      .until(() -> waitFor(synchronizationRequestRepository.get(syncRequestId))
        .orElse(null), is(synchronizationJobMatcher(DONE_STATUS, 1, 0, 1, 0)));
  }

  @Test
  public void feeFineBalanceEventShouldBeCreatedAfterSynchronization() {

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

  private static void stubLoans(Date dueDate, boolean recall, String itemStatus) {
    wireMock.stubFor(get(urlPathMatching("/loan-storage/loans"))
      .atPriority(5)
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(makeLoanResponseBody(dueDate, recall, randomId(), itemStatus))));
  }

  private static void stubAccounts() {
    wireMock.stubFor(get(urlPathMatching("/accounts"))
      .atPriority(5)
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(makeAccountsResponseBody())));
  }

  private static void stubAccountsWithEmptyResponse() {
    wireMock.stubFor(get(urlPathMatching("/accounts"))
      .atPriority(5)
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(makeAccountsEmptyResponseBody())));
  }

  private static String makeLoanResponseBody(Date dueDate, boolean recall, String itemId, String itemStatus) {

      JsonObject loan = new JsonObject()
        .put("id", randomId())
        .put("overdueFinePolicyId", randomId())
        .put("dueDate", new DateTime(dueDate).toString(ISODateTimeFormat.dateTime()))
        .put("dueDateChangedByRecall", recall)
        .put("itemId", itemId)
        .put("userId", USER_ID)
        .put("itemStatus", itemStatus);

      return new JsonObject()
          .put("loans", new JsonArray()
            .add(loan))
          .put("totalRecords", 1)
        .encodePrettily();
  }

  private static String makeAccountsResponseBody() {

    JsonObject account = new JsonObject()
      .put("id", randomId())
      .put("userId", USER_ID)
      .put("loanId", randomId())
      .put("feeFineId", randomId())
      .put("feeFineType", "Type1")
      .put("remaining", 1.0);

    return new JsonObject()
      .put("accounts", new JsonArray()
        .add(account))
      .put("totalRecords", 1)
      .encodePrettily();
  }

  private static String makeAccountsEmptyResponseBody() {

    return new JsonObject()
      .put("accounts", new JsonArray())
      .put("totalRecords", 0)
      .encodePrettily();
  }
}

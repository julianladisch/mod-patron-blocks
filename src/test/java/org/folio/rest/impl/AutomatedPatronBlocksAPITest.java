package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.domain.Condition.MAX_NUMBER_OF_ITEMS_CHARGED_OUT;
import static org.folio.domain.Condition.MAX_NUMBER_OF_LOST_ITEMS;
import static org.folio.domain.Condition.MAX_NUMBER_OF_OVERDUE_ITEMS;
import static org.folio.domain.Condition.MAX_NUMBER_OF_OVERDUE_RECALLS;
import static org.folio.domain.Condition.MAX_OUTSTANDING_FEE_FINE_BALANCE;
import static org.folio.domain.Condition.RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS;
import static org.folio.repository.PatronBlockLimitsRepository.PATRON_BLOCK_LIMITS_TABLE_NAME;
import static org.folio.repository.UserSummaryRepository.USER_SUMMARY_TABLE_NAME;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.joda.time.DateTime.now;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import org.folio.domain.Condition;
import org.folio.domain.OpenFeeFine;
import org.folio.domain.OpenLoan;
import org.folio.domain.UserSummary;
import org.folio.repository.PatronBlockConditionsRepository;
import org.folio.repository.PatronBlockLimitsRepository;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
import org.folio.rest.jaxrs.model.AutomatedPatronBlock;
import org.folio.rest.jaxrs.model.AutomatedPatronBlocks;
import org.folio.rest.jaxrs.model.PatronBlockCondition;
import org.folio.rest.jaxrs.model.PatronBlockLimit;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class AutomatedPatronBlocksAPITest extends TestBase {
  private static final String USER_ID = randomId();
  private static final String PATRON_GROUP_ID = randomId();

  private final PatronBlockLimitsRepository limitsRepository =
    new PatronBlockLimitsRepository(postgresClient);
  private final UserSummaryRepository summaryRepository =
    new UserSummaryRepository(postgresClient);
  private final PatronBlockConditionsRepository conditionsRepository =
    new PatronBlockConditionsRepository(postgresClient);

  private static final EnumMap<Condition, Integer> LIMIT_VALUES;
  static {
    LIMIT_VALUES = new EnumMap<>(Condition.class);
    LIMIT_VALUES.put(MAX_NUMBER_OF_ITEMS_CHARGED_OUT, 5);
    LIMIT_VALUES.put(MAX_NUMBER_OF_LOST_ITEMS, 2);
    LIMIT_VALUES.put(MAX_NUMBER_OF_OVERDUE_ITEMS, 4);
    LIMIT_VALUES.put(MAX_NUMBER_OF_OVERDUE_RECALLS, 3);
    LIMIT_VALUES.put(RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS, 6);
    LIMIT_VALUES.put(MAX_OUTSTANDING_FEE_FINE_BALANCE, 7);
  }

  @Before
  public void beforeEach() {
    super.resetMocks();
    mockUsersResponse();
    deleteAllFromTable(PATRON_BLOCK_LIMITS_TABLE_NAME);
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void shouldReturnBadRequestErrorWhenCalledWithInvalidUserId() {
    sendRequest("invalid")
      .then()
      .statusCode(400)
      .contentType(ContentType.TEXT)
      .body(equalTo("Invalid user UUID: \"invalid\""));
  }

  @Test
  public void shouldReturnNoBlocksWhenUserSummaryDoesNotExist() {
    String emptyBlocksResponse = toJson(new AutomatedPatronBlocks());

    sendRequest(randomId())
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(emptyBlocksResponse));
  }

  @Test
  public void shouldReturnNoBlocksWhenNoLimitsExistForPatronGroup() {
    createSummary(USER_ID, BigDecimal.TEN, 12);
    String emptyBlocksResponse = toJson(new AutomatedPatronBlocks());

    sendRequest(randomId())
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(emptyBlocksResponse));
  }

  @Test
  public void maxNumberOfItemsChargedOutLimitIsExceeded() {
    final Condition condition = MAX_NUMBER_OF_ITEMS_CHARGED_OUT;
    final int limitValue = LIMIT_VALUES.get(condition);

    createLimitsForAllConditions();

    OpenLoan openLoan = new OpenLoan()
      .withLoanId(randomId())
      .withRecall(false)
      .withDueDate(new Date())
      .withReturnedDate(null);

    List<OpenLoan> threeOpenLoans = fillListOfSize(openLoan, limitValue + 1);
    createSummary(USER_ID, BigDecimal.ZERO, 0, new ArrayList<>(), threeOpenLoans);

    String expectedResponse = buildDefaultResponseFor(condition);

    sendRequest(USER_ID)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(expectedResponse));
  }

  @Test
  public void maxNumberOfLostItemsLimitIsExceeded() {
    final Condition condition = MAX_NUMBER_OF_LOST_ITEMS;
    int limitValue = LIMIT_VALUES.get(condition);

    createLimitsForAllConditions();
    createSummary(USER_ID, BigDecimal.ZERO, limitValue + 1);

    String expectedResponse = buildDefaultResponseFor(condition);

    sendRequest(USER_ID)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(expectedResponse));
  }

  @Test
  public void maxNumberOfOverdueItemsLimitIsExceeded() {
    final Condition condition = MAX_NUMBER_OF_OVERDUE_ITEMS;
    int limitValue = LIMIT_VALUES.get(condition);

    createLimitsForAllConditions();

    DateTime dueDate = now();
    DateTime returnedDate = dueDate.plusDays(1);

    OpenLoan overdueLoan = new OpenLoan()
      .withLoanId(randomId())
      .withRecall(false)
      .withDueDate(dueDate.toDate())
      .withReturnedDate(returnedDate.toDate());

    List<OpenLoan> overdueLoans = fillListOfSize(overdueLoan, limitValue + 1);
    createSummary(USER_ID, BigDecimal.ZERO, 0, new ArrayList<>(), overdueLoans);

    String expectedResponse = buildDefaultResponseFor(condition);

    sendRequest(USER_ID)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(expectedResponse));
  }

  @Test
  public void maxNumberOfOverdueRecallsLimitIsExceeded() {
    final Condition condition = MAX_NUMBER_OF_OVERDUE_RECALLS;
    int limitValue = LIMIT_VALUES.get(condition);

    createLimitsForAllConditions();

    DateTime dueDate = now();
    DateTime returnedDate = dueDate.plusDays(1);

    OpenLoan overdueLoan = new OpenLoan()
      .withLoanId(randomId())
      .withRecall(true)
      .withDueDate(dueDate.toDate())
      .withReturnedDate(returnedDate.toDate());

    List<OpenLoan> loans = fillListOfSize(overdueLoan, limitValue + 1);
    createSummary(USER_ID, BigDecimal.ZERO, 0, new ArrayList<>(), loans);

    String expectedResponse = buildDefaultResponseFor(condition);

    sendRequest(USER_ID)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(expectedResponse));
  }

  @Test
  public void recallOverdueByMaximumNumberOfDaysLimitIsExceeded() {
    final Condition condition = RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS;
    int limitValue = LIMIT_VALUES.get(condition);

    createLimitsForAllConditions();

    DateTime dueDate = now();
    DateTime returnedDate1 = dueDate.plusDays(limitValue - 1);
    DateTime returnedDate2 = dueDate.plusDays(limitValue + 1);

    OpenLoan overdueLoan1 = new OpenLoan()
      .withLoanId(randomId())
      .withRecall(true)
      .withDueDate(dueDate.toDate())
      .withReturnedDate(returnedDate1.toDate());

    OpenLoan overdueLoan2 = new OpenLoan()
      .withLoanId(randomId())
      .withRecall(true)
      .withDueDate(dueDate.toDate())
      .withReturnedDate(returnedDate2.toDate());

    List<OpenLoan> openLoans = Arrays.asList(overdueLoan1, overdueLoan2);
    createSummary(USER_ID, BigDecimal.ZERO, 0, new ArrayList<>(), openLoans);

    String expectedResponse = buildDefaultResponseFor(condition);

    sendRequest(USER_ID)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(expectedResponse));
  }

  @Test
  public void maxOutstandingFeeFineBalanceLimitIsExceeded() {
    final Condition condition = MAX_OUTSTANDING_FEE_FINE_BALANCE;
    int limitValue = LIMIT_VALUES.get(condition);

    createLimitsForAllConditions();

    BigDecimal outstandingFeeFineBalance =  BigDecimal.valueOf(limitValue + 5);
    createSummary(USER_ID, outstandingFeeFineBalance, 0);

    String expectedResponse = buildDefaultResponseFor(condition);

    sendRequest(USER_ID)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(expectedResponse));
  }

  @Test
  public void allLimitsAreExceeded() {
    createLimitsForAllConditions();

    BigDecimal outstandingBalance = BigDecimal.valueOf(
      LIMIT_VALUES.get(MAX_OUTSTANDING_FEE_FINE_BALANCE) + 5);

    int maxOverdueRecallLimit = LIMIT_VALUES.get(RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS);
    DateTime dueDate = now();
    DateTime returnedDate = dueDate.plusDays(maxOverdueRecallLimit + 1);

    OpenLoan overdueRecalledLoan = new OpenLoan()
      .withLoanId(randomId())
      .withRecall(true)
      .withDueDate(dueDate.toDate())
      .withReturnedDate(returnedDate.toDate());

    int numberOfOpenLoans = Math.max(
      LIMIT_VALUES.get(MAX_NUMBER_OF_ITEMS_CHARGED_OUT), Math.max(
          LIMIT_VALUES.get(MAX_NUMBER_OF_OVERDUE_ITEMS),
          LIMIT_VALUES.get(MAX_NUMBER_OF_OVERDUE_RECALLS))) + 1;

    List<OpenLoan> openLoans = fillListOfSize(overdueRecalledLoan, numberOfOpenLoans);

    int numberOfLostItems = LIMIT_VALUES.get(MAX_NUMBER_OF_LOST_ITEMS) + 1;

    createSummary(USER_ID, outstandingBalance, numberOfLostItems, new ArrayList<>(), openLoans);

    String expectedResponse = buildDefaultResponseFor(Condition.values());

    sendRequest(USER_ID)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(expectedResponse));
  }

  @Test
  public void updatedValuesFromConditionArePassedToResponse(TestContext context) {
    final Condition condition = MAX_NUMBER_OF_LOST_ITEMS;
    int limitValue = LIMIT_VALUES.get(condition);

    createLimitsForAllConditions();
    createSummary(USER_ID, BigDecimal.ZERO, limitValue + 1);

    Optional<PatronBlockCondition> optionalResult =
      waitFor(conditionsRepository.get(condition.getId()));

    context.assertTrue(optionalResult.isPresent());

    final PatronBlockCondition originalCondition = optionalResult.get();

    PatronBlockCondition updatedCondition = new PatronBlockCondition()
      .withId(condition.getId())
      .withBlockBorrowing(!originalCondition.getBlockBorrowing())
      .withBlockRenewals(!originalCondition.getBlockRenewals())
      .withBlockRequests(!originalCondition.getBlockRequests())
      .withMessage("Can't do anything");

    waitFor(conditionsRepository.update(updatedCondition));

    String expectedResponse = toJson(
      new AutomatedPatronBlocks().withAutomatedPatronBlocks(singletonList(
        createBlock(condition, updatedCondition.getMessage(), updatedCondition.getBlockBorrowing(),
          updatedCondition.getBlockRenewals(), updatedCondition.getBlockRequests()))));

    sendRequest(USER_ID)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(expectedResponse));

    context.assertTrue(waitFor(conditionsRepository.update(originalCondition)));
  }

  private Response sendRequest(String userId) {
    return okapiClient.get("automated-patron-blocks/" + userId);
  }

  private void mockUsersResponse() {
    String mockResponse = new JsonObject()
      .put("id", USER_ID)
      .put("patronGroup", PATRON_GROUP_ID)
      .encodePrettily();

    wireMock.stubFor(get(urlPathMatching("/users/.+"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(mockResponse)
      ));
  }

  private static String toJson(Object object) {
    return JsonObject.mapFrom(object)
      .encodePrettily();
  }

  private String createLimit(Condition condition, String patronGroupId, double value) {
    PatronBlockLimit limit = new PatronBlockLimit()
      .withId(randomId())
      .withConditionId(condition.getId())
      .withPatronGroupId(patronGroupId)
      .withValue(value);

    return waitFor(limitsRepository.save(limit));
  }

  private String createSummary(String userId, BigDecimal outstandingFeeFineBalance,
    int numberOfLostItems) {

    UserSummary userSummary = new UserSummary()
      .withId(randomId())
      .withOutstandingFeeFineBalance(outstandingFeeFineBalance)
      .withUserId(userId)
      .withNumberOfLostItems(numberOfLostItems);

    return waitFor(summaryRepository.save(userSummary));
  }

  private String createSummary(String userId, BigDecimal outstandingFeeFineBalance,
    int numberOfLostItems, List<OpenFeeFine> feesFines, List<OpenLoan> openLoans) {

    UserSummary userSummary = new UserSummary()
      .withId(randomId())
      .withOutstandingFeeFineBalance(outstandingFeeFineBalance)
      .withUserId(userId)
      .withNumberOfLostItems(numberOfLostItems)
      .withOpenFeesFines(feesFines)
      .withOpenLoans(openLoans);

    return waitFor(summaryRepository.save(userSummary));
  }

  private AutomatedPatronBlock createBlock(Condition condition, String message,
    boolean blockBorrowing, boolean blockRenewals, boolean blockRequests) {

    return new AutomatedPatronBlock()
      .withMessage(message)
      .withBlockBorrowing(blockBorrowing)
      .withBlockRenewals(blockRenewals)
      .withBlockRequests(blockRequests)
      .withPatronBlockConditionId(condition.getId());
  }

  private <T> List<T> fillListOfSize(T object, int listSize) {
    List<T> list = new ArrayList<>();
    for (int i = 0; i < listSize; i++) {
      list.add(object);
    }
    return list;
  }

  private void createLimitsForAllConditions() {
    for (Condition condition : Condition.values()) {
      createLimit(condition, PATRON_GROUP_ID, LIMIT_VALUES.get(condition));
    }
  }

  private String buildDefaultResponseFor(Condition... conditions) {
    return toJson(
      new AutomatedPatronBlocks().withAutomatedPatronBlocks(
        Arrays.stream(conditions)
          .map(condition -> createBlock(condition, EMPTY, false, false, false))
          .collect(toList())
      ));
  }

}

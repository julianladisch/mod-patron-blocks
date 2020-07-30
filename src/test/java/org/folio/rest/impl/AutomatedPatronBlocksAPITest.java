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
import static org.folio.rest.utils.EntityBuilder.buildFeeFine;
import static org.folio.rest.utils.EntityBuilder.buildLoan;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.joda.time.DateTime.now;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.folio.domain.Condition;
import org.folio.repository.PatronBlockConditionsRepository;
import org.folio.repository.PatronBlockLimitsRepository;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.TestBase;
import org.folio.rest.jaxrs.model.AutomatedPatronBlock;
import org.folio.rest.jaxrs.model.AutomatedPatronBlocks;
import org.folio.rest.jaxrs.model.OpenFeeFine;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.PatronBlockCondition;
import org.folio.rest.jaxrs.model.PatronBlockLimit;
import org.folio.rest.jaxrs.model.UserSummary;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class AutomatedPatronBlocksAPITest extends TestBase {
  private static final String USER_ID = randomId();
  private static final String PATRON_GROUP_ID = randomId();
  private static final boolean SINGLE_LIMIT = true;
  private static final boolean ALL_LIMITS = false;

  private final PatronBlockLimitsRepository limitsRepository =
    new PatronBlockLimitsRepository(postgresClient);
  private final UserSummaryRepository summaryRepository =
    new UserSummaryRepository(postgresClient);
  private final PatronBlockConditionsRepository conditionsRepository =
    new PatronBlockConditionsRepository(postgresClient);

  private static final EnumMap<Condition, Integer> LIMIT_VALUES;
  static {
    LIMIT_VALUES = new EnumMap<>(Condition.class);
    LIMIT_VALUES.put(MAX_NUMBER_OF_ITEMS_CHARGED_OUT, 50);
    LIMIT_VALUES.put(MAX_NUMBER_OF_LOST_ITEMS, 20);
    LIMIT_VALUES.put(MAX_NUMBER_OF_OVERDUE_ITEMS, 40);
    LIMIT_VALUES.put(MAX_NUMBER_OF_OVERDUE_RECALLS, 30);
    LIMIT_VALUES.put(RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS, 60);
    LIMIT_VALUES.put(MAX_OUTSTANDING_FEE_FINE_BALANCE, 70);
  }

  private boolean expectBlockBorrowing;
  private boolean expectBlockRenewals;
  private boolean expectBlockRequests;

  @Before
  public void beforeEach() {
    super.resetMocks();
    mockUsersResponse();
    deleteAllFromTable(PATRON_BLOCK_LIMITS_TABLE_NAME);
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);

    expectBlockBorrowing = false;
    expectBlockRenewals = false;
    expectBlockRequests = false;

    Arrays.stream(Condition.values())
      .forEach(condition -> updateCondition(condition, true, true, true));
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
    sendRequestAndCheckResult(randomId(), emptyBlocksResponse);
  }

  @Test
  public void shouldReturnNoBlocksWhenNoLimitsExistForPatronGroup() {
    createSummary(USER_ID);
    String emptyBlocksResponse = toJson(new AutomatedPatronBlocks());
    sendRequestAndCheckResult(randomId(), emptyBlocksResponse);
  }

  private void validateMaxNumberOfItemsChargedOutBlockResponse(int openLoansSizeDelta,
    boolean singleLimit) {

    final Condition condition = MAX_NUMBER_OF_ITEMS_CHARGED_OUT;
    final int limitValue = LIMIT_VALUES.get(condition);

    OpenLoan openLoan = buildLoan(false, false, now().plusHours(1).toDate());
    List<OpenLoan> openLoans = fillListOfSize(openLoan, limitValue + openLoansSizeDelta);
    createSummary(USER_ID, new ArrayList<>(), openLoans);

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenMaxNumberOfItemsChargedOutLimitIsNotReached() {
    expectNoBlocks();
    validateMaxNumberOfItemsChargedOutBlockResponse(-1, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenMaxNumberOfItemsChargedOutLimitIsNotReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateMaxNumberOfItemsChargedOutBlockResponse(-1, ALL_LIMITS);
  }

  @Test
  public void blockWhenMaxNumberOfItemsChargedOutLimitIsReached() {
    expectBlockBorrowing = true;
    expectBlockRenewals = false;
    expectBlockRequests = false;
    validateMaxNumberOfItemsChargedOutBlockResponse(0, SINGLE_LIMIT);
  }

  @Test
  public void blockWhenMaxNumberOfItemsChargedOutLimitIsReachedAndAllLimitsExist() {
    expectBlockBorrowing = true;
    expectBlockRenewals = false;
    expectBlockRequests = false;
    validateMaxNumberOfItemsChargedOutBlockResponse(0, ALL_LIMITS);
  }

  @Test
  public void blockWhenMaxNumberOfItemsChargedOutLimitIsExceeded() {
    expectAllBlocks();
    validateMaxNumberOfItemsChargedOutBlockResponse(1, SINGLE_LIMIT);
  }

  @Test
  public void blockWhenMaxNumberOfItemsChargedOutLimitIsExceededAndAllLimitsExist() {
    expectAllBlocks();
    validateMaxNumberOfItemsChargedOutBlockResponse(1, ALL_LIMITS);
  }

  private void validateMaxNumberOfLostItemsBlockResponse(int lostItemsDelta, boolean singleLimit) {
    final Condition condition = MAX_NUMBER_OF_LOST_ITEMS;
    int limitValue = LIMIT_VALUES.get(condition);

    OpenLoan openLoan = buildLoan(false, true, now().plusHours(1).toDate());
    List<OpenLoan> openLoans = fillListOfSize(openLoan, limitValue + lostItemsDelta);
    createSummary(USER_ID, new ArrayList<>(), openLoans);

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenMaxNumberOfLostItemsLimitIsNotReached() {
    expectNoBlocks();
    validateMaxNumberOfLostItemsBlockResponse(-1, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenMaxNumberOfLostItemsLimitIsNotReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateMaxNumberOfLostItemsBlockResponse(-1, ALL_LIMITS);
  }

  @Test
  public void noBlockWhenMaxNumberOfLostItemsLimitIsReached() {
    expectNoBlocks();
    validateMaxNumberOfLostItemsBlockResponse(0, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenMaxNumberOfLostItemsLimitIsReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateMaxNumberOfLostItemsBlockResponse(0, ALL_LIMITS);
  }

  @Test
  public void blockWhenMaxNumberOfLostItemsLimitIsExceeded() {
    expectAllBlocks();
    validateMaxNumberOfLostItemsBlockResponse(1, SINGLE_LIMIT);
  }

  @Test
  public void blockWhenMaxNumberOfLostItemsLimitIsExceededAndAllLimitsExist() {
    expectAllBlocks();
    validateMaxNumberOfLostItemsBlockResponse(1, ALL_LIMITS);
  }

  private void validateMaxOverdueItemsBlockResponse(int openLoansSizeDelta, boolean singleLimit) {
    final Condition condition = MAX_NUMBER_OF_OVERDUE_ITEMS;
    int limitValue = LIMIT_VALUES.get(condition);

    OpenLoan overdueLoan = buildLoan(false, false, now().minusHours(1).toDate());
    List<OpenLoan> overdueLoans = fillListOfSize(overdueLoan, limitValue + openLoansSizeDelta);
    createSummary(USER_ID, new ArrayList<>(), overdueLoans);

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueItemsLimitIsNotReached() {
    expectNoBlocks();
    validateMaxOverdueItemsBlockResponse(-1, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueItemsLimitIsNotReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateMaxOverdueItemsBlockResponse(-1, ALL_LIMITS);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueItemsLimitIsReached() {
    expectNoBlocks();
    validateMaxOverdueItemsBlockResponse(0, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueItemsLimitReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateMaxOverdueItemsBlockResponse(0, ALL_LIMITS);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueItemsLimitIsExceeded() {
    expectAllBlocks();
    validateMaxOverdueItemsBlockResponse(1, SINGLE_LIMIT);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueItemsLimitIsExceededAndAllLimitsExist() {
    expectAllBlocks();
    validateMaxOverdueItemsBlockResponse(1, ALL_LIMITS);
  }

  private void validateMaxOverdueRecallsBlockResponse(int openLoansSizeDelta, boolean singleLimit) {
    Condition condition = MAX_NUMBER_OF_OVERDUE_RECALLS;
    int limitValue = LIMIT_VALUES.get(condition);

    OpenLoan overdueLoan = buildLoan(true, false, now().minusHours(1).toDate());
    List<OpenLoan> loans = fillListOfSize(overdueLoan, limitValue + openLoansSizeDelta);
    createSummary(USER_ID, new ArrayList<>(), loans);

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueRecallsLimitIsNotReached() {
    expectNoBlocks();
    validateMaxOverdueRecallsBlockResponse(-1, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueRecallsLimitIsNotReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateMaxOverdueRecallsBlockResponse(-1, ALL_LIMITS);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueRecallsLimitIsReached() {
    expectNoBlocks();
    validateMaxOverdueRecallsBlockResponse(0, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueRecallsLimitIsReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateMaxOverdueRecallsBlockResponse(0, ALL_LIMITS);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueRecallsLimitIsExceeded() {
    expectAllBlocks();
    validateMaxOverdueRecallsBlockResponse(1, SINGLE_LIMIT);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueRecallsLimitIsExceededAllLimitsExist() {
    expectAllBlocks();
    validateMaxOverdueRecallsBlockResponse(1, ALL_LIMITS);
  }

  private void validateRecallOverdueByMaximumNumberOfDaysBlockResponse(int dueDateDelta,
    boolean singleLimit) {

    final Condition condition = RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS;
    int limitValue = LIMIT_VALUES.get(condition);

    DateTime dueDateBelowLimit = now().minusDays(limitValue + dueDateDelta);
    OpenLoan overdueLoan = buildLoan(true, false, dueDateBelowLimit.toDate());
    createSummary(USER_ID, new ArrayList<>(), singletonList(overdueLoan));

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenRecallOverdueByMaximumNumberOfDaysLimitIsNotReached() {
    expectNoBlocks();
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(-1, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenRecallOverdueByMaximumNumberOfDaysLimitIsNotReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(-1, ALL_LIMITS);
  }

  @Test
  public void noBlockWhenRecallOverdueByMaximumNumberOfDaysLimitIsReached() {
    expectNoBlocks();
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(0, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenRecallOverdueByMaximumNumberOfDaysLimitIsReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(0, ALL_LIMITS);
  }

  @Test
  public void blockWhenRecallOverdueByMaximumNumberOfDaysLimitIsExceeded() {
    expectAllBlocks();
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(1, SINGLE_LIMIT);
  }

  @Test
  public void blockWhenRecallOverdueByMaximumNumberOfDaysLimitIsExceededAllLimitsExist() {
    expectAllBlocks();
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(1, ALL_LIMITS);
  }

  private void validateMaxOutstandingFeeFineBalanceBlockResponse(int feeFineBalanceDelta,
    boolean singleLimit) {

    final Condition condition = MAX_OUTSTANDING_FEE_FINE_BALANCE;
    int limitValue = LIMIT_VALUES.get(condition);
    int numberOfFeesFines = 2;

    BigDecimal balancePerFeeFine = BigDecimal.valueOf(limitValue + feeFineBalanceDelta)
      .divide(BigDecimal.valueOf(numberOfFeesFines), 2, RoundingMode.UNNECESSARY);

    OpenFeeFine openFeeFine = buildFeeFine(randomId(), randomId(), randomId(), balancePerFeeFine);
    List<OpenFeeFine> openFeeFines = fillListOfSize(openFeeFine, numberOfFeesFines);
    createSummary(USER_ID, openFeeFines, new ArrayList<>());

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenMaxOutstandingFeeFineBalanceLimitIsNotReached() {
    expectNoBlocks();
    validateMaxOutstandingFeeFineBalanceBlockResponse(-1, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenMaxOutstandingFeeFineBalanceLimitIsNotReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateMaxOutstandingFeeFineBalanceBlockResponse(-1, ALL_LIMITS);
  }

  @Test
  public void noBlockWhenMaxOutstandingFeeFineBalanceLimitIsReached() {
    expectNoBlocks();
    validateMaxOutstandingFeeFineBalanceBlockResponse(0, SINGLE_LIMIT);
  }

  @Test
  public void noBlockWhenMaxOutstandingFeeFineBalanceLimitIsReachedAndAllLimitsExist() {
    expectNoBlocks();
    validateMaxOutstandingFeeFineBalanceBlockResponse(0, ALL_LIMITS);
  }

  @Test
  public void blockWhenMaxOutstandingFeeFineBalanceLimitIsExceeded() {
    expectAllBlocks();
    validateMaxOutstandingFeeFineBalanceBlockResponse(1, SINGLE_LIMIT);
  }

  @Test
  public void blockWhenMaxOutstandingFeeFineBalanceLimitIsExceededAllLimitsExist() {
    expectAllBlocks();
    validateMaxOutstandingFeeFineBalanceBlockResponse(1, ALL_LIMITS);
  }

  @Test
  public void allLimitsAreExceeded() {
    expectAllBlocks();

    String loanId = randomId();
    createLimitsForAllConditions();

    int maxOverdueRecallLimit = LIMIT_VALUES.get(RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS);
    DateTime dueDate = now().minusDays(maxOverdueRecallLimit + 1);

    OpenLoan overdueRecalledLoan = buildLoan(loanId, true, true, dueDate.toDate());

    int numberOfOpenLoans = Collections.max(Arrays.asList(
      LIMIT_VALUES.get(MAX_NUMBER_OF_LOST_ITEMS),
      LIMIT_VALUES.get(MAX_NUMBER_OF_ITEMS_CHARGED_OUT),
      LIMIT_VALUES.get(MAX_NUMBER_OF_OVERDUE_ITEMS),
      LIMIT_VALUES.get(MAX_NUMBER_OF_OVERDUE_RECALLS))) + 1;

    BigDecimal balance = BigDecimal.valueOf(LIMIT_VALUES.get(MAX_OUTSTANDING_FEE_FINE_BALANCE) + 1);
    List<OpenFeeFine> openFeeFines = singletonList(buildFeeFine(loanId, randomId(), randomId(), balance));
    List<OpenLoan> openLoans = fillListOfSize(overdueRecalledLoan, numberOfOpenLoans);
    createSummary(USER_ID, openFeeFines, openLoans);

    String expectedResponse = buildDefaultResponseFor(Condition.values());

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void updatedValuesFromConditionArePassedToResponse(TestContext context) {
    final Condition condition = MAX_NUMBER_OF_ITEMS_CHARGED_OUT;
    int limitValue = LIMIT_VALUES.get(condition);

    createLimitsForAllConditions();

    OpenLoan openLoan = buildLoan(false, false, DateTime.now().plusHours(1).toDate());
    List<OpenLoan> openLoans = fillListOfSize(openLoan, limitValue + 1);
    createSummary(USER_ID, new ArrayList<>(), openLoans);

    Optional<PatronBlockCondition> optionalResult =
      waitFor(conditionsRepository.get(condition.getId()));

    context.assertTrue(optionalResult.isPresent());

    final PatronBlockCondition originalCondition = optionalResult.get();

    PatronBlockCondition updatedCondition = new PatronBlockCondition()
      .withId(condition.getId())
      .withBlockBorrowing(!originalCondition.getBlockBorrowing())
      .withBlockRenewals(originalCondition.getBlockRenewals())
      .withBlockRequests(!originalCondition.getBlockRequests())
      .withMessage(EMPTY);

    waitFor(conditionsRepository.update(updatedCondition));

    String expectedResponse = toJson(
      new AutomatedPatronBlocks().withAutomatedPatronBlocks(singletonList(
        createBlock(condition, updatedCondition.getMessage(), updatedCondition.getBlockBorrowing(),
          updatedCondition.getBlockRenewals(), updatedCondition.getBlockRequests()))
        .stream()
        .filter(Objects::nonNull)
        .collect(toList())
      ));

    sendRequestAndCheckResult(expectedResponse);

    context.assertTrue(waitFor(conditionsRepository.update(originalCondition)));
  }

  private Response sendRequest(String userId) {
    return okapiClient.get("automated-patron-blocks/" + userId);
  }

  private ValidatableResponse sendRequestAndCheckResult(String userId, String expectedResponse) {
    return sendRequest(userId)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(expectedResponse));
  }

  private ValidatableResponse sendRequestAndCheckResult(String expectedResponse) {
    return sendRequest(USER_ID)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .body(equalTo(expectedResponse));
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

  private void updateCondition(Condition condition, boolean blockBorrowing, boolean blockRenewals,
    boolean blockRequests) throws IllegalArgumentException {

    PatronBlockCondition patronBlockCondition =
      waitFor(conditionsRepository.get(condition.getId()))
        .orElseThrow(() -> new IllegalArgumentException("Invalid condition ID"))
        .withBlockBorrowing(blockBorrowing)
        .withBlockRenewals(blockRenewals)
        .withBlockRequests(blockRequests);

    waitFor(conditionsRepository.update(patronBlockCondition));
  }

  private String createLimit(Condition condition, String patronGroupId, double value) {
    PatronBlockLimit limit = new PatronBlockLimit()
      .withId(randomId())
      .withConditionId(condition.getId())
      .withPatronGroupId(patronGroupId)
      .withValue(value);

    return waitFor(limitsRepository.save(limit));
  }

  private String createSummary(String userId) {
    UserSummary userSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId);

    return waitFor(summaryRepository.save(userSummary));
  }

  private String createSummary(String userId, List<OpenFeeFine> feesFines,
    List<OpenLoan> openLoans) {

    UserSummary userSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withOpenFeesFines(feesFines)
      .withOpenLoans(openLoans);

    return waitFor(summaryRepository.save(userSummary));
  }

  private AutomatedPatronBlock createBlock(Condition condition, String message) {
    return createBlock(condition, message, expectBlockBorrowing, expectBlockRenewals, expectBlockRequests);
  }

  private AutomatedPatronBlock createBlock(Condition condition, String message,
    boolean blockBorrowing, boolean blockRenewals, boolean blockRequests) {

    if (blockBorrowing || blockRenewals || blockRequests) {
      return new AutomatedPatronBlock()
        .withMessage(message)
        .withBlockBorrowing(blockBorrowing)
        .withBlockRenewals(blockRenewals)
        .withBlockRequests(blockRequests)
        .withPatronBlockConditionId(condition.getId());
    }

    return null;
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
          .map(condition -> createBlock(condition, EMPTY))
          .filter(Objects::nonNull)
          .collect(toList())
      ));
  }

  private String createLimitsAndBuildExpectedResponse(Condition condition, boolean singleLimit) {

    int limitValue = LIMIT_VALUES.get(condition);

    if (singleLimit) {
      createLimit(condition, PATRON_GROUP_ID, limitValue);
    } else {
      createLimitsForAllConditions();
    }

    return buildDefaultResponseFor(condition);
  }

  private void expectNoBlocks() {
    expectBlockBorrowing = false;
    expectBlockRenewals = false;
    expectBlockRequests = false;
  }

  private void expectAllBlocks() {
    expectBlockBorrowing = true;
    expectBlockRenewals = true;
    expectBlockRequests = true;
  }
}

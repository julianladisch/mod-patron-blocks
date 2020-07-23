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
import org.junit.BeforeClass;
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
  private static final boolean BLOCK_SHOULD_EXIST = true;
  private static final boolean NO_BLOCKS_SHOULD_EXIST = false;
  private static final boolean SINGLE_LIMIT = true;
  private static final boolean ALL_LIMITS = false;

  private static PatronBlockLimitsRepository limitsRepository;
  private static UserSummaryRepository summaryRepository;
  private static PatronBlockConditionsRepository conditionsRepository;

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

  @Before
  public void beforeEach() {
    super.resetMocks();
    mockUsersResponse();
    deleteAllFromTable(PATRON_BLOCK_LIMITS_TABLE_NAME);
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @BeforeClass
  public static void beforeAll() {
    limitsRepository = new PatronBlockLimitsRepository(postgresClient);
    summaryRepository = new UserSummaryRepository(postgresClient);
    conditionsRepository = new PatronBlockConditionsRepository(postgresClient);

    activateAllConditions();
  }

  private static void activateAllConditions() {
    List<PatronBlockCondition> allConditions =
      waitFor(conditionsRepository.getAllWithDefaultLimit());

    for (PatronBlockCondition condition : allConditions) {
      waitFor(conditionsRepository.update(condition
        .withBlockBorrowing(true)
        .withBlockRenewals(true)
        .withBlockRequests(true)
      ));
    }
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
    boolean singleLimit, boolean blockShouldExist) {

    final Condition condition = MAX_NUMBER_OF_ITEMS_CHARGED_OUT;
    final int limitValue = LIMIT_VALUES.get(condition);

    OpenLoan openLoan = buildLoan(false, false, now().plusHours(1).toDate());
    List<OpenLoan> openLoans = fillListOfSize(openLoan, limitValue + openLoansSizeDelta);
    createSummary(USER_ID, new ArrayList<>(), openLoans);

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit,
      blockShouldExist);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenMaxNumberOfItemsChargedOutLimitIsNotReached() {
    validateMaxNumberOfItemsChargedOutBlockResponse(-1, SINGLE_LIMIT, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void noBlockWhenMaxNumberOfItemsChargedOutLimitIsNotReachedAndAllLimitsExist() {
    validateMaxNumberOfItemsChargedOutBlockResponse(-1, ALL_LIMITS, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfItemsChargedOutLimitIsReached() {
    validateMaxNumberOfItemsChargedOutBlockResponse(0, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfItemsChargedOutLimitIsReachedAndAllLimitsExist() {
    validateMaxNumberOfItemsChargedOutBlockResponse(0, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfItemsChargedOutLimitIsExceeded() {
    validateMaxNumberOfItemsChargedOutBlockResponse(1, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfItemsChargedOutLimitIsExceededAndAllLimitsExist() {
    validateMaxNumberOfItemsChargedOutBlockResponse(1, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  private void validateMaxNumberOfLostItemsBlockResponse(int lostItemsDelta, boolean singleLimit,
    boolean blockShouldExist) {

    final Condition condition = MAX_NUMBER_OF_LOST_ITEMS;
    int limitValue = LIMIT_VALUES.get(condition);

    OpenLoan openLoan = buildLoan(false, true, now().plusHours(1).toDate());
    List<OpenLoan> openLoans = fillListOfSize(openLoan, limitValue + lostItemsDelta);
    createSummary(USER_ID, new ArrayList<>(), openLoans);

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit,
      blockShouldExist);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenMaxNumberOfLostItemsLimitIsNotReached() {
    validateMaxNumberOfLostItemsBlockResponse(-1, SINGLE_LIMIT, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void noBlockWhenMaxNumberOfLostItemsLimitIsNotReachedAndAllLimitsExist() {
    validateMaxNumberOfLostItemsBlockResponse(-1, ALL_LIMITS, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfLostItemsLimitIsReached() {
    validateMaxNumberOfLostItemsBlockResponse(0, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfLostItemsLimitIsReachedAndAllLimitsExist() {
    validateMaxNumberOfLostItemsBlockResponse(0, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfLostItemsLimitIsExceeded() {
    validateMaxNumberOfLostItemsBlockResponse(1, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfLostItemsLimitIsExceededAndAllLimitsExist() {
    validateMaxNumberOfLostItemsBlockResponse(1, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  private void validateMaxOverdueItemsBlockResponse(int openLoansSizeDelta, boolean singleLimit,
    boolean blockShouldExist) {

    final Condition condition = MAX_NUMBER_OF_OVERDUE_ITEMS;
    int limitValue = LIMIT_VALUES.get(condition);

    OpenLoan overdueLoan = buildLoan(false, false, now().minusHours(1).toDate());
    List<OpenLoan> overdueLoans = fillListOfSize(overdueLoan, limitValue + openLoansSizeDelta);
    createSummary(USER_ID, new ArrayList<>(), overdueLoans);

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit,
      blockShouldExist);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueItemsLimitIsNotReached() {
    validateMaxOverdueItemsBlockResponse(-1, SINGLE_LIMIT, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueItemsLimitIsNotReachedAndAllLimitsExist() {
    validateMaxOverdueItemsBlockResponse(-1, ALL_LIMITS, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueItemsLimitIsReached() {
    validateMaxOverdueItemsBlockResponse(0, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueItemsLimitReachedAndAllLimitsExist() {
    validateMaxOverdueItemsBlockResponse(0, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueItemsLimitIsExceeded() {
    validateMaxOverdueItemsBlockResponse(1, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueItemsLimitIsExceededAndAllLimitsExist() {
    validateMaxOverdueItemsBlockResponse(1, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  private void validateMaxOverdueRecallsBlockResponse(int openLoansSizeDelta, boolean singleLimit,
    boolean blockShouldExist) {

    Condition condition = MAX_NUMBER_OF_OVERDUE_RECALLS;
    int limitValue = LIMIT_VALUES.get(condition);

    OpenLoan overdueLoan = buildLoan(true, false, now().minusHours(1).toDate());
    List<OpenLoan> loans = fillListOfSize(overdueLoan, limitValue + openLoansSizeDelta);
    createSummary(USER_ID, new ArrayList<>(), loans);

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit,
      blockShouldExist);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueRecallsLimitIsNotReached() {
    validateMaxOverdueRecallsBlockResponse(-1, SINGLE_LIMIT, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void noBlockWhenMaxNumberOfOverdueRecallsLimitIsNotReachedAndAllLimitsExist() {
    validateMaxOverdueRecallsBlockResponse(-1, ALL_LIMITS, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueRecallsLimitIsReached() {
    validateMaxOverdueRecallsBlockResponse(0, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueRecallsLimitIsReachedAndAllLimitsExist() {
    validateMaxOverdueRecallsBlockResponse(0, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueRecallsLimitIsExceeded() {
    validateMaxOverdueRecallsBlockResponse(1, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxNumberOfOverdueRecallsLimitIsExceededAllLimitsExist() {
    validateMaxOverdueRecallsBlockResponse(1, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  private void validateRecallOverdueByMaximumNumberOfDaysBlockResponse(int dueDateDelta,
    boolean singleLimit, boolean blockShouldExist) {

    final Condition condition = RECALL_OVERDUE_BY_MAX_NUMBER_OF_DAYS;
    int limitValue = LIMIT_VALUES.get(condition);

    DateTime dueDateBelowLimit = now().minusDays(limitValue + dueDateDelta);
    OpenLoan overdueLoan = buildLoan(true, false, dueDateBelowLimit.toDate());
    createSummary(USER_ID, new ArrayList<>(), singletonList(overdueLoan));

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit,
      blockShouldExist);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenRecallOverdueByMaximumNumberOfDaysLimitIsNotReached() {
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(-1, SINGLE_LIMIT,
      NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void noBlockWhenRecallOverdueByMaximumNumberOfDaysLimitIsNotReachedAndAllLimitsExist() {
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(-1, ALL_LIMITS, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void blockWhenRecallOverdueByMaximumNumberOfDaysLimitIsReached() {
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(0, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenRecallOverdueByMaximumNumberOfDaysLimitIsReachedAndAllLimitsExist() {
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(0, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenRecallOverdueByMaximumNumberOfDaysLimitIsExceeded() {
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(1, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenRecallOverdueByMaximumNumberOfDaysLimitIsExceededAllLimitsExist() {
    validateRecallOverdueByMaximumNumberOfDaysBlockResponse(1, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  private void validateMaxOutstandingFeeFineBalanceBlockResponse(int feeFineBalanceDelta,
    boolean singleLimit, boolean blockShouldExist) {

    final Condition condition = MAX_OUTSTANDING_FEE_FINE_BALANCE;
    int limitValue = LIMIT_VALUES.get(condition);
    int numberOfFeesFines = 2;

    BigDecimal balancePerFeeFine = BigDecimal.valueOf(limitValue + feeFineBalanceDelta)
      .divide(BigDecimal.valueOf(numberOfFeesFines), 2, RoundingMode.UNNECESSARY);

    OpenFeeFine openFeeFine = buildFeeFine(randomId(), randomId(), randomId(), balancePerFeeFine);
    List<OpenFeeFine> openFeeFines = fillListOfSize(openFeeFine, numberOfFeesFines);
    createSummary(USER_ID, openFeeFines, new ArrayList<>());

    String expectedResponse = createLimitsAndBuildExpectedResponse(condition, singleLimit,
      blockShouldExist);

    sendRequestAndCheckResult(expectedResponse);
  }

  @Test
  public void noBlockWhenMaxOutstandingFeeFineBalanceLimitIsNotReached() {
    validateMaxOutstandingFeeFineBalanceBlockResponse(-1, SINGLE_LIMIT, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void noBlockWhenMaxOutstandingFeeFineBalanceLimitIsNotReachedAndAllLimitsExist() {
    validateMaxOutstandingFeeFineBalanceBlockResponse(-1, ALL_LIMITS, NO_BLOCKS_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxOutstandingFeeFineBalanceLimitIsReached() {
    validateMaxOutstandingFeeFineBalanceBlockResponse(0, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxOutstandingFeeFineBalanceLimitIsReachedAndAllLimitsExist() {
    validateMaxOutstandingFeeFineBalanceBlockResponse(0, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxOutstandingFeeFineBalanceLimitIsExceeded() {
    validateMaxOutstandingFeeFineBalanceBlockResponse(1, SINGLE_LIMIT, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void blockWhenMaxOutstandingFeeFineBalanceLimitIsExceededAllLimitsExist() {
    validateMaxOutstandingFeeFineBalanceBlockResponse(1, ALL_LIMITS, BLOCK_SHOULD_EXIST);
  }

  @Test
  public void allLimitsAreExceeded() {
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
      .withMessage("Can't do anything");

    waitFor(conditionsRepository.update(updatedCondition));

    String expectedResponse = toJson(
      new AutomatedPatronBlocks().withAutomatedPatronBlocks(singletonList(
        createBlock(condition, updatedCondition.getMessage(), updatedCondition.getBlockBorrowing(),
          updatedCondition.getBlockRenewals(), updatedCondition.getBlockRequests()))));

    sendRequestAndCheckResult(expectedResponse);

    context.assertTrue(waitFor(conditionsRepository.update(originalCondition)));
  }

  @Test
  public void noBlockIsCreatedForInactiveCondition(TestContext context) {
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
      .withBlockBorrowing(false)
      .withBlockRenewals(false)
      .withBlockRequests(false)
      .withMessage("All blocks are set to 'false', so the condition is effectively disabled");

    waitFor(conditionsRepository.update(updatedCondition));

    String expectedResponse = toJson(new AutomatedPatronBlocks());

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

  private AutomatedPatronBlock createBlock(Condition condition, String message,
    boolean blockBorrowing, boolean blockRenewals, boolean blockRequests) {

    return new AutomatedPatronBlock()
      .withMessage(message)
      .withBlockBorrowing(blockBorrowing)
      .withBlockRenewals(blockRenewals)
      .withBlockRequests(blockRequests)
      .withPatronBlockConditionId(condition.getId());
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
          .map(condition -> createBlock(condition, EMPTY, true, true, true))
          .collect(toList())
      ));
  }

  private String createLimitsAndBuildExpectedResponse(Condition condition, boolean singleLimit,
    boolean blockShouldExist) {

    int limitValue = LIMIT_VALUES.get(condition);

    if (singleLimit) {
      createLimit(condition, PATRON_GROUP_ID, limitValue);
    } else {
      createLimitsForAllConditions();
    }

    String expectedResponse;
    if (blockShouldExist) {
      expectedResponse = buildDefaultResponseFor(condition);
    } else {
      expectedResponse = buildDefaultResponseFor();
    }

    return expectedResponse;
  }
}

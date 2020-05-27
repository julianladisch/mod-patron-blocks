package org.folio.rest.handlers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import org.folio.domain.OpenFeeFine;
import org.folio.domain.UserSummary;
import org.folio.repository.UserSummaryRepository;
import org.folio.repository.UserSummaryRepositoryImpl;
import org.folio.rest.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Future;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class FeeFineBalanceChangedEventHandlerTest extends TestBase {
  private static final String USER_SUMMARY_TABLE_NAME = "user_summary";
  private static final AbstractEventHandler eventHandler =
    new FeeFineBalanceChangedEventHandler(postgresClient);
  private static final UserSummaryRepository userSummaryRepository =
    new UserSummaryRepositoryImpl(postgresClient);

  @Before
  public void beforeEach() {
    super.resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE_NAME);
  }

  @Test
  public void createNewUserSummary(TestContext context) {
    final String userId = randomId();
    final String feeFineId = randomId();
    final String feeFineTypeId = randomId();
    final BigDecimal balance = new BigDecimal("1.55");

    String payload = createPayloadString(userId, feeFineId, feeFineTypeId, balance);
    waitFor(eventHandler.handle(payload));
    checkResult(userId, balance, 1, feeFineId, feeFineTypeId, balance, context);
  }

  @Test
  public void addNewFeeFineToExistingUserSummary(TestContext context) {
    final String userId = randomId();
    final BigDecimal initialOutstandingFeeFineBalance = new BigDecimal("2.55");

    UserSummary initialUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(initialOutstandingFeeFineBalance);

    OpenFeeFine existingFeeFine = new OpenFeeFine()
      .withBalance(initialOutstandingFeeFineBalance)
      .withFeeFineTypeId(randomId())
      .withFeeFineId(randomId());

    initialUserSummary.getOpenFeeFines().add(existingFeeFine);

    waitFor(userSummaryRepository.saveUserSummary(initialUserSummary));

    final String feeFineId = randomId();
    final String feeFineTypeId = randomId();
    final BigDecimal eventBalance = new BigDecimal("7.45");
    String payload = createPayloadString(userId, feeFineId, feeFineTypeId, eventBalance);

    waitFor(eventHandler.handle(payload));
    checkResult(userId, initialOutstandingFeeFineBalance.add(eventBalance), 2, feeFineId,
      feeFineTypeId, eventBalance, context);
  }

  @Test
  public void updateFeeFineBalanceInExistingUserSummary(TestContext context) {
    final String userId = randomId();
    final String feeFineId = randomId();
    final String feeFineTypeId = randomId();
    final BigDecimal initialFeeFineBalance = new BigDecimal("1.25");

    UserSummary existingUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(initialFeeFineBalance);

    OpenFeeFine existingFeeFine = new OpenFeeFine()
      .withBalance(initialFeeFineBalance)
      .withFeeFineTypeId(feeFineTypeId)
      .withFeeFineId(feeFineId);

    existingUserSummary.getOpenFeeFines().add(existingFeeFine);

    waitFor(userSummaryRepository.saveUserSummary(existingUserSummary));

    final BigDecimal eventBalance = new BigDecimal("2.75");
    String payload = createPayloadString(userId, feeFineId, feeFineTypeId, eventBalance);

    waitFor(eventHandler.handle(payload));
    checkResult(userId, eventBalance, 1, feeFineId, feeFineTypeId, eventBalance, context);
  }

  @Test
  public void deleteClosedFeeFineFromExistingUserSummary(TestContext context) {
    final String userId = randomId();

    final String feeFineId1 = randomId();
    final String feeFineTypeId1 = randomId();
    final BigDecimal feeFineBalance1 = new BigDecimal("1.25");

    OpenFeeFine existingFeeFine1 = new OpenFeeFine()
      .withFeeFineId(feeFineId1)
      .withFeeFineTypeId(feeFineTypeId1)
      .withBalance(feeFineBalance1);

    final String feeFineId2 = randomId();
    final String feeFineTypeId2 = randomId();
    final BigDecimal feeFineBalance2 = new BigDecimal("2.55");

    OpenFeeFine existingFeeFine2 = new OpenFeeFine()
      .withBalance(feeFineBalance2)
      .withFeeFineTypeId(feeFineTypeId2)
      .withFeeFineId(feeFineId2);

    UserSummary existingUserSummary = new UserSummary()
      .withId(randomId())
      .withUserId(userId)
      .withNumberOfLostItems(0)
      .withOutstandingFeeFineBalance(feeFineBalance1.add(feeFineBalance2))
      .withOpenFeesFines(Arrays.asList(existingFeeFine1, existingFeeFine2));

    waitFor(userSummaryRepository.saveUserSummary(existingUserSummary));

    String payload = createPayloadString(userId, feeFineId2, feeFineTypeId2, BigDecimal.ZERO);

    waitFor(eventHandler.handle(payload));
    checkResult(userId, feeFineBalance1, 1, feeFineId1, feeFineTypeId1, feeFineBalance1, context);
  }

  @Test
  public void eventWithInvalidJsonPayload(TestContext context) {
    String payload = "not a json";
    Future<String> handleFuture = eventHandler.handle(payload);
    waitFor(handleFuture);
    context.assertTrue(handleFuture.failed());
    context.assertTrue(handleFuture.cause() instanceof DecodeException);
  }

  @Test
  public void eventWithInvalidBalance(TestContext context) {
    String payload = createPayloadJson(randomId(), randomId(), randomId(), BigDecimal.ZERO)
      .put("balance", "zero")
      .encodePrettily();

    Future<String> handleFuture = eventHandler.handle(payload);
    waitFor(handleFuture);
    context.assertTrue(handleFuture.failed());
    context.assertTrue(handleFuture.cause() instanceof IllegalArgumentException);
    context.assertTrue(handleFuture.cause().getLocalizedMessage()
      .startsWith("Invalid fee/fine balance value in event payload"));
  }

  @Test
  public void eventWithInvalidUserId(TestContext context) {
    String invalidUserId = randomId() + "z";
    String payload = createPayloadString(invalidUserId, randomId(), randomId(), BigDecimal.ZERO);

    Future<String> handleFuture = eventHandler.handle(payload);
    waitFor(handleFuture);
    context.assertTrue(handleFuture.failed());
    context.assertTrue(handleFuture.cause() instanceof NumberFormatException);
  }

  private static String createPayloadString(String userId, String feeFineId, String feeFineTypeId,
    BigDecimal balance) {

    return createPayloadJson(userId, feeFineId, feeFineTypeId, balance)
      .encodePrettily();
  }

  private static JsonObject createPayloadJson(String userId, String feeFineId, String feeFineTypeId,
    BigDecimal balance) {

    return new JsonObject()
      .put("userId", userId)
      .put("feeFineId", feeFineId)
      .put("feeFineTypeId", feeFineTypeId)
      .put("balance", balance.doubleValue());
  }

  private void checkResult(String userId, BigDecimal expectedOutstandingFeeFineBalance,
    int expectedNumberOfOpenFeesFines, String expectedFeeFineId, String expectedFeeFineTypeId,
    BigDecimal expectedFeeFineBalance, TestContext context) {

    UserSummary userSummary = findUserSummaryByUserId(userId, context);

    context.assertNotNull(userSummary.getId());
    context.assertEquals(userId, userSummary.getUserId());
    context.assertEquals(expectedOutstandingFeeFineBalance,
      userSummary.getOutstandingFeeFineBalance());
    context.assertTrue(userSummary.getOpenLoans().isEmpty());
    context.assertEquals(0, userSummary.getNumberOfLostItems());
    context.assertEquals(expectedNumberOfOpenFeesFines, userSummary.getOpenFeeFines().size());

    OpenFeeFine openFeeFine = findOpenFeeFineById(userSummary, expectedFeeFineId, context);

    context.assertEquals(expectedFeeFineId, openFeeFine.getFeeFineId());
    context.assertEquals(expectedFeeFineTypeId, openFeeFine.getFeeFineTypeId());
    context.assertEquals(expectedFeeFineBalance, openFeeFine.getBalance());
  }

  private static OpenFeeFine findOpenFeeFineById(UserSummary userSummary, String feeFineId,
    TestContext context) {

    return userSummary.getOpenFeeFines().stream()
      .filter(feeFine -> feeFine.getFeeFineId().equals(feeFineId))
      .findFirst()
      .orElseGet(() -> {
        context.fail(String.format("Fee/fine %s was not found in given user summary", feeFineId));
        return null;
      });
  }

  private static UserSummary findUserSummaryByUserId(String userId, TestContext context) {
    Future<Optional<UserSummary>> findUser = userSummaryRepository.getUserSummaryByUserId(userId);
    waitFor(findUser);

    return findUser.result().orElseGet(() -> {
      context.fail(String.format("Summary for user %s was not found", userId));
      return null;
    });
  }

}
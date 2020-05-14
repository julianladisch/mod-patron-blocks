package org.folio.patronblocks.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.folio.patronblocks.model.UserSummary;
import org.folio.patronblocks.rest.APITests;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class UserSummaryRepositoryImplTest extends APITests {

  private static final String USER_SUMMARY_TABLE = "user_summary";
  private PostgresClient postgresClient = PostgresClient.getInstance(vertx, OKAPI_TENANT);
  private UserSummaryRepository userSummaryRepository =
    new UserSummaryRepositoryImpl(postgresClient);

  @Before
  public void setUp(TestContext context) {
    Async async = context.async();
    postgresClient
      .delete(USER_SUMMARY_TABLE, new Criterion(), event -> {
        if (event.failed()) {
          log.error(event.cause());
          context.fail(event.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldAddUserSummary() {
    String userId = UUID.randomUUID().toString();
    UserSummary userSummary =  createUserSummary(userId, UUID.randomUUID().toString(),
      BigDecimal.ONE, 2, 1);
    Future<String> userSummaryId = userSummaryRepository.saveUserSummary(userSummary);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(userSummaryId::result, notNullValue());

    assertThat(userSummaryId.result(), is(userId));

    Future<Optional<UserSummary>> userSummaryById =
      userSummaryRepository.getUserSummaryById(userId);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(() -> userSummaryById.result().isPresent());

    assertThat(matchesUserSummaries(userSummary, userSummaryById.result().get()), is(true));

  }

  @Test
  public void shouldUpdateUserSummary() {
    String userSummaryId = UUID.randomUUID().toString();
    Future<String> userSummary = userSummaryRepository.saveUserSummary(
      createUserSummary(userSummaryId, UUID.randomUUID().toString(), new BigDecimal(2), 4, 2));

    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(userSummary::result, notNullValue());

    UserSummary updatedUserSummary = createUserSummary(userSummaryId, UUID.randomUUID().toString(),
      new BigDecimal("10.3"), 3, 1);
    Future<Boolean> isUpdatedFuture = userSummaryRepository.updateUserSummary(updatedUserSummary);

    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(isUpdatedFuture::result);

    Future<Optional<UserSummary>> userSummaryById =
      userSummaryRepository.getUserSummaryById(userSummaryId);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(() -> userSummaryById.result().isPresent());

    assertThat(matchesUserSummaries(updatedUserSummary, userSummaryById.result().get()), is(true));
  }

  @Test
  public void shouldDeleteUserSummary() {
    String userSummaryId1 = UUID.randomUUID().toString();
    userSummaryRepository.saveUserSummary(createUserSummary(userSummaryId1,
      UUID.randomUUID().toString(), new BigDecimal(2), 4, 2));

    UserSummary userSummary = createUserSummary(
      UUID.randomUUID().toString(), UUID.randomUUID().toString(), new BigDecimal("3.25"), 3, 1);
    userSummaryRepository.saveUserSummary(userSummary);

    String userSummaryId3 = UUID.randomUUID().toString();
    userSummaryRepository.saveUserSummary(createUserSummary(userSummaryId3,
      UUID.randomUUID().toString(), new BigDecimal(4), 2, 3));

    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(userSummaryRepository.getUserSummaries(null, 0, 100)::result, hasSize(3));

    userSummaryRepository.deleteUserSummary(userSummaryId1);
    userSummaryRepository.deleteUserSummary(userSummaryId3);

    Future<List<UserSummary>> userSummaryRemaining =
      userSummaryRepository.getUserSummaries(null, 0, 100);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(userSummaryRemaining::result, hasSize(1));

    assertThat(matchesUserSummaries(userSummary, userSummaryRemaining.result().get(0)), is(true));
  }

  private UserSummary createUserSummary(String id, String userId, BigDecimal balance,
    int lostItems, int openFeesFinesForLostItems) {

    return new UserSummary()
      .withId(id)
      .withUserId(userId)
      .withNumberOfLostItems(lostItems)
      .withNumberOfOpenFeesFinesForLostItems(openFeesFinesForLostItems)
      .withOutstandingFeeFineBalance(balance);
  }

  private boolean matchesUserSummaries(UserSummary expected, UserSummary actual) {
    return actual.getId().equals(expected.getId())
      && actual.getUserId().equals(expected.getUserId())
      && actual.getNumberOfLostItems().equals(expected.getNumberOfLostItems())
      && actual.getNumberOfOpenFeesFinesForLostItems().equals(
        expected.getNumberOfOpenFeesFinesForLostItems())
      && actual.getOutstandingFeeFineBalance().equals(expected.getOutstandingFeeFineBalance());
  }
}

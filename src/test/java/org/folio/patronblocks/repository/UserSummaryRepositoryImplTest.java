package org.folio.patronblocks.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.folio.patronblocks.rest.APITests;
import org.folio.rest.jaxrs.model.UserSummary;
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

  private UserSummaryRepository userSummaryRepository = new UserSummaryRepositoryImpl(
    PostgresClient.getInstance(vertx, "test_tenant"));
  private static final String USER_SUMMARY_TABLE = "user_summary";

  @Before
  public void setUp(TestContext context) {
    Async async = context.async();
    PostgresClient.getInstance(vertx, OKAPI_TENANT)
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
    Future<String> userSumId = userSummaryRepository.saveUserSummary(
      createUserSummary(userId, BigDecimal.ONE, 2, 1));
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(() -> userSumId.result() != null);

    assertThat(userSumId.result(), is(userId));

    Future<Optional<UserSummary>> userSummaryById =
      userSummaryRepository.getUserSummaryById(userId);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(() -> userSummaryById.result().isPresent());

    UserSummary resultUserSummary = userSummaryById.result().get();

    assertThat(resultUserSummary.getId(), is(userId));
    assertThat(resultUserSummary.getNumberOfLostItems(), is(2));
    assertThat(resultUserSummary.getNumberOfOpenFeesFinesForLostItems(), is(1));
    assertThat(resultUserSummary.getOutstandingFeeFineBalance(), is(BigDecimal.ONE));
  }

  @Test
  public void shouldUpdateUserSummary() {
    String userId = UUID.randomUUID().toString();
    Future<String> userSumId = userSummaryRepository.saveUserSummary(
      createUserSummary(userId, new BigDecimal(2), 4, 2));

    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(() -> userSumId.result() != null);

    Future<Boolean> isUpdatedFuture = userSummaryRepository.updateUserSummary(
      createUserSummary(userId, new BigDecimal(10), 3, 1));

    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(isUpdatedFuture::result);

    Future<Optional<UserSummary>> userSummaryById =
      userSummaryRepository.getUserSummaryById(userId);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(() -> userSummaryById.result().isPresent());

    UserSummary updatedUserSummaryRecord = userSummaryById.result().get();
    assertThat(updatedUserSummaryRecord.getNumberOfLostItems(), is(3));
    assertThat(updatedUserSummaryRecord.getNumberOfOpenFeesFinesForLostItems(), is(1));
    assertThat(updatedUserSummaryRecord.getOutstandingFeeFineBalance(), is(BigDecimal.TEN));
  }

  @Test
  public void shouldDeleteUserSummary() {
    String userId1 = UUID.randomUUID().toString();
    Future<String> userSumId1 = userSummaryRepository.saveUserSummary(
      createUserSummary(userId1, new BigDecimal(2), 4, 2));
    String userId2 = UUID.randomUUID().toString();
    Future<String> userSumId2 = userSummaryRepository.saveUserSummary(
      createUserSummary(userId2, new BigDecimal(3), 3, 1));
    Future<String> userSumId3 = userSummaryRepository.saveUserSummary(
      createUserSummary(UUID.randomUUID().toString(), new BigDecimal(1), 2, 3));

    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(() -> userSumId1.result() != null);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(() -> userSumId2.result() != null);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(() -> userSumId3.result() != null);

    Future<List<UserSummary>> userSummaries =
      userSummaryRepository.getUserSummaries(null, 0, 100);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(userSummaries::result, hasSize(3));

    userSummaryRepository.deleteUserSummary(userId1);
    userSummaryRepository.deleteUserSummary(userId2);

    Future<List<UserSummary>> userSummaryRemaining =
      userSummaryRepository.getUserSummaries(null, 0, 100);
    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(userSummaryRemaining::result, hasSize(1));

    assertThat(userSummaryRemaining.result().get(0).getNumberOfLostItems(), is(2));
    assertThat(userSummaryRemaining.result().get(0)
      .getNumberOfOpenFeesFinesForLostItems(), is(3));
    assertThat(userSummaryRemaining.result().get(0).getOutstandingFeeFineBalance(),
      is(BigDecimal.ONE));
  }

  private UserSummary createUserSummary(String userId, BigDecimal balance,
    int lostItems, int openFeesFinesForLostItems) {

    return new UserSummary()
      .withId(userId)
      .withNumberOfLostItems(lostItems)
      .withNumberOfOpenFeesFinesForLostItems(openFeesFinesForLostItems)
      .withOutstandingFeeFineBalance(balance);
  }
}

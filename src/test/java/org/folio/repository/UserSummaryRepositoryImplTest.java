package org.folio.repository;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.folio.domain.UserSummary;
import org.folio.rest.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.CompositeFuture;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class UserSummaryRepositoryImplTest extends TestBase {

  private static final String USER_SUMMARY_TABLE = "user_summary";
  private final UserSummaryRepository repository = new UserSummaryRepositoryImpl(postgresClient);

  @Before
  public void setUp() {
    resetMocks();
    deleteAllFromTable(USER_SUMMARY_TABLE);
  }

  @Test
  public void shouldAddUserSummary(TestContext context) {
    String summaryId = randomId();
    UserSummary userSummaryToSave =  createUserSummary(summaryId, randomId(), ONE, 2);
    waitFor(repository.saveUserSummary(userSummaryToSave));

    Optional<UserSummary> retrievedUserSummary = waitFor(repository.getUserSummaryById(summaryId));

    context.assertTrue(retrievedUserSummary.isPresent());
    assertSummariesAreEqual(userSummaryToSave, retrievedUserSummary.get(), context);
  }

  @Test
  public void shouldGetUserSummaryByUserId(TestContext context) {
    UserSummary expectedUserSummary = createUserSummary(randomId(), randomId(), ONE, 1);

    waitFor(CompositeFuture.all(
      repository.saveUserSummary(createUserSummary(randomId(), randomId(), ONE, 3)),
      repository.saveUserSummary(expectedUserSummary),
      repository.saveUserSummary(createUserSummary(randomId(), randomId(), ONE, 5)))
    );

    Optional<UserSummary> retrievedUserSummary =
      waitFor(repository.getUserSummaryByUserId(expectedUserSummary.getUserId()));

    context.assertTrue(retrievedUserSummary.isPresent());
    assertSummariesAreEqual(expectedUserSummary, retrievedUserSummary.get(), context);
  }

  @Test
  public void shouldUpdateUserSummary(TestContext context) {
    String userSummaryId = randomId();

    waitFor(repository.saveUserSummary(
      createUserSummary(userSummaryId, randomId(), new BigDecimal(2), 4)));

    UserSummary updatedUserSummary = createUserSummary(userSummaryId, randomId(),
      new BigDecimal("10.3"), 3);

    waitFor(repository.updateUserSummary(updatedUserSummary));
    Optional<UserSummary> userSummary = waitFor(repository.getUserSummaryById(userSummaryId));

    context.assertTrue(userSummary.isPresent());
    assertSummariesAreEqual(updatedUserSummary, userSummary.get(), context);
  }

  @Test
  public void shouldDeleteUserSummary(TestContext context) {
    String userSummaryId1 = randomId();
    String userSummaryId2 = randomId();
    String userSummaryId3 = randomId();

    UserSummary userSummary = createUserSummary(
      userSummaryId2, randomId(), new BigDecimal("3.25"), 3);

    waitFor(CompositeFuture.all(
      repository.saveUserSummary(
        createUserSummary(userSummaryId1, randomId(), new BigDecimal(2), 4)),
      repository.saveUserSummary(userSummary),
      repository.saveUserSummary(
        createUserSummary(userSummaryId3, randomId(), new BigDecimal(4), 2)))
    );

    List<UserSummary> retrievedSummaries =
      waitFor(repository.getUserSummaries(null, 0, 100));

    context.assertEquals(3, retrievedSummaries.size());

    waitFor(CompositeFuture.all(
      repository.deleteUserSummary(userSummaryId1),
      repository.deleteUserSummary(userSummaryId3))
    );

    List<UserSummary> remainingUserSummaries =
      waitFor(repository.getUserSummaries(null, 0, 100));

    context.assertEquals(1, remainingUserSummaries.size());
    assertSummariesAreEqual(userSummary, remainingUserSummaries.get(0), context);
  }

  @Test
  public void shouldUpsertUserSummary(TestContext context) {
    String summaryId = randomId();
    UserSummary initialUserSummary = createUserSummary(summaryId, randomId(), ONE, 1);

    waitFor(repository.upsertUserSummary(initialUserSummary));
    Optional<UserSummary> retrievedInitialSummary =
      waitFor(repository.getUserSummaryById(summaryId));

    context.assertTrue(retrievedInitialSummary.isPresent());
    assertSummariesAreEqual(initialUserSummary, retrievedInitialSummary.get(), context);

    UserSummary updatedSummary = initialUserSummary.withOutstandingFeeFineBalance(TEN);
    waitFor(repository.upsertUserSummary(updatedSummary));

    Optional<UserSummary> retrievedUpdatedSummary =
      waitFor(repository.getUserSummaryById(summaryId));

    context.assertTrue(retrievedUpdatedSummary.isPresent());
    assertSummariesAreEqual(updatedSummary, retrievedUpdatedSummary.get(), context);
  }

  private UserSummary createUserSummary(String id, String userId, BigDecimal balance,
    int lostItems) {

    return new UserSummary()
      .withId(id)
      .withUserId(userId)
      .withNumberOfLostItems(lostItems)
      .withOutstandingFeeFineBalance(balance);
  }

  private void assertSummariesAreEqual(UserSummary expected, UserSummary actual, TestContext ctx) {
    ctx.assertEquals(expected.getId(), actual.getId());
    ctx.assertEquals(expected.getUserId(), actual.getUserId());
    ctx.assertEquals(expected.getNumberOfLostItems(), actual.getNumberOfLostItems());
    ctx.assertEquals(expected.getOutstandingFeeFineBalance(), actual.getOutstandingFeeFineBalance());
    ctx.assertEquals(expected.getOpenFeeFines().size(), actual.getOpenFeeFines().size());
    ctx.assertEquals(expected.getOpenLoans().size(), actual.getOpenLoans().size());
  }

}

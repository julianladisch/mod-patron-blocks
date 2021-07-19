package org.folio.repository;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.util.UuidHelper.randomId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;

public class UserSummaryRepository extends BaseRepository<UserSummary> {
  public static final String USER_SUMMARY_TABLE_NAME = "user_summary";
  private static final String USER_ID_FIELD = "'userId'";
  private static final String OPERATION_EQUALS = "=";
  private static final String FIND_SUMMARY_BY_FEE_FINE_ID_QUERY_TEMPLATE =
    "openFeesFines == \"*\\\"feeFineId\\\": \\\"%s\\\"*\"";

  private static final Logger log = LogManager.getLogger(UserSummaryRepository.class);

  public UserSummaryRepository(PostgresClient pgClient) {
    super(pgClient, USER_SUMMARY_TABLE_NAME, UserSummary.class);
  }

  public Future<String> upsert(UserSummary entity) {
    return super.upsert(entity, entity.getId());
  }

  public Future<String> save(UserSummary entity) {
    return super.save(entity, entity.getId());
  }

  public Future<Boolean> update(UserSummary entity) {
    return super.update(entity, entity.getId())
      .onFailure(throwable -> {
        OptimisticLockingErrorHandlingContext ctx = new OptimisticLockingErrorHandlingContext();
        ctx.optimisticLockingErrors.add(throwable);
        findByUserIdOrBuildNew(entity.getUserId()).onSuccess(userSummary -> {
          processOptimisticLockingError(ctx, userSummary);
        });
      });
  }

  public Future<UserSummary> findByUserIdOrBuildNew(String userId) {
    return getByUserId(userId)
      .map(summary -> summary.orElseGet(() -> buildEmptyUserSummary(userId)));
  }

  public Future<Optional<UserSummary>> findByFeeFineId(String feeFineId) {
    String query = String.format(FIND_SUMMARY_BY_FEE_FINE_ID_QUERY_TEMPLATE, feeFineId);

    return get(query, 0, 1)
      .map(result -> result.stream().findFirst());
  }

  public Future<Optional<UserSummary>> getByUserId(String userId) {
    Criterion criterion = new Criterion(new Criteria()
      .addField(USER_ID_FIELD)
      .setOperation(OPERATION_EQUALS)
      .setVal(userId)
      .setJSONB(true));

    return this.get(criterion)
      .compose(results -> {
        if (results.isEmpty()) {
          return succeededFuture(Optional.empty());
        }

        return succeededFuture(Optional.ofNullable(results.get(0)));
      });
  }

  private UserSummary buildEmptyUserSummary(String userId) {
    return new UserSummary()
      .withId(randomId())
      .withUserId(userId);
  }

  private Future<Boolean> processOptimisticLockingError(OptimisticLockingErrorHandlingContext ctx,
    UserSummary userSummary) {
    Throwable throwable = ctx.optimisticLockingErrors.get(
      ctx.getOptimisticLockingErrors().size() - 1);
    log.error(throwable);
    if (PgExceptionUtil.isVersionConflict(throwable) &&
      ctx.attemptCounter.get() < 10
      //((System.nanoTime() - ctx.attemptStarted) < 1000000000000000011L)
    ) {
      return super.update(userSummary, userSummary.getId())
        .onFailure(error -> {
          ctx.attemptCounter.incrementAndGet();
          ctx.optimisticLockingErrors.add(error);
          findByUserIdOrBuildNew(userSummary.getUserId()).onSuccess(
            userSummary1 -> processOptimisticLockingError(ctx, userSummary1));
        });
    }
      return succeededFuture(false);
    }

    private static class OptimisticLockingErrorHandlingContext {
      private final long attemptStarted;
      private final AtomicInteger attemptCounter = new AtomicInteger(1);
      private final List<Throwable> optimisticLockingErrors = new ArrayList<>();

      public OptimisticLockingErrorHandlingContext() {
        this.attemptStarted = System.nanoTime();
      }

      public List<Throwable> getOptimisticLockingErrors() {
        return optimisticLockingErrors;
      }
    }
  }

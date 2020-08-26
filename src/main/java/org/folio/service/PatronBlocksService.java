package org.folio.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.folio.domain.ActionBlocks;
import org.folio.repository.PatronBlockConditionsRepository;
import org.folio.repository.PatronBlockLimitsRepository;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.jaxrs.model.AutomatedPatronBlock;
import org.folio.rest.jaxrs.model.AutomatedPatronBlocks;
import org.folio.rest.jaxrs.model.OpenLoan;
import org.folio.rest.jaxrs.model.PatronBlockCondition;
import org.folio.rest.jaxrs.model.PatronBlockLimit;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.util.CustomCompositeFuture;
import org.joda.time.DateTime;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

public class PatronBlocksService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DEFAULT_ERROR_MESSAGE = "Failed to calculate automated patron blocks";

  private final UserSummaryRepository userSummaryRepository;
  private final PatronBlockConditionsRepository conditionsRepository;
  private final PatronBlockLimitsRepository limitsRepository;
  private final UsersClient usersClient;
  private final OverduePeriodCalculatorService overduePeriodCalculatorService;
  private final CirculationStorageClient circulationStorageClient;

  public PatronBlocksService(Map<String, String> okapiHeaders, Vertx vertx) {
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(TENANT));
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    userSummaryRepository = new UserSummaryRepository(postgresClient);
    conditionsRepository = new PatronBlockConditionsRepository(postgresClient);
    limitsRepository = new PatronBlockLimitsRepository(postgresClient);
    usersClient = new UsersClient(vertx, okapiHeaders);
    overduePeriodCalculatorService =
      new OverduePeriodCalculatorService(vertx, okapiHeaders);
    circulationStorageClient =
      new CirculationStorageClient(vertx, okapiHeaders);
  }

  public Future<AutomatedPatronBlocks> getBlocksForUser(String userId) {
    return userSummaryRepository.getByUserId(userId)
      .compose(optionalSummary -> optionalSummary
        .map(userSummary -> new BlocksCalculationContext().withUserSummary(userSummary))
        .map(this::getBlocksForSummary)
        .orElseGet(() -> succeededFuture(new AutomatedPatronBlocks())));
  }

  private Future<AutomatedPatronBlocks> getBlocksForSummary(BlocksCalculationContext ctx) {
    return succeededFuture(ctx)
      .compose(this::addUserGroupIdToContext)
      .compose(this::addPatronBlockLimitsToContext)
      .compose(this::addAllPatronBlockConditionsToContext)
      .compose(this::addOverdueMinutesToContext)
      .map(this::calculateBlocks);
  }

  private AutomatedPatronBlocks calculateBlocks(BlocksCalculationContext ctx) {
    final AutomatedPatronBlocks blocks = new AutomatedPatronBlocks();

    if (ctx.patronBlockLimits.isEmpty()) {
      return blocks;
    }

    blocks.getAutomatedPatronBlocks().addAll(ctx.patronBlockLimits.stream()
      .map(ctx::withCurrentPatronBlockLimit)
      .map(this::addCurrentConditionToContext)
      .map(this::addActionBlocksByLimitAndConditionToContext)
      .filter(context -> context.currentActionBlocks.isNotEmpty())
      .map(this::createBlockForLimit)
      .filter(Objects::nonNull)
      .collect(Collectors.toList()));

    return blocks;
  }

  private Future<BlocksCalculationContext> addUserGroupIdToContext(BlocksCalculationContext ctx) {
    if (ctx.userSummary == null || ctx.userSummary.getUserId() == null) {
      logAddingToContextError("user group ID");
      return failedFuture(DEFAULT_ERROR_MESSAGE);
    }

    return usersClient.findPatronGroupIdForUser(ctx.userSummary.getUserId())
      .map(ctx::withUserGroupId);
  }

  private Future<BlocksCalculationContext> addPatronBlockLimitsToContext(
    BlocksCalculationContext ctx) {

    if (ctx.userGroupId == null) {
      logAddingToContextError("patron block limits");
      return failedFuture(DEFAULT_ERROR_MESSAGE);
    }

    return limitsRepository.findLimitsForPatronGroup(ctx.userGroupId)
      .map(ctx::withPatronBlockLimits);
  }

  private Future<BlocksCalculationContext> addAllPatronBlockConditionsToContext(
    BlocksCalculationContext ctx) {

    return conditionsRepository.getAllWithDefaultLimit().map(ctx::withPatronBlockConditions);
  }

  private Future<BlocksCalculationContext> addOverdueMinutesToContext(
    BlocksCalculationContext ctx) {

    List<Future<LoanOverdueMinutes>> overdueMinutesFutures = new ArrayList<>();

    Set<String> uniqueIds = ctx.userSummary.getOpenLoans().stream()
      .map(OpenLoan::getLoanId)
      .collect(Collectors.toSet());

    ctx.userSummary.getOpenLoans().forEach(openLoan ->
      overdueMinutesFutures.add(
        circulationStorageClient.findLoanById(openLoan.getLoanId())
          .compose(loan -> overduePeriodCalculatorService.getMinutes(loan, DateTime.now()))
          .map(intValue -> new LoanOverdueMinutes(openLoan.getLoanId(), intValue))
      ));

    Future<BlocksCalculationContext> result = CustomCompositeFuture.all(overdueMinutesFutures)
      .map(ar -> {
        Map<String, Integer> overdueMinutes = ar.list().stream()
          .filter(LoanOverdueMinutes.class::isInstance)
          .map(LoanOverdueMinutes.class::cast)
          .collect(Collectors.toMap(r -> r.loanId, r -> r.overdueMinutes, (key, value) -> key));

        return ctx.withOverdueMinutes(overdueMinutes);
      })
      .onFailure(throwable -> logAddingToContextError("overdue minutes", throwable.getMessage()));

    if (result.failed()) {
      return failedFuture(DEFAULT_ERROR_MESSAGE);
    }
    else {
      return result;
    }
  }

  private BlocksCalculationContext addCurrentConditionToContext(
    BlocksCalculationContext ctx) {

    if (ctx.currentPatronBlockLimit == null ||
      ctx.currentPatronBlockLimit.getConditionId() == null) {

      logAddingToContextError("current condition");
      return ctx;
    }

    String conditionId = ctx.currentPatronBlockLimit.getConditionId();

    PatronBlockCondition patronBlockCondition = ctx.patronBlockConditions.stream()
      .filter(condition -> condition.getId().equals(conditionId))
      .findFirst()
      .orElse(null);

    if (patronBlockCondition == null) {
      logAddingToContextError("current condition",
        format("cannot find condition by ID %s", conditionId));
      return ctx;
    }

    return ctx.withCurrentPatronBlockCondition(patronBlockCondition);
  }

  private BlocksCalculationContext addActionBlocksByLimitAndConditionToContext(
    BlocksCalculationContext ctx) {

    if (ctx.userSummary == null || ctx.currentPatronBlockLimit == null ||
      ctx.currentPatronBlockCondition == null || ctx.overdueMinutes == null) {

      logAddingToContextError("action blocks by limit and condition");
      return ctx;
    }

    PatronBlockCondition patronBlockCondition = ctx.currentPatronBlockCondition;

    ActionBlocks actionBlocksByLimit = ActionBlocks.byLimit(ctx.userSummary,
      ctx.currentPatronBlockLimit, ctx.overdueMinutes);

    ActionBlocks actionBlocksByCondition = new ActionBlocks(
      Boolean.TRUE.equals(patronBlockCondition.getBlockBorrowing()),
      Boolean.TRUE.equals(patronBlockCondition.getBlockRenewals()),
      Boolean.TRUE.equals(patronBlockCondition.getBlockRequests()));

    return ctx.withCurrentActionBlocks(
      ActionBlocks.and(actionBlocksByLimit, actionBlocksByCondition));
  }

  private AutomatedPatronBlock createBlockForLimit(BlocksCalculationContext ctx) {
    if (ctx.currentPatronBlockCondition == null || ctx.currentActionBlocks == null) {
      log.error("Failed to create automated patron block - context is invalid");
      return null;
    }

    return new AutomatedPatronBlock()
      .withPatronBlockConditionId(ctx.currentPatronBlockCondition.getId())
      .withBlockBorrowing(ctx.currentActionBlocks.getBlockBorrowing())
      .withBlockRenewals(ctx.currentActionBlocks.getBlockRenewals())
      .withBlockRequests(ctx.currentActionBlocks.getBlockRequests())
      .withMessage(ctx.currentPatronBlockCondition.getMessage());
  }

  private void logAddingToContextError(String parameterName, String additionalMessage) {
    String message = format("Blocks calculation context is invalid, failed to add %s",
      parameterName);

    if (additionalMessage != null) {
      message += format(". %s", additionalMessage);
    }

    log.error(message);
  }

  private void logAddingToContextError(String parameterName) {
    logAddingToContextError(parameterName, null);
  }

  @With
  @AllArgsConstructor
  @NoArgsConstructor(force = true)
  private static class BlocksCalculationContext {
    final UserSummary userSummary;
    final String userGroupId;
    final List<PatronBlockLimit> patronBlockLimits;
    final List<PatronBlockCondition> patronBlockConditions;
    final Map<String, Integer> overdueMinutes;
    final PatronBlockLimit currentPatronBlockLimit;
    final PatronBlockCondition currentPatronBlockCondition;
    final ActionBlocks currentActionBlocks;
  }

  @AllArgsConstructor
  private static class LoanOverdueMinutes {
    final String loanId;
    final Integer overdueMinutes;
  }
}

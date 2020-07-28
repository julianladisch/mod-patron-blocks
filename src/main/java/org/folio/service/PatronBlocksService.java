package org.folio.service;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.domain.ActionBlocks;
import org.folio.domain.Condition;
import org.folio.exception.EntityNotFoundException;
import org.folio.repository.PatronBlockConditionsRepository;
import org.folio.repository.PatronBlockLimitsRepository;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.client.UsersClient;
import org.folio.rest.jaxrs.model.AutomatedPatronBlock;
import org.folio.rest.jaxrs.model.AutomatedPatronBlocks;
import org.folio.rest.jaxrs.model.PatronBlockCondition;
import org.folio.rest.jaxrs.model.PatronBlockLimit;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class PatronBlocksService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final UserSummaryRepository userSummaryRepository;
  private final PatronBlockConditionsRepository conditionsRepository;
  private final PatronBlockLimitsRepository limitsRepository;
  private final UsersClient usersClient;

  public PatronBlocksService(Map<String, String> okapiHeaders, Vertx vertx) {
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(TENANT));
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    userSummaryRepository = new UserSummaryRepository(postgresClient);
    conditionsRepository = new PatronBlockConditionsRepository(postgresClient);
    limitsRepository = new PatronBlockLimitsRepository(postgresClient);
    usersClient = new UsersClient(vertx, okapiHeaders);
  }

  public Future<AutomatedPatronBlocks> getBlocksForUser(String userId) {
    return userSummaryRepository.getByUserId(userId)
      .compose(optionalSummary -> optionalSummary
        .map(this::getBlocksForSummary)
        .orElseGet(() -> succeededFuture(new AutomatedPatronBlocks())));
  }

  private Future<AutomatedPatronBlocks> getBlocksForSummary(UserSummary summary) {
    return succeededFuture(summary.getUserId())
      .compose(usersClient::findPatronGroupIdForUser)
      .compose(limitsRepository::findLimitsForPatronGroup)
      .compose(limits -> calculateBlocks(summary, limits));
  }

  private Future<AutomatedPatronBlocks> calculateBlocks(UserSummary userSummary,
    List<PatronBlockLimit> limits) {

    final AutomatedPatronBlocks blocks = new AutomatedPatronBlocks();

    if (limits.isEmpty()) {
      return succeededFuture(blocks);
    }

    List<Future<AutomatedPatronBlock>> futures = limits.stream()
      .map(limit -> createBlockForLimit(userSummary, limit))
      .collect(Collectors.toList());

    return CompositeFuture.all(new ArrayList<>(futures))
      .onFailure(log::error)
      .onSuccess(cf -> blocks.getAutomatedPatronBlocks().addAll(
        Arrays.stream(Condition.values())
        .map(condition -> cf.list().stream()
          .map(AutomatedPatronBlock.class::cast)
          .filter(Objects::nonNull)
          .filter(e -> e.getPatronBlockConditionId().equals(condition.getId()))
          .findFirst()
          .orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList())))
      .map(blocks);
  }

  private Future<AutomatedPatronBlock> createBlockForLimit(UserSummary userSummary,
    PatronBlockLimit limit) {

    ActionBlocks actionBlocks = ActionBlocks.determineBlocks(userSummary, limit);

    return succeededFuture(limit)
      .compose(this::findConditionForLimit)
      .map(condition -> createBlockForCondition(condition, actionBlocks));
  }

  private Future<PatronBlockCondition> findConditionForLimit(PatronBlockLimit limit) {
    final String conditionId = limit.getConditionId();

    return conditionsRepository.get(conditionId)
      .map(optional -> optional.orElseThrow(() ->
        new EntityNotFoundException(format(
          "Condition %s referenced by limit %s does not exist", conditionId, limit.getId()))));
  }

  private AutomatedPatronBlock createBlockForCondition(PatronBlockCondition condition,
    ActionBlocks actionBlocks) {

    boolean blockBorrowing = Boolean.TRUE.equals(condition.getBlockBorrowing())
      && actionBlocks.getBlockBorrowing();
    boolean blockRenewals = Boolean.TRUE.equals(condition.getBlockRenewals())
      && actionBlocks.getBlockRenewals();
    boolean blockRequests = Boolean.TRUE.equals(condition.getBlockRequests())
      && actionBlocks.getBlockRequests();

    if (blockBorrowing || blockRenewals || blockRequests) {
      return new AutomatedPatronBlock()
        .withPatronBlockConditionId(condition.getId())
        .withBlockBorrowing(blockBorrowing)
        .withBlockRenewals(blockRenewals)
        .withBlockRequests(blockRequests)
        .withMessage(condition.getMessage());
    }

    return null;
  }

}

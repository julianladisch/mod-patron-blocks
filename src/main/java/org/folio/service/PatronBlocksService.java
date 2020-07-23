package org.folio.service;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.domain.Condition.isConditionLimitExceeded;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

  private Future<AutomatedPatronBlocks> calculateBlocks(UserSummary summary,
    List<PatronBlockLimit> limits) {

    final AutomatedPatronBlocks blocks = new AutomatedPatronBlocks();

    if (limits.isEmpty()) {
      return succeededFuture(blocks);
    }

    final List<Future<PatronBlockCondition>> findConditions = limits.stream()
      .filter(limit -> isConditionLimitExceeded(summary, limit))
      .map(this::findConditionForLimit)
      .collect(toList());

    return CompositeFuture.all(new ArrayList<>(findConditions))
      .onFailure(log::error)
      .onSuccess(ignored -> createBlocksForActiveConditions(findConditions, blocks))
      .map(blocks);
  }

  private void createBlocksForActiveConditions(
    List<Future<PatronBlockCondition>> findConditionFutures, AutomatedPatronBlocks blocks) {

    blocks.withAutomatedPatronBlocks(
      findConditionFutures.stream()
        .filter(Future::succeeded)
        .map(Future::result)
        .filter(this::isConditionEnabled)
        .map(this::createBlockForCondition)
        .collect(toList())
    );
  }

  private Future<PatronBlockCondition> findConditionForLimit(PatronBlockLimit limit) {
    final String conditionId = limit.getConditionId();

    return conditionsRepository.get(conditionId)
      .map(optional -> optional.orElseThrow(() ->
        new EntityNotFoundException(format(
          "Condition %s referenced by limit %s does not exist", conditionId, limit.getId()))));
  }

  private AutomatedPatronBlock createBlockForCondition(PatronBlockCondition condition) {
    return new AutomatedPatronBlock()
      .withPatronBlockConditionId(condition.getId())
      .withBlockBorrowing(condition.getBlockBorrowing())
      .withBlockRenewals(condition.getBlockRenewals())
      .withBlockRequests(condition.getBlockRequests())
      .withMessage(condition.getMessage());
  }

  private boolean isConditionEnabled(PatronBlockCondition condition) {
    return isTrue(condition.getBlockBorrowing())
      || isTrue(condition.getBlockRenewals())
      || isTrue(condition.getBlockRequests());
  }

}

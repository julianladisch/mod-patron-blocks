package org.folio.service;

import static java.lang.String.format;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;

import java.util.Map;

import org.folio.exception.EntityNotFoundInDbException;
import org.folio.repository.UserSummaryRepository;
import org.folio.rest.jaxrs.model.UserSummary;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class UserSummaryService {
  private final UserSummaryRepository userSummaryRepository;

  public UserSummaryService(Map<String, String> okapiHeaders, Vertx vertx) {
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(TENANT));
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    userSummaryRepository = new UserSummaryRepository(postgresClient);
  }

  public Future<UserSummary> getByUserId(String userId) {
    return userSummaryRepository.getByUserId(userId)
      .map(optionalUserSummary -> optionalUserSummary.orElseThrow(() ->
        new EntityNotFoundInDbException(format("User summary for user ID %s not found", userId))));
  }
}

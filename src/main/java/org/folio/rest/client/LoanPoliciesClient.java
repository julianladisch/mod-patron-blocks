package org.folio.rest.client;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.folio.rest.jaxrs.model.LoanPolicy;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class LoanPoliciesClient extends OkapiClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public LoanPoliciesClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(WebClient.create(vertx), okapiHeaders);
  }

  public Future<LoanPolicy> findLoanPolicyById(String loanPolicyId) {
    return fetchById("loan-policy-storage/loan-policies", loanPolicyId, LoanPolicy.class)
      .compose(Future::succeededFuture);
  }
}

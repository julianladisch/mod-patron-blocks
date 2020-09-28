package org.folio.rest.client;

import static org.folio.rest.client.WebClientProvider.getWebClient;

import java.util.Map;

import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class CirculationStorageClient extends OkapiClient {

  public CirculationStorageClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(getWebClient(vertx), okapiHeaders);
  }

  public Future<Loan> findLoanById(String loanId) {
    return fetchById("loan-storage/loans", loanId, Loan.class);
  }

  public Future<LoanPolicy> findLoanPolicyById(String loanPolicyId) {
    return fetchById("loan-policy-storage/loan-policies", loanPolicyId, LoanPolicy.class);
  }
}

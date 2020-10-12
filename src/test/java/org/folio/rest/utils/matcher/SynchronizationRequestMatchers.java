package org.folio.rest.utils.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;

import java.util.Arrays;

import org.hamcrest.Matcher;

import io.vertx.core.json.JsonObject;

public class SynchronizationRequestMatchers {
  private SynchronizationRequestMatchers() { }

  public static Matcher<JsonObject> newSynchronizationRequestFull(String id) {
    return synchronizationRequest(id, "full", null, "open", 0, 0, 0, 0);
  }

  public static Matcher<JsonObject> newSynchronizationRequestByUser(String id, String userId) {
    return synchronizationRequest(id, "user", userId, "open", 0, 0, 0, 0);
  }

  public static Matcher<JsonObject> synchronizationRequest(String id, String scope, String userId,
    String status, long totalNumberOfLoans, long totalNumberOfFeesFines,
    long numberOfProcessedLoans, long numberOfProcessedFeesFines) {

    return allOf(Arrays.asList(
      hasJsonPath("id", is(id)),
      hasJsonPath("scope", is(scope)),
      userId == null ? hasNoJsonPath("userId") : hasJsonPath("userId", is(userId)),
      hasJsonPath("status", is(status)),
      hasJsonPath("totalNumberOfLoans", is((double) totalNumberOfLoans)),
      hasJsonPath("totalNumberOfFeesFines", is((double) totalNumberOfFeesFines)),
      hasJsonPath("numberOfProcessedLoans", is((double) numberOfProcessedLoans)),
      hasJsonPath("numberOfProcessedFeesFines", is((double) numberOfProcessedFeesFines))
    ));
  }
}

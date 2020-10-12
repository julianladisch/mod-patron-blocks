package org.folio.rest.utils.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;

import java.util.Arrays;

import org.folio.rest.jaxrs.model.SynchronizationJob;
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
    String status, int totalNumberOfLoans, int totalNumberOfFeesFines,
    int numberOfProcessedLoans, int numberOfProcessedFeesFines) {

    return allOf(Arrays.asList(
      hasJsonPath("id", is(id)),
      hasJsonPath("scope", is(scope)),
      userId == null ? hasNoJsonPath("userId") : hasJsonPath("userId", is(userId)),
      hasJsonPath("status", is(status)),
      hasJsonPath("totalNumberOfLoans", is(totalNumberOfLoans)),
      hasJsonPath("totalNumberOfFeesFines", is(totalNumberOfFeesFines)),
      hasJsonPath("numberOfProcessedLoans", is(numberOfProcessedLoans)),
      hasJsonPath("numberOfProcessedFeesFines", is(numberOfProcessedFeesFines))
    ));
  }

  public static Matcher<SynchronizationJob> synchronizationJobMatcher(String status,
    int totalNumberOfLoans, int totalNumberOfFeesFines, int numberOfProcessedLoans,
    int numberOfProcessedFeesFines) {

    return allOf(Arrays.asList(
      hasProperty("status", is(status)),
      hasProperty("totalNumberOfLoans", is(totalNumberOfLoans)),
      hasProperty("totalNumberOfFeesFines", is(totalNumberOfFeesFines)),
      hasProperty("numberOfProcessedLoans", is(numberOfProcessedLoans)),
      hasProperty("numberOfProcessedFeesFines", is(numberOfProcessedFeesFines))
    ));
  }
}

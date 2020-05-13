package org.folio.patronblocks.repository;

import java.util.List;
import java.util.Optional;

import org.folio.rest.jaxrs.model.UserSummary;

import io.vertx.core.Future;

/**
 * Repository for UserSummary
 */
public interface UserSummaryRepository {

  /**
   * Searches for UserSummaries in database
   *
   * @param query CQL query
   * @param offset offset
   * @param limit limit
   * @return future with list of UserSummary
   */
  Future<List<UserSummary>> getUserSummaries(String query, int offset, int limit);

  /**
   * Searches for UserSummary by id
   *
   * @param id UserSummary id
   * @return future with UserSummary
   */
  Future<Optional<UserSummary>> getUserSummaryById(String id);

  /**
   * Saves UserSummary to database
   *
   * @param userSummary UserSummary to save
   * @return future with id of saved UserSummary
   */
  Future<String> saveUserSummary(UserSummary userSummary);

  /**
   * Updates UserSummary in database
   *
   * @param userSummary UserSummary to update
   * @return future with true if succeeded
   */
  Future<Boolean> updateUserSummary(UserSummary userSummary);

  /**
   * Deletes UserSummary from database
   *
   * @param id UserSummary id to delete
   * @return future with true is succeeded
   */
  Future<Boolean> deleteUserSummary(String id);
}

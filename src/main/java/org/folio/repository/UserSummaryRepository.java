package org.folio.repository;

import java.util.List;
import java.util.Optional;


import org.folio.domain.UserSummary;

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
   * Searches for UserSummary by user id
   *
   * @param userId user id
   * @return future with optional UserSummary
   */
  Future<Optional<UserSummary>> getUserSummaryByUserId(String userId);

  /**
   * Saves UserSummary to database
   *
   * @param userSummary UserSummary to save
   * @return future with id of saved UserSummary
   */
  Future<String> saveUserSummary(UserSummary userSummary);

  /**
   * Update UserSummary if it already exists in the DB, insert otherwise
   *
   * @param userSummary UserSummary to update or insert
   * @return future with id of saved UserSummary
   */
  Future<String> upsertUserSummary(UserSummary userSummary);

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

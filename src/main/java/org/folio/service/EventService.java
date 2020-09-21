package org.folio.service;

import java.util.List;

import org.folio.domain.EventType;
import org.folio.repository.BaseRepository;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.ItemCheckedInEvent;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.UuidHelper;

import io.vertx.core.Future;

public class EventService {
  private static final int NUMBER_OF_EVENTS_LIMIT = 10000;

  private final BaseRepository<ItemCheckedOutEvent> itemCheckedOutEventRepository;
  private final BaseRepository<ItemCheckedInEvent> itemCheckedInEventRepository;
  private final BaseRepository<ItemClaimedReturnedEvent> itemClaimedReturnedEventRepository;
  private final BaseRepository<ItemDeclaredLostEvent> itemDeclaredLostEventRepository;
  private final BaseRepository<LoanDueDateChangedEvent> loanDueDateChangedEventRepository;
  private final BaseRepository<FeeFineBalanceChangedEvent> feeFineBalanceChangedEventRepository;

  public EventService(PostgresClient postgresClient) {
    itemCheckedOutEventRepository = new BaseRepository<>(postgresClient,
      EventType.ITEM_CHECKED_OUT.getTableName(), ItemCheckedOutEvent.class);

    itemCheckedInEventRepository = new BaseRepository<>(postgresClient,
      EventType.ITEM_CHECKED_IN.getTableName(), ItemCheckedInEvent.class);

    itemClaimedReturnedEventRepository = new BaseRepository<>(postgresClient,
      EventType.ITEM_CLAIMED_RETURNED.getTableName(), ItemClaimedReturnedEvent.class);

    itemDeclaredLostEventRepository = new BaseRepository<>(postgresClient,
      EventType.ITEM_DECLARED_LOST.getTableName(), ItemDeclaredLostEvent.class);

    loanDueDateChangedEventRepository = new BaseRepository<>(postgresClient,
      EventType.LOAN_DUE_DATE_CHANGED.getTableName(), LoanDueDateChangedEvent.class);

    feeFineBalanceChangedEventRepository = new BaseRepository<>(postgresClient,
      EventType.FEE_FINE_BALANCE_CHANGED.getTableName(), FeeFineBalanceChangedEvent.class);
  }

  public Future<String> save(ItemCheckedOutEvent event) {
    return itemCheckedOutEventRepository.save(event, UuidHelper.randomId());
  }

  public Future<String> save(ItemCheckedInEvent event) {
    return itemCheckedInEventRepository.save(event, UuidHelper.randomId());
  }

  public Future<String> save(ItemClaimedReturnedEvent event) {
    return itemClaimedReturnedEventRepository.save(event, UuidHelper.randomId());
  }

  public Future<String> save(ItemDeclaredLostEvent event) {
    return itemDeclaredLostEventRepository.save(event, UuidHelper.randomId());
  }

  public Future<String> save(LoanDueDateChangedEvent event) {
    return loanDueDateChangedEventRepository.save(event, UuidHelper.randomId());
  }

  public Future<String> save(FeeFineBalanceChangedEvent event) {
    return feeFineBalanceChangedEventRepository.save(event, UuidHelper.randomId());
  }

  public Future<List<ItemCheckedOutEvent>> getItemCheckedOutEvents(String userId) {
    return itemCheckedOutEventRepository.get(buildFilterEventsByUserIdCriterion(userId));
  }

  public Future<List<ItemCheckedInEvent>> getItemCheckedInEvents(String userId) {
    return itemCheckedInEventRepository.get(buildFilterEventsByUserIdCriterion(userId));
  }

  public Future<List<ItemClaimedReturnedEvent>> getItemClaimedReturnedEvents(String userId) {
    return itemClaimedReturnedEventRepository.get(buildFilterEventsByUserIdCriterion(userId));
  }

  public Future<List<ItemDeclaredLostEvent>> getItemDeclaredLostEvents(String userId) {
    return itemDeclaredLostEventRepository.get(buildFilterEventsByUserIdCriterion(userId));
  }

  public Future<List<LoanDueDateChangedEvent>> getLoanDueDateChangedEvents(String userId) {
    return loanDueDateChangedEventRepository.get(buildFilterEventsByUserIdCriterion(userId));
  }

  public Future<List<FeeFineBalanceChangedEvent>> getFeeFineBalanceChangedEvents(String userId) {
    return feeFineBalanceChangedEventRepository.get(buildFilterEventsByUserIdCriterion(userId));
  }

  private Criterion buildFilterEventsByUserIdCriterion(String userId) {
    return new Criterion(new Criteria()
      .addField("'userId'")
      .setOperation("=")
      .setVal(userId)
      .setJSONB(true)
    ).setLimit(new Limit(NUMBER_OF_EVENTS_LIMIT));
  }
}

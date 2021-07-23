package org.folio.service;

import java.util.List;
import java.util.function.Function;

import org.folio.domain.Event;
import org.folio.domain.EventType;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.repository.EventRepository;
import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.ItemAgedToLostEvent;
import org.folio.rest.jaxrs.model.ItemCheckedInEvent;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.UuidHelper;

import io.vertx.core.Future;

public class EventService {

  private static final String ITEM_CHECKED_OUT_EVENT_TABLE_NAME = "item_checked_out_event";
  private static final String ITEM_CHECKED_IN_EVENT_TABLE_NAME = "item_checked_in_event";
  private static final String ITEM_DECLARED_LOST_EVENT_TABLE_NAME = "item_declared_lost_event";
  private static final String ITEM_AGED_TO_LOST_EVENT_TABLE_NAME = "item_aged_to_lost_event";
  private static final String ITEM_CLAIMED_RETURNED_EVENT_TABLE_NAME = "item_claimed_returned_event";
  private static final String LOAN_DUE_DATE_CHANGED_EVENT_TABLE_NAME = "loan_due_date_changed_event";
  private static final String FEE_FINE_BALANCE_CHANGED_EVENT_TABLE_NAME = "fee_fine_balance_changed_event";

  private final EventRepository<ItemCheckedOutEvent> itemCheckedOutEventRepository;
  private final EventRepository<ItemCheckedInEvent> itemCheckedInEventRepository;
  private final EventRepository<ItemClaimedReturnedEvent> itemClaimedReturnedEventRepository;
  private final EventRepository<ItemDeclaredLostEvent> itemDeclaredLostEventRepository;
  private final EventRepository<ItemAgedToLostEvent> itemAgedToLostEventEventRepository;
  private final EventRepository<LoanDueDateChangedEvent> loanDueDateChangedEventRepository;
  private final EventRepository<FeeFineBalanceChangedEvent> feeFineBalanceChangedEventRepository;

  public EventService(PostgresClient postgresClient) {
    itemCheckedOutEventRepository = new EventRepository<>(postgresClient,
      ITEM_CHECKED_OUT_EVENT_TABLE_NAME, ItemCheckedOutEvent.class);

    itemCheckedInEventRepository = new EventRepository<>(postgresClient,
      ITEM_CHECKED_IN_EVENT_TABLE_NAME, ItemCheckedInEvent.class);

    itemClaimedReturnedEventRepository = new EventRepository<>(postgresClient,
      ITEM_CLAIMED_RETURNED_EVENT_TABLE_NAME, ItemClaimedReturnedEvent.class);

    itemDeclaredLostEventRepository = new EventRepository<>(postgresClient,
      ITEM_DECLARED_LOST_EVENT_TABLE_NAME, ItemDeclaredLostEvent.class);

    itemAgedToLostEventEventRepository = new EventRepository<>(postgresClient,
      ITEM_AGED_TO_LOST_EVENT_TABLE_NAME, ItemAgedToLostEvent.class);

    loanDueDateChangedEventRepository = new EventRepository<>(postgresClient,
      LOAN_DUE_DATE_CHANGED_EVENT_TABLE_NAME, LoanDueDateChangedEvent.class);

    feeFineBalanceChangedEventRepository = new EventRepository<>(postgresClient,
      FEE_FINE_BALANCE_CHANGED_EVENT_TABLE_NAME, FeeFineBalanceChangedEvent.class);
  }

  public Future<String> save(Event event) {
    EventType eventType = EventType.getByEvent(event);

    Future<String> save;
    switch (eventType) {
    case ITEM_CHECKED_OUT:
      save = save((ItemCheckedOutEvent) event);
      break;
    case ITEM_CHECKED_IN:
      save = save((ItemCheckedInEvent) event);
      break;
    case ITEM_CLAIMED_RETURNED:
      save = save((ItemClaimedReturnedEvent) event);
      break;
    case ITEM_DECLARED_LOST:
      save = save((ItemDeclaredLostEvent) event);
      break;
    case ITEM_AGED_TO_LOST:
      save = save((ItemAgedToLostEvent) event);
      break;
    case LOAN_DUE_DATE_CHANGED:
      save = save((LoanDueDateChangedEvent) event);
      break;
    case FEE_FINE_BALANCE_CHANGED:
      save = save((FeeFineBalanceChangedEvent) event);
      break;
    default:
      throw new IllegalStateException("Unexpected value: " + eventType);
    }
    return save;
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

  public Future<String> save(ItemAgedToLostEvent event) {
    return itemAgedToLostEventEventRepository.save(event, UuidHelper.randomId());
  }

  public Future<String> save(LoanDueDateChangedEvent event) {
    return loanDueDateChangedEventRepository.save(event, UuidHelper.randomId());
  }

  public Future<String> save(FeeFineBalanceChangedEvent event) {
    return feeFineBalanceChangedEventRepository.save(event, UuidHelper.randomId());
  }

  public Future<List<ItemCheckedOutEvent>> getItemCheckedOutEvents(String userId) {
    return itemCheckedOutEventRepository.getByUserId(userId);
  }

  public Future<List<ItemCheckedInEvent>> getItemCheckedInEvents(String userId) {
    return itemCheckedInEventRepository.getByUserId(userId);
  }

  public Future<List<ItemClaimedReturnedEvent>> getItemClaimedReturnedEvents(String userId) {
    return itemClaimedReturnedEventRepository.getByUserId(userId);
  }

  public Future<List<ItemDeclaredLostEvent>> getItemDeclaredLostEvents(String userId) {
    return itemDeclaredLostEventRepository.getByUserId(userId);
  }

  public Future<List<ItemAgedToLostEvent>> getItemAgedToLostEvents(String userId) {
    return itemAgedToLostEventEventRepository.getByUserId(userId);
  }

  public Future<List<LoanDueDateChangedEvent>> getLoanDueDateChangedEvents(String userId) {
    return loanDueDateChangedEventRepository.getByUserId(userId);
  }

  public Future<List<FeeFineBalanceChangedEvent>> getFeeFineBalanceChangedEvents(String userId) {
    return feeFineBalanceChangedEventRepository.getByUserId(userId);
  }

  public Future<Void> removeAllEvents(String tenantId) {
    return GenericCompositeFuture.all(List.of(itemCheckedOutEventRepository.removeAll(tenantId),
      itemCheckedInEventRepository.removeAll(tenantId),
      itemClaimedReturnedEventRepository.removeAll(tenantId),
      itemDeclaredLostEventRepository.removeAll(tenantId),
      loanDueDateChangedEventRepository.removeAll(tenantId),
      feeFineBalanceChangedEventRepository.removeAll(tenantId))
    ).mapEmpty();
  }

  public Future<Void> removeAllEventsForUser(String tenantId, String userId) {
    return GenericCompositeFuture.all(
      List.of(itemCheckedOutEventRepository.removeByUserId(tenantId, userId),
        itemCheckedInEventRepository.removeByUserId(tenantId, userId),
        itemClaimedReturnedEventRepository.removeByUserId(tenantId, userId),
        itemDeclaredLostEventRepository.removeByUserId(tenantId, userId),
        loanDueDateChangedEventRepository.removeByUserId(tenantId, userId),
        feeFineBalanceChangedEventRepository.removeByUserId(tenantId, userId))
    ).mapEmpty();
  }
}

package org.folio.domain;

import java.util.HashMap;
import java.util.Map;

import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.ItemAgedToLostEvent;
import org.folio.rest.jaxrs.model.ItemCheckedInEvent;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;
import org.folio.util.EventProcessingResultAdapter;

public enum EventType {
  FEE_FINE_BALANCE_CHANGED(FeeFineBalanceChangedEvent.class,
    EventProcessingResultAdapter.FEE_FINE_BALANCE_CHANGED),
  ITEM_CHECKED_OUT(ItemCheckedOutEvent.class, EventProcessingResultAdapter.ITEM_CHECKED_OUT),
  ITEM_CHECKED_IN(ItemCheckedInEvent.class, EventProcessingResultAdapter.ITEM_CHECKED_IN),
  ITEM_DECLARED_LOST(ItemDeclaredLostEvent.class, EventProcessingResultAdapter.ITEM_DECLARED_LOST),
  ITEM_AGED_TO_LOST(ItemAgedToLostEvent.class, EventProcessingResultAdapter.ITEM_AGED_TO_LOST),
  ITEM_CLAIMED_RETURNED(ItemClaimedReturnedEvent.class,
    EventProcessingResultAdapter.ITEM_CLAIMED_RETURNED),
  LOAN_DUE_DATE_CHANGED(LoanDueDateChangedEvent.class,
    EventProcessingResultAdapter.LOAN_DUE_DATE_CHANGED),
  UNKNOWN(null, null);

  private static final Map<Class<? extends Event>, EventType> eventToType;

  static {
    eventToType = new HashMap<>(values().length);
    for (EventType eventType : values()) {
      eventToType.put(eventType.getEventClass(), eventType);
    }
  }

  private final Class<? extends Event> eventClass;

  private final EventProcessingResultAdapter eventProcessingResultAdapter;

  EventType(Class<? extends Event> eventClass,
    EventProcessingResultAdapter eventProcessingResultAdapter) {
    this.eventClass = eventClass;
    this.eventProcessingResultAdapter = eventProcessingResultAdapter;
  }

  public EventProcessingResultAdapter getEventProcessingResultAdapter() {
    return eventProcessingResultAdapter;
  }

  public Class<? extends Event> getEventClass() {
    return eventClass;
  }

  public static EventType getByEvent(Object event) {
    return eventToType.getOrDefault(event.getClass(), UNKNOWN);
  }

  public static String getNameByEvent(Event event) {
    return getByEvent(event).name();
  }

}

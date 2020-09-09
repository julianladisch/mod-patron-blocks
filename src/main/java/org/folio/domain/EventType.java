package org.folio.domain;

import java.util.HashMap;
import java.util.Map;

import org.folio.rest.jaxrs.model.FeeFineBalanceChangedEvent;
import org.folio.rest.jaxrs.model.ItemCheckedInEvent;
import org.folio.rest.jaxrs.model.ItemCheckedOutEvent;
import org.folio.rest.jaxrs.model.ItemClaimedReturnedEvent;
import org.folio.rest.jaxrs.model.ItemDeclaredLostEvent;
import org.folio.rest.jaxrs.model.LoanDueDateChangedEvent;

public enum EventType {
  FEE_FINE_BALANCE_CHANGED(FeeFineBalanceChangedEvent.class, "fee_fine_balance_changed_event"),
  ITEM_CHECKED_OUT(ItemCheckedOutEvent.class, "item_checked_out_event"),
  ITEM_CHECKED_IN(ItemCheckedInEvent.class, "item_checked_in_event"),
  ITEM_DECLARED_LOST(ItemDeclaredLostEvent.class, "item_declared_lost_event"),
  ITEM_CLAIMED_RETURNED(ItemClaimedReturnedEvent.class, "item_claimed_returned_event"),
  LOAN_DUE_DATE_CHANGED(LoanDueDateChangedEvent.class, "loan_due_date_changed_event"),
  UNKNOWN(null, null);

  private static final Map<Class<? extends Event>, EventType> eventToType;
  static {
    eventToType = new HashMap<>(values().length);
    for (EventType eventType : values()) {
      eventToType.put(eventType.getEventClass(), eventType);
    }
  }

  private final Class<? extends Event> eventClass;
  private final String tableName;

  EventType(Class<? extends Event> eventClass, String tableName) {
    this.eventClass = eventClass;
    this.tableName = tableName;
  }

  public Class<? extends Event> getEventClass() {
    return eventClass;
  }

  public String getTableName() {
    return tableName;
  }

  public static EventType getByEvent(Object event) {
    return eventToType.getOrDefault(event.getClass(), UNKNOWN);
  }

  public static String getNameByEvent(Event event) {
    return getByEvent(event).name();
  }

}

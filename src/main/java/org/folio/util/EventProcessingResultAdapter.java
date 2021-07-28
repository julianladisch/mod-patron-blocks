package org.folio.util;

import static org.folio.rest.jaxrs.resource.AutomatedPatronBlocksHandlers.*;

import java.util.function.Function;
import java.util.function.Supplier;

import org.folio.rest.jaxrs.resource.support.ResponseDelegate;

public enum EventProcessingResultAdapter {
  FEE_FINE_BALANCE_CHANGED(
    PostAutomatedPatronBlocksHandlersFeeFineBalanceChangedResponse::respond204,
    PostAutomatedPatronBlocksHandlersFeeFineBalanceChangedResponse::respond409WithTextPlain,
    PostAutomatedPatronBlocksHandlersFeeFineBalanceChangedResponse::respond500WithTextPlain
  ),

  ITEM_CHECKED_OUT(
    PostAutomatedPatronBlocksHandlersItemCheckedOutResponse::respond204,
    PostAutomatedPatronBlocksHandlersItemCheckedOutResponse::respond409WithTextPlain,
    PostAutomatedPatronBlocksHandlersItemCheckedOutResponse::respond500WithTextPlain
  ),

  ITEM_CHECKED_IN(
    PostAutomatedPatronBlocksHandlersItemCheckedInResponse::respond204,
    PostAutomatedPatronBlocksHandlersItemCheckedInResponse::respond409WithTextPlain,
    PostAutomatedPatronBlocksHandlersItemCheckedInResponse::respond500WithTextPlain
  ),

  ITEM_DECLARED_LOST(
    PostAutomatedPatronBlocksHandlersItemDeclaredLostResponse::respond204,
    PostAutomatedPatronBlocksHandlersItemDeclaredLostResponse::respond409WithTextPlain,
    PostAutomatedPatronBlocksHandlersItemDeclaredLostResponse::respond500WithTextPlain
  ),

  ITEM_AGED_TO_LOST(
    PostAutomatedPatronBlocksHandlersItemAgedToLostResponse::respond204,
    PostAutomatedPatronBlocksHandlersItemAgedToLostResponse::respond409WithTextPlain,
    PostAutomatedPatronBlocksHandlersItemAgedToLostResponse::respond500WithTextPlain
  ),

  ITEM_CLAIMED_RETURNED(
    PostAutomatedPatronBlocksHandlersItemClaimedReturnedResponse::respond204,
    PostAutomatedPatronBlocksHandlersItemClaimedReturnedResponse::respond409WithTextPlain,
    PostAutomatedPatronBlocksHandlersItemClaimedReturnedResponse::respond500WithTextPlain
  ),

  LOAN_DUE_DATE_CHANGED(
    PostAutomatedPatronBlocksHandlersItemClaimedReturnedResponse::respond204,
    PostAutomatedPatronBlocksHandlersItemClaimedReturnedResponse::respond409WithTextPlain,
    PostAutomatedPatronBlocksHandlersItemClaimedReturnedResponse::respond500WithTextPlain
  );

  public final Supplier<ResponseDelegate> respond204;
  public final Function<Object, ResponseDelegate> respond409;
  public final Function<Object, ResponseDelegate> respond500;

  EventProcessingResultAdapter(
    Supplier<ResponseDelegate> respond204,
    Function<Object, ResponseDelegate> respond409,
    Function<Object, ResponseDelegate> respond500) {
    this.respond204 = respond204;
    this.respond409 = respond409;
    this.respond500 = respond500;
  }
}

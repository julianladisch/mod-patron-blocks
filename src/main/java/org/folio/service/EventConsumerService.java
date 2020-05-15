package org.folio.service;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EventConsumerService {

  private static final Logger log = LoggerFactory.getLogger(EventConsumerService.class);

  public void handleFeefineBalanceChangedEvent(String entity) {
    log.info("Payload:\n" + entity);


  }

}
